/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.dictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.NamespaceDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Default implementation of the Dictionary.
 *  
 * @author David Caruana
 *
 */
public class DictionaryDAOImpl implements DictionaryDAO
{
    /**
     * Lock objects
     */
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();
    
    // Namespace Data Access
    private NamespaceDAO namespaceDAO;

    // Tenant Service
    private TenantService tenantService;
    
    // Internal cache (clusterable)
    private SimpleCache<String, DictionaryRegistry> dictionaryRegistryCache;

    // used to reset the cache
    private ThreadLocal<DictionaryRegistry> dictionaryRegistryThreadLocal = new ThreadLocal<DictionaryRegistry>();
    private ThreadLocal<DictionaryRegistry> defaultDictionaryRegistryThreadLocal = new ThreadLocal<DictionaryRegistry>();

    // Static list of registered dictionary listeners
    private List<DictionaryListener> dictionaryListeners = new ArrayList<DictionaryListener>();

    // Logger
    private static Log logger = LogFactory.getLog(DictionaryDAO.class);


	// inject dependencies
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }
    
    public void setDictionaryRegistryCache(SimpleCache<String, DictionaryRegistry> dictionaryRegistryCache)
    {
        this.dictionaryRegistryCache = dictionaryRegistryCache;
    }
    
    /**
     * Construct
     * 
     * @param namespaceDAO  namespace data access
     */
    public DictionaryDAOImpl(NamespaceDAO namespaceDAO)
    {
        this.namespaceDAO = namespaceDAO;
        this.namespaceDAO.registerDictionary(this);
        
    }
    
    /**
     * Register with the Dictionary
     */
    public void register(DictionaryListener dictionaryListener)
    {
        if (! dictionaryListeners.contains(dictionaryListener))
        {
            dictionaryListeners.add(dictionaryListener);
        }
    }
    
    /**
     * Initialise the Dictionary & Namespaces
     */
    public void init()
    {
        initDictionary(tenantService.getCurrentUserDomain());
    }
    
    /**
     * Destroy the Dictionary & Namespaces
     */
    public void destroy()
    {
        String tenantDomain = tenantService.getCurrentUserDomain();
        
        removeDictionaryRegistry(tenantDomain);
        
        namespaceDAO.destroy();
        
        // notify registered listeners that dictionary has been destroyed
        for (DictionaryListener dictionaryDeployer : dictionaryListeners)
        {
            dictionaryDeployer.afterDictionaryDestroy();
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Dictionary destroyed");
        }
    }
    
    /**
     * Reset the Dictionary & Namespaces
     */      
    public void reset()
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("Resetting dictionary ...");
        }
        
        destroy();
    	init();
    	
        if (logger.isDebugEnabled()) 
        {
            logger.debug("... resetting dictionary completed");
        }
    }
    
    // load dictionary (models and namespaces)
    private DictionaryRegistry initDictionary(final String tenantDomain)
    {
        long startTime = System.currentTimeMillis();
        
        try
        {
            return AuthenticationUtil.runAs(new RunAsWork<DictionaryRegistry>()
            {
                public DictionaryRegistry doWork()
                {  
                    try
                    {
                        // create threadlocal, if needed
                        createDataDictionaryLocal(tenantDomain);
                        
                        DictionaryRegistry dictionaryRegistry = initDictionaryRegistry(tenantDomain);
                        
                        if (dictionaryRegistry == null)
                        {     
                            // unexpected
                            throw new AlfrescoRuntimeException("Failed to init dictionaryRegistry " + tenantDomain);
                        }
                        
                        try
                        {
                            writeLock.lock();
                            dictionaryRegistryCache.put(tenantDomain, dictionaryRegistry);
                        }
                        finally
                        {
                            writeLock.unlock();
                        }
                        
                        return dictionaryRegistry;
                    }
                    finally
                    {
                        try
                        {
                            readLock.lock();
                            if (dictionaryRegistryCache.get(tenantDomain) != null)
                            {
                                removeDataDictionaryLocal(tenantDomain);
                            }
                        }
                        finally
                        {
                            readLock.unlock();
                        }
                    }
                }                               
            }, tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(), tenantDomain));
        }
        finally
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Init Dictionary: model count = "+(getModels() != null ? getModels().size() : 0) +" in "+(System.currentTimeMillis()-startTime)+" msecs "+(tenantDomain.equals(TenantService.DEFAULT_DOMAIN) ? "" : " (Tenant: "+tenantDomain+")"));
            }
        }
    }
    
    private DictionaryRegistry initDictionaryRegistry(String tenantDomain)
    {
        getDictionaryRegistry(tenantDomain).setCompiledModels(new HashMap<QName,CompiledModel>());
        getDictionaryRegistry(tenantDomain).setUriToModels(new HashMap<String, List<CompiledModel>>());
        
        // initialise empty dictionary & namespaces
        namespaceDAO.init();
        
        // populate the dictionary based on registered sources
        for (DictionaryListener dictionaryDeployer : dictionaryListeners)
        {
            dictionaryDeployer.onDictionaryInit();
        }
        
        // notify registered listeners that dictionary has been initialised (population is complete)
        for (DictionaryListener dictionaryListener : dictionaryListeners)
        {
            dictionaryListener.afterDictionaryInit();
        }
        
        return getDictionaryRegistryLocal(tenantDomain);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.DictionaryDAO#putModel(org.alfresco.repo.dictionary.impl.M2Model)
     */
    public QName putModel(M2Model model)
    {
        String tenantDomain = tenantService.getCurrentUserDomain();
        
        // Compile model definition
        CompiledModel compiledModel = model.compile(this, namespaceDAO);
        QName modelName = compiledModel.getModelDefinition().getName();
        
        // Remove namespace definitions for previous model, if it exists
        CompiledModel previousVersion = getCompiledModels(tenantDomain).get(modelName);
        if (previousVersion != null)
        {
            for (M2Namespace namespace : previousVersion.getM2Model().getNamespaces())
            {
                namespaceDAO.removePrefix(namespace.getPrefix());
                namespaceDAO.removeURI(namespace.getUri());
                unmapUriToModel(namespace.getUri(), previousVersion, tenantDomain);
            }
            for (M2Namespace importNamespace : previousVersion.getM2Model().getImports())
            {
            	unmapUriToModel(importNamespace.getUri(), previousVersion, tenantDomain);
            }
        }
        
        // Create namespace definitions for new model
        for (M2Namespace namespace : model.getNamespaces())
        {
            namespaceDAO.addURI(namespace.getUri());
            namespaceDAO.addPrefix(namespace.getPrefix(), namespace.getUri());
            mapUriToModel(namespace.getUri(), compiledModel, tenantDomain);
        }
        for (M2Namespace importNamespace : model.getImports())
        {
        	mapUriToModel(importNamespace.getUri(), compiledModel, tenantDomain);
        }
        
        // Publish new Model Definition
        getCompiledModels(tenantDomain).put(modelName, compiledModel);

        if (logger.isDebugEnabled())
        {
            logger.debug("Registered model " + modelName.toPrefixString(namespaceDAO));
            for (M2Namespace namespace : model.getNamespaces())
            {
                logger.debug("Registered namespace '" + namespace.getUri() + "' (prefix '" + namespace.getPrefix() + "')");
            }
        }
        
        return modelName;
    }
    
    /**
     * @see org.alfresco.repo.dictionary.DictionaryDAO#removeModel(org.alfresco.service.namespace.QName)
     */
    public void removeModel(QName modelName)
    {
        String tenantDomain = tenantService.getCurrentUserDomain();
        
        CompiledModel compiledModel = getCompiledModels(tenantDomain).get(modelName);
        if (compiledModel != null)
        {
            // Remove the namespaces from the namespace service
            M2Model model = compiledModel.getM2Model();            
            for (M2Namespace namespace : model.getNamespaces())
            {
                namespaceDAO.removePrefix(namespace.getPrefix());
                namespaceDAO.removeURI(namespace.getUri());
                unmapUriToModel(namespace.getUri(), compiledModel, tenantDomain);
            }
            
            // Remove the model from the list
            getCompiledModels(tenantDomain).remove(modelName);
        }
    }
    
    /**
     * Map Namespace URI to Model
     * 
     * @param uri   namespace uri
     * @param model   model
     * @param tenantDomain
     */
    private void mapUriToModel(String uri, CompiledModel model, String tenantDomain)
    {
    	List<CompiledModel> models = getUriToModels(tenantDomain).get(uri);
    	if (models == null)
    	{
    		models = new ArrayList<CompiledModel>();
    		getUriToModels(tenantDomain).put(uri, models);
    	}
    	if (!models.contains(model))
    	{
    		models.add(model);
    	}
    }
    
    /**
     * Unmap Namespace URI from Model
     * 
     * @param uri  namespace uri
     * @param model   model
     * @param tenantDomain
     */
    private void unmapUriToModel(String uri, CompiledModel model, String tenantDomain)
    {
    	List<CompiledModel> models = getUriToModels(tenantDomain).get(uri);
    	if (models != null)
    	{
    		models.remove(model);
    	}
    }

    
    /**
     * Get Models mapped to Namespace Uri
     * 
     * @param uri   namespace uri
     * @return   mapped models 
     */
    private List<CompiledModel> getModelsForUri(String uri)
    {
        String tenantDomain = tenantService.getCurrentUserDomain();
        if (! tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            // get non-tenant models (if any)
            List<CompiledModel> models = getUriToModels(TenantService.DEFAULT_DOMAIN).get(uri);
            
            List<CompiledModel> filteredModels = new ArrayList<CompiledModel>();
            if (models != null)
            {
                filteredModels.addAll(models);
            }
    
            // get tenant models (if any)
            List<CompiledModel> tenantModels = getUriToModels(tenantDomain).get(uri);
            if (tenantModels != null)
            {
                if (models != null)
                {
                    // check to see if tenant model overrides a non-tenant model
                    for (CompiledModel tenantModel : tenantModels)
                    {
                        for (CompiledModel model : models)
                        {
                            if (tenantModel.getM2Model().getName().equals(model.getM2Model().getName()))
                            {
                                filteredModels.remove(model);
                            }
                        }
                    }
                }
                filteredModels.addAll(tenantModels);
                models = filteredModels;
            }
            
            if (models == null)
            {
                models = Collections.emptyList();
            }
            return models;
        }
        
        List<CompiledModel> models = getUriToModels(TenantService.DEFAULT_DOMAIN).get(uri);
        if (models == null)
        {
            models = Collections.emptyList(); 
        }
        return models;
    }
    
    /**
     * @param modelName  the model name
     * @return the compiled model of the given name
     */
    /* package */ CompiledModel getCompiledModel(QName modelName)
    {
        String tenantDomain = tenantService.getCurrentUserDomain();
        if (! tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            // get tenant-specific model (if any)
            CompiledModel model = getCompiledModels(tenantDomain).get(modelName);
            if (model != null)
            {
                return model;
            }
            // else drop down to check for shared (core/system) models ...
        }

        // get non-tenant model (if any)
        CompiledModel model = getCompiledModels(TenantService.DEFAULT_DOMAIN).get(modelName);
        if (model == null)
        {
            throw new DictionaryException("d_dictionary.model.err.no_model", modelName);
        }
        return model;
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.ModelQuery#getPropertyType(org.alfresco.repo.ref.QName)
     */
    public DataTypeDefinition getDataType(QName typeName)
    {
        List<CompiledModel> models = getModelsForUri(typeName.getNamespaceURI());
        for (CompiledModel model : models)
        {
        	DataTypeDefinition dataType = model.getDataType(typeName);
        	if (dataType != null)
        	{
        		return dataType;
        	}
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ModelQuery#getDataType(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public DataTypeDefinition getDataType(Class javaClass)
    {
        String tenantDomain = tenantService.getCurrentUserDomain();
        if (! tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            // get tenant models (if any)                
            for (CompiledModel model : getCompiledModels(tenantDomain).values())
            { 
                DataTypeDefinition dataTypeDef = model.getDataType(javaClass);
                if (dataTypeDef != null)
                {
                    return dataTypeDef;
                }
            }          
        
            // get non-tenant models (if any)
            for (CompiledModel model : getCompiledModels(TenantService.DEFAULT_DOMAIN).values())
            {    
                DataTypeDefinition dataTypeDef = model.getDataType(javaClass);
                if (dataTypeDef != null)
                {
                    return dataTypeDef;
                }
            }
        
            return null;
        }
        else
        {
            for (CompiledModel model : getCompiledModels(TenantService.DEFAULT_DOMAIN).values())
            {    
                DataTypeDefinition dataTypeDef = model.getDataType(javaClass);
                if (dataTypeDef != null)
                {
                    return dataTypeDef;
                }
            }
        }
        return null;
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.DictionaryDAO#getPropertyTypes(org.alfresco.repo.ref.QName)
     */
    public Collection<DataTypeDefinition> getDataTypes(QName modelName)
    {
        CompiledModel model = getCompiledModel(modelName);
        return model.getDataTypes();
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.ModelQuery#getType(org.alfresco.repo.ref.QName)
     */
    public TypeDefinition getType(QName typeName)
    {
        List<CompiledModel> models = getModelsForUri(typeName.getNamespaceURI());
        for (CompiledModel model : models)
        {
        	TypeDefinition type = model.getType(typeName);
        	if (type != null)
        	{
        		return type;
        	}
        }
        
        if (logger.isWarnEnabled())
        {
            logger.warn("Type not found: "+typeName);
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryDAO#getSubTypes(org.alfresco.service.namespace.QName, boolean)
     */
    public Collection<QName> getSubTypes(QName superType, boolean follow)
    {
    	// note: could be optimised further, if compiled into the model
    	
        // Get all types (with parent type) for all models
        Map<QName, QName> allTypesAndParents = new HashMap<QName, QName>(); // name, parent
        
        for (CompiledModel model : getCompiledModels().values())
        {
        	for (TypeDefinition type : model.getTypes())
        	{
        		allTypesAndParents.put(type.getName(), type.getParentName());
        	}
        }
        
        // Get sub types
    	HashSet<QName> subTypes = new HashSet<QName>();
        for (QName type : allTypesAndParents.keySet())
        {
        	if (follow)
        	{   
        		// all sub types
        		QName current = type;
	            while ((current != null) && !current.equals(superType))
	            {
	            	current = allTypesAndParents.get(current); // get parent
	            }
	            if (current != null)
	            {
	            	subTypes.add(type);
	            }
        	}
        	else
        	{
        		// immediate sub types only
        	    QName typesSuperType = allTypesAndParents.get(type);
        		if (typesSuperType != null && typesSuperType.equals(superType))
        		{
        			subTypes.add(type);
        		}
        	}

        }
        return subTypes;
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.ModelQuery#getAspect(org.alfresco.repo.ref.QName)
     */
    public AspectDefinition getAspect(QName aspectName)
    {
        List<CompiledModel> models = getModelsForUri(aspectName.getNamespaceURI());
        for (CompiledModel model : models)
        {
        	AspectDefinition aspect = model.getAspect(aspectName);
        	if (aspect != null)
        	{
        		return aspect;
        	}
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryDAO#getSubAspects(org.alfresco.service.namespace.QName, boolean)
     */
    public Collection<QName> getSubAspects(QName superAspect, boolean follow)
    {
    	// note: could be optimised further, if compiled into the model
    	
        // Get all aspects (with parent aspect) for all models   
        Map<QName, QName> allAspectsAndParents = new HashMap<QName, QName>(); // name, parent
        
        for (CompiledModel model : getCompiledModels().values())
        {
        	for (AspectDefinition aspect : model.getAspects())
        	{
        		allAspectsAndParents.put(aspect.getName(), aspect.getParentName());
        	}
        }
   	
        // Get sub aspects
    	HashSet<QName> subAspects = new HashSet<QName>();
        for (QName aspect : allAspectsAndParents.keySet())
        {
        	if (follow)
        	{
        		// all sub aspects
	        	QName current = aspect;
	            while ((current != null) && !current.equals(superAspect))
	            {
	            	current = allAspectsAndParents.get(current); // get parent
	            }
	            if (current != null)
	            {
	            	subAspects.add(aspect);
	            }
	    	}
	    	else
	    	{
	    		// immediate sub aspects only
	    		if (allAspectsAndParents.get(aspect).equals(superAspect))
	    		{
	    			subAspects.add(aspect);
	    		}
	    	}        	
        }
        return subAspects;
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.ModelQuery#getClass(org.alfresco.repo.ref.QName)
     */
    public ClassDefinition getClass(QName className)
    {
        List<CompiledModel> models = getModelsForUri(className.getNamespaceURI());

        for (CompiledModel model : models)
        {
        	ClassDefinition classDef = model.getClass(className);
        	if (classDef != null)
        	{
        		return classDef;
        	}
        }
        return null;
    }

    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.ModelQuery#getProperty(org.alfresco.repo.ref.QName)
     */
    public PropertyDefinition getProperty(QName propertyName)
    {
        List<CompiledModel> models = getModelsForUri(propertyName.getNamespaceURI());
        for (CompiledModel model : models)
        {
        	PropertyDefinition propDef = model.getProperty(propertyName);
        	if (propDef != null)
        	{
        		return propDef;
        	}
        }
        return null;
    }

    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ModelQuery#getConstraint(org.alfresco.service.namespace.QName)
     */
    public ConstraintDefinition getConstraint(QName constraintQName)
    {
        List<CompiledModel> models = getModelsForUri(constraintQName.getNamespaceURI());
        for (CompiledModel model : models)
        {
        	ConstraintDefinition constraintDef = model.getConstraint(constraintQName);
        	if (constraintDef != null)
        	{
        		return constraintDef;
        	}
        }
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.ModelQuery#getAssociation(org.alfresco.repo.ref.QName)
     */
    public AssociationDefinition getAssociation(QName assocName)
    {
        List<CompiledModel> models = getModelsForUri(assocName.getNamespaceURI());
        for (CompiledModel model : models)
        {
        	AssociationDefinition assocDef = model.getAssociation(assocName);
        	if (assocDef != null)
        	{
        		return assocDef;
        	}
        }
        return null;
    }

    public Collection<AssociationDefinition> getAssociations(QName modelName)
    {
        CompiledModel model = getCompiledModel(modelName);
        return model.getAssociations();
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.DictionaryDAO#getModels()
     */
    public Collection<QName> getModels()
    {
        // get all models - including inherited models, if applicable
        return getCompiledModels().keySet();
    }
    
    // MT-specific
    public boolean isModelInherited(QName modelName)
    {    
        String tenantDomain = tenantService.getCurrentUserDomain();
        if (! tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            // get tenant-specific model (if any)
            CompiledModel model = getCompiledModels(tenantDomain).get(modelName);
            if (model != null)
            {
                return false;
            }
            // else drop down to check for shared (core/system) models ...
        }

        // get non-tenant model (if any)
        CompiledModel model = getCompiledModels(TenantService.DEFAULT_DOMAIN).get(modelName);
        if (model == null)
        {
            throw new DictionaryException("d_dictionary.model.err.no_model", modelName);
        }
        
        if (! tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    private Map<QName,CompiledModel> getCompiledModels() 
    {
        String tenantDomain = tenantService.getCurrentUserDomain();
        if (! tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            // return all tenant-specific models and all inherited (non-overridden) models
            Map<QName, CompiledModel> filteredModels = new HashMap<QName, CompiledModel>();
            
            // get tenant models (if any)
            Map<QName,CompiledModel> tenantModels = getCompiledModels(tenantDomain);
            
            // get non-tenant models - these will include core/system models and any additional custom models (which are implicitly available to all tenants)
            Map<QName,CompiledModel> nontenantModels = getCompiledModels(TenantService.DEFAULT_DOMAIN);

            // check for overrides
            filteredModels.putAll(nontenantModels);
     
            for (QName tenantModel : tenantModels.keySet())
            {
                for (QName nontenantModel : nontenantModels.keySet())
                {
                    if (tenantModel.equals(nontenantModel))
                    {
                        // override
                        filteredModels.remove(nontenantModel);
                        break;
                    }
                }
            }

            filteredModels.putAll(tenantModels);
            return filteredModels;
        }
        else
        {
            // return all (non-tenant) models
            return getCompiledModels(TenantService.DEFAULT_DOMAIN);
        } 
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.DictionaryDAO#getModel(org.alfresco.repo.ref.QName)
     */
    public ModelDefinition getModel(QName name)
    {
        CompiledModel model = getCompiledModel(name);
        return model.getModelDefinition();
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.DictionaryDAO#getTypes(org.alfresco.repo.ref.QName)
     */
    public Collection<TypeDefinition> getTypes(QName modelName)
    {
        CompiledModel model = getCompiledModel(modelName);
        return model.getTypes();
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.DictionaryDAO#getAspects(org.alfresco.repo.ref.QName)
     */
    public Collection<AspectDefinition> getAspects(QName modelName)
    {
        CompiledModel model = getCompiledModel(modelName);
        return model.getAspects();
    }


    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.impl.DictionaryDAO#getAnonymousType(org.alfresco.repo.ref.QName, java.util.Collection)
     */
    public TypeDefinition getAnonymousType(QName type, Collection<QName> aspects)
    {
        TypeDefinition typeDef = getType(type);
        if (typeDef == null)
        {
            throw new DictionaryException("d_dictionary.model.err.type_not_found", type);
        }
        Collection<AspectDefinition> aspectDefs = new ArrayList<AspectDefinition>();
        if (aspects != null)
        {
            for (QName aspect : aspects)
            {
                AspectDefinition aspectDef = getAspect(aspect);
                if (aspectDef == null)
                {
                    throw new DictionaryException("d_dictionary.model.err.aspect_not_found", aspect);
                }
                aspectDefs.add(aspectDef);
            }
        }
        return new M2AnonymousTypeDefinition(typeDef, aspectDefs);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryDAO#getProperties(org.alfresco.service.namespace.QName)
     */
    public Collection<PropertyDefinition> getProperties(QName modelName)
    {
        CompiledModel model = getCompiledModel(modelName);
        return model.getProperties();
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryDAO#getProperties(org.alfresco.service.namespace.QName, org.alfresco.service.namespace.QName)
     */
    public Collection<PropertyDefinition> getProperties(QName modelName, QName dataType)
    {
        HashSet<PropertyDefinition> properties = new HashSet<PropertyDefinition>();

        Collection<PropertyDefinition> props = getProperties(modelName);
        for(PropertyDefinition prop : props)
        {
            if((dataType == null) ||   prop.getDataType().getName().equals(dataType))
            {
                properties.add(prop);
            }
        }
        return properties;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryDAO#getNamespaces(org.alfresco.service.namespace.QName)
     */
    public Collection<NamespaceDefinition> getNamespaces(QName modelName)
    {
        CompiledModel model = getCompiledModel(modelName);
        ModelDefinition modelDef = model.getModelDefinition();
        
        List<NamespaceDefinition> namespaces = new ArrayList<NamespaceDefinition>();
        for (M2Namespace namespace : model.getM2Model().getNamespaces())
        {
            namespaces.add(new M2NamespaceDefinition(modelDef, namespace.getUri(), namespace.getPrefix()));
        }
        
        return namespaces;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryDAO#getConstraints(org.alfresco.service.namespace.QName)
     */
    public Collection<ConstraintDefinition> getConstraints(QName modelName)
    {
        CompiledModel model = getCompiledModel(modelName);
        return model.getConstraints();
    }
    
    // re-entrant (eg. via reset)
    private DictionaryRegistry getDictionaryRegistry(String tenantDomain)
    {
        DictionaryRegistry dictionaryRegistry = null;
        
        // check threadlocal first - return if set
        dictionaryRegistry = getDictionaryRegistryLocal(tenantDomain);
        if (dictionaryRegistry != null)
        {
            return dictionaryRegistry; // return local dictionaryRegistry
        }
        
        try
        {
            // check cache second - return if set
            readLock.lock();
            dictionaryRegistry = dictionaryRegistryCache.get(tenantDomain);
            
            if (dictionaryRegistry != null)
            {
                return dictionaryRegistry; // return cached config
            }
        }
        finally
        {
            readLock.unlock();
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("getDictionaryRegistry: not in cache (or threadlocal) - re-init ["+Thread.currentThread().getId()+", "+AlfrescoTransactionSupport.getTransactionId()+"]"+(tenantDomain.equals(TenantService.DEFAULT_DOMAIN) ? "" : " (Tenant: "+tenantDomain+")"));
        }
        
        // reset caches - may have been invalidated (e.g. in a cluster)
        dictionaryRegistry = initDictionary(tenantDomain);
        
        if (dictionaryRegistry == null)
        {     
            // unexpected
            throw new AlfrescoRuntimeException("Failed to get dictionaryRegistry " + tenantDomain);
        }
        
        return dictionaryRegistry;
    }
    
    // create threadlocal
    private void createDataDictionaryLocal(String tenantDomain)      
    {
       // create threadlocal, if needed
        DictionaryRegistry dictionaryRegistry = getDictionaryRegistryLocal(tenantDomain);
        if (dictionaryRegistry == null)
        {
            dictionaryRegistry = new DictionaryRegistry(tenantDomain);
            
            if (tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
            {
                defaultDictionaryRegistryThreadLocal.set(dictionaryRegistry);
            }
            else
            {
                dictionaryRegistryThreadLocal.set(dictionaryRegistry);
            }
        }
    }
    
    // get threadlocal 
    private DictionaryRegistry getDictionaryRegistryLocal(String tenantDomain)
    {
        DictionaryRegistry dictionaryRegistry = null;
        
        if (tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            dictionaryRegistry = this.defaultDictionaryRegistryThreadLocal.get();
        }
        else
        {
            dictionaryRegistry = this.dictionaryRegistryThreadLocal.get();
        }
        
        // check to see if domain switched
        if ((dictionaryRegistry != null) && (tenantDomain.equals(dictionaryRegistry.getTenantDomain())))
        {
            return dictionaryRegistry; // return threadlocal, if set
        }   
        
        return null;
    }
    
    // remove threadlocal
    private void removeDataDictionaryLocal(String tenantDomain)      
    {
        if (tenantDomain.equals(TenantService.DEFAULT_DOMAIN))
        {
            defaultDictionaryRegistryThreadLocal.set(null); // it's in the cache, clear the threadlocal
        }
        else
        {
            dictionaryRegistryThreadLocal.set(null); // it's in the cache, clear the threadlocal
        }
    }
    
    private void removeDictionaryRegistry(String tenantDomain)
    {
        try
        {
            writeLock.lock();
            if (dictionaryRegistryCache.get(tenantDomain) != null)
            {
                dictionaryRegistryCache.remove(tenantDomain);
            }
            
            removeDataDictionaryLocal(tenantDomain);
        }
        finally
        {
            writeLock.unlock();
        }          
    }
    
    /**
     * Get compiledModels from the cache (in the context of the given tenant domain)
     * 
     * @param tenantDomain
     */
    private Map<QName,CompiledModel> getCompiledModels(String tenantDomain)
    {
        return getDictionaryRegistry(tenantDomain).getCompiledModels();
    }

    /**
     * Get uriToModels from the cache (in the context of the given tenant domain)
     * 
     * @param tenantDomain
     */
    private Map<String, List<CompiledModel>> getUriToModels(String tenantDomain)
    {
        return getDictionaryRegistry(tenantDomain).getUriToModels();
    }
    
    /**
     * Return diffs between input model and model in the Dictionary.
     * 
     * If the input model does not exist in the Dictionary or is equivalent to the one in the Dictionary
     * then no diffs will be returned.
     * 
     * @param model
     * @return model diffs (if any)
     */
    private List<M2ModelDiff> diffModel(M2Model model)
    {
        // Compile model definition
        CompiledModel compiledModel = model.compile(this, namespaceDAO);
        QName modelName = compiledModel.getModelDefinition().getName();
        
        CompiledModel previousVersion = null;
        try { previousVersion = getCompiledModel(modelName); } catch (DictionaryException e) {} // ignore missing model

        if (previousVersion == null)
        {
            return new ArrayList<M2ModelDiff>(0);
        }
        else
        {
            return diffModel(previousVersion, compiledModel);
        }
    }
    
    /**
     * Return diffs between two compiled models.
     * 
     * note:
     * - checks classes (types & aspects) for incremental updates
     * - checks properties for incremental updates, but does not include the diffs
     * - checks assocs & child assocs for incremental updates, but does not include the diffs
     * - incremental updates include changes in title/description, property default value, etc
     * - ignores changes in model definition except name (ie. title, description, author, published date, version are treated as an incremental update)
     * 
     * TODO
     * - imports
     * - namespace
     * - datatypes
     * - constraints (including property constraints - references and inline)
     * 
     * @param model
     * @return model diffs (if any)
     */
    /* package */ List<M2ModelDiff> diffModel(CompiledModel previousVersion, CompiledModel model)
    {
        List<M2ModelDiff> M2ModelDiffs = new ArrayList<M2ModelDiff>();
        
        if (previousVersion != null)
        { 
            Collection<TypeDefinition> previousTypes = previousVersion.getTypes();
            Collection<AspectDefinition> previousAspects = previousVersion.getAspects();
           
            if (model == null)
            {
                // delete model
                for (TypeDefinition previousType : previousTypes)
                {
                    M2ModelDiffs.add(new M2ModelDiff(previousType.getName(), M2ModelDiff.TYPE_TYPE, M2ModelDiff.DIFF_DELETED));
                }
                for (AspectDefinition previousAspect : previousAspects)
                {
                    M2ModelDiffs.add(new M2ModelDiff(previousAspect.getName(), M2ModelDiff.TYPE_ASPECT, M2ModelDiff.DIFF_DELETED));
                }              
            }
            else
            {
                // update model
                Collection<TypeDefinition> types = model.getTypes();
                Collection<AspectDefinition> aspects = model.getAspects();
                
                if (previousTypes.size() != 0)
                {
                    M2ModelDiffs.addAll(M2ClassDefinition.diffClassLists(new ArrayList<ClassDefinition>(previousTypes), new ArrayList<ClassDefinition>(types), M2ModelDiff.TYPE_TYPE));
                }
                else
                {
                    for (TypeDefinition type : types)
                    {
                        M2ModelDiffs.add(new M2ModelDiff(type.getName(), M2ModelDiff.TYPE_TYPE, M2ModelDiff.DIFF_CREATED));
                    }
                }
                
                if (previousAspects.size() != 0)
                {
                    M2ModelDiffs.addAll(M2ClassDefinition.diffClassLists(new ArrayList<ClassDefinition>(previousAspects), new ArrayList<ClassDefinition>(aspects), M2ModelDiff.TYPE_ASPECT));
                }
                else
                {
                    for (AspectDefinition aspect : aspects)
                    {
                        M2ModelDiffs.add(new M2ModelDiff(aspect.getName(), M2ModelDiff.TYPE_ASPECT, M2ModelDiff.DIFF_CREATED));
                    }
                }
            }
        }
        else
        {
            if (model != null)
            {
                // new model
                Collection<TypeDefinition> types = model.getTypes();
                Collection<AspectDefinition> aspects = model.getAspects();
                
                for (TypeDefinition type : types)
                {
                    M2ModelDiffs.add(new M2ModelDiff(type.getName(), M2ModelDiff.TYPE_TYPE, M2ModelDiff.DIFF_CREATED));
                }
                           
                for (AspectDefinition aspect : aspects)
                {
                    M2ModelDiffs.add(new M2ModelDiff(aspect.getName(), M2ModelDiff.TYPE_ASPECT, M2ModelDiff.DIFF_CREATED));
                }  
            }
            else 
            {
                // nothing to diff
            }
        }
        
        return M2ModelDiffs;
    }
    
    /**
     * validate against dictionary
     * 
     * if new model 
     * then nothing to validate
     * 
     * else if an existing model 
     * then could be updated (or unchanged) so validate to currently only allow incremental updates
     *   - addition of new types, aspects (except default aspects), properties, associations
     *   - no deletion of types, aspects or properties or associations
     *   - no addition, update or deletion of default/mandatory aspects
     * 
     * @param newOrUpdatedModel
     */
    public void validateModel(M2Model newOrUpdatedModel)
    {
        // Check that all the passed values are not null        
        ParameterCheck.mandatory("newOrUpdatedModel", newOrUpdatedModel);
        
        List<M2ModelDiff> modelDiffs = diffModel(newOrUpdatedModel);
        
        for (M2ModelDiff modelDiff : modelDiffs)
        {
            if (modelDiff.getDiffType().equals(M2ModelDiff.DIFF_DELETED))
            {
                throw new AlfrescoRuntimeException("Failed to validate model update - found deleted " + modelDiff.getElementType() + " '" + modelDiff.getElementName() + "'");
            }
            
            if (modelDiff.getDiffType().equals(M2ModelDiff.DIFF_UPDATED))
            {
                throw new AlfrescoRuntimeException("Failed to validate model update - found non-incrementally updated " + modelDiff.getElementType() + " '" + modelDiff.getElementName() + "'");
            }
        } 
    }
    
    /* package */ class DictionaryRegistry
    {
        private Map<String, List<CompiledModel>> uriToModels = new HashMap<String, List<CompiledModel>>(0);
        private Map<QName,CompiledModel> compiledModels = new HashMap<QName,CompiledModel>(0);
        
        private String tenantDomain;
        
        public DictionaryRegistry(String tenantDomain)
        {
            this.tenantDomain = tenantDomain;
        }
        
        public String getTenantDomain()
        {
            return tenantDomain;
        }
        
        public Map<String, List<CompiledModel>> getUriToModels()
        {
            return uriToModels;
        }
        public void setUriToModels(Map<String, List<CompiledModel>> uriToModels)
        {
            this.uriToModels = uriToModels;
        }
        public Map<QName, CompiledModel> getCompiledModels()
        {
            return compiledModels;
        }
        public void setCompiledModels(Map<QName, CompiledModel> compiledModels)
        {
            this.compiledModels = compiledModels;
        }
    }
}
