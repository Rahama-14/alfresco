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
package org.alfresco.cmis.dictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alfresco.cmis.CMISDataTypeEnum;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.dictionary.DictionaryListener;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.AbstractLifecycleBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;


/**
 * Common CMIS Dictionary Support including registry of Types.
 * 
 * @author davidc
 */
public abstract class CMISAbstractDictionaryService extends AbstractLifecycleBean implements CMISDictionaryService, DictionaryListener
{
    // Logger
    protected static final Log logger = LogFactory.getLog(CMISAbstractDictionaryService.class);

    // service dependencies
    private DictionaryDAO dictionaryDAO;
    protected CMISMapping cmisMapping;
    protected DictionaryService dictionaryService;

    /**
     * Set the mapping service
     * 
     * @param cmisMapping
     */
    public void setCMISMapping(CMISMapping cmisMapping)
    {
        this.cmisMapping = cmisMapping;
    }

    /**
     * Set the dictionary Service
     * 
     * @param dictionaryService
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * Set the dictionary DAO
     * 
     * @param dictionaryDAO
     */
    public void setDictionaryDAO(DictionaryDAO dictionaryDAO)
    {
        this.dictionaryDAO = dictionaryDAO;
    }


    // TODO: Handle tenants
    // TODO: read / write locks
    private DictionaryRegistry registry;

    
    /**
     * CMIS Dictionary registry
     *
     * Index of CMIS Type Definitions
     */
    /*package*/ class DictionaryRegistry
    {
        // Type Definitions Index
        Map<QName, CMISAbstractTypeDefinition> typeDefsByQName = new HashMap<QName, CMISAbstractTypeDefinition>();
        Map<QName, CMISAbstractTypeDefinition> assocDefsByQName = new HashMap<QName, CMISAbstractTypeDefinition>();
        Map<CMISTypeId, CMISAbstractTypeDefinition> objectDefsByTypeId = new HashMap<CMISTypeId, CMISAbstractTypeDefinition>();
        Map<CMISTypeId, CMISTypeDefinition> typeDefsByTypeId = new HashMap<CMISTypeId, CMISTypeDefinition>();
        Map<String, CMISTypeDefinition> typeDefsByTable = new HashMap<String, CMISTypeDefinition>();

        // Property Definitions Index
        Map<String, CMISPropertyDefinition> propDefsByName = new HashMap<String, CMISPropertyDefinition>();
        Map<QName, CMISPropertyDefinition> propDefsByQName = new HashMap<QName, CMISPropertyDefinition>();
        Map<CMISPropertyId, CMISPropertyDefinition> propDefsByPropId = new HashMap<CMISPropertyId, CMISPropertyDefinition>();

        /**
         * Register Type Definition
         * 
         * @param typeDef
         */
        public void registerTypeDefinition(CMISAbstractTypeDefinition typeDef)
        {
            CMISTypeDefinition existingTypeDef = objectDefsByTypeId.get(typeDef.getTypeId());
            if (existingTypeDef != null)
            {
                throw new AlfrescoRuntimeException("Type " + typeDef.getTypeId() + " already registered");
            }
            
            objectDefsByTypeId.put(typeDef.getTypeId(), typeDef);
            if (typeDef.isPublic())
            {
                QName typeQName = typeDef.getTypeId().getQName();
                if (typeQName != null)
                {
                    if (typeDef instanceof CMISRelationshipTypeDefinition)
                    {
                        assocDefsByQName.put(typeQName, typeDef);
                    }
                    else
                    {
                        typeDefsByQName.put(typeQName, typeDef);
                    }
                }
                typeDefsByTypeId.put(typeDef.getTypeId(), typeDef);
                typeDefsByTable.put(typeDef.getQueryName().toLowerCase(), typeDef);
            }
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Registered type " + typeDef.getTypeId() + " (scope=" + typeDef.getTypeId().getScope() + ", public=" + typeDef.isPublic() + ")");
                logger.debug(" QName: " + typeDef.getTypeId().getQName());
                logger.debug(" Table: " + typeDef.getQueryName());
            }
        }

        /**
         * Registry Property Definition
         * 
         * @param propDef
         */
        public void registerPropertyDefinition(CMISPropertyDefinition propDef)
        {
            CMISPropertyDefinition existingPropDef = propDefsByPropId.get(propDef.getPropertyId());
            if (existingPropDef != null)
            {
                throw new AlfrescoRuntimeException("Property " + propDef.getPropertyId() + " of " + propDef.getOwningType().getTypeId() + " already registered by type " + existingPropDef.getOwningType().getTypeId());
            }
            
            propDefsByPropId.put(propDef.getPropertyId(), propDef);
            propDefsByQName.put(propDef.getPropertyId().getQName(), propDef);
            propDefsByName.put(propDef.getPropertyId().getName().toLowerCase(), propDef);
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Registered property " + propDef.getPropertyId().getId());
                logger.debug(" QName: " + propDef.getPropertyId().getQName());
                logger.debug(" Name: " + propDef.getPropertyId().getName());
                logger.debug(" Owning Type: " + propDef.getOwningType().getTypeId());
            }
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("DictionaryRegistry[");
            builder.append("Types=").append(typeDefsByTypeId.size()).append(", ");
            builder.append("Properties=").append(propDefsByPropId.size());
            builder.append("]");
            return builder.toString();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#findType(org.alfresco.cmis.dictionary.CMISTypeId)
     */
    public CMISTypeDefinition findType(CMISTypeId typeId)
    {
        return registry.objectDefsByTypeId.get(typeId);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#findType(java.lang.String)
     */
    public CMISTypeDefinition findType(String typeId)
    {
        CMISTypeId cmisTypeId = cmisMapping.getCmisTypeId(typeId);
        return findType(cmisTypeId);
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#findTypeForClass(org.alfresco.service.namespace.QName, org.alfresco.cmis.dictionary.CMISScope[])
     */
    public CMISTypeDefinition findTypeForClass(QName clazz, CMISScope... matchingScopes)
    {
        // searching for relationship
        boolean scopeByRelationship = false;
        for (CMISScope scope : matchingScopes)
        {
            if (scope == CMISScope.RELATIONSHIP)
            {
                scopeByRelationship = true;
                break;
            }
        }
        
        // locate type in registry
        CMISTypeDefinition typeDef = null;
        if (scopeByRelationship)
        {
            typeDef = registry.assocDefsByQName.get(clazz);
        }
        else
        {
            typeDef = registry.typeDefsByQName.get(clazz);
        }

        // ensure matches one of provided matching scopes
        CMISTypeDefinition matchingTypeDef = (matchingScopes.length == 0) ? typeDef : null;
        if (typeDef != null)
        {
            for (CMISScope scope : matchingScopes)
            {
                if (typeDef.getTypeId().getScope() == scope)
                {
                    matchingTypeDef = typeDef;
                    break;
                }
            }
        }
        
        return matchingTypeDef;
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#findTypeForTable(java.lang.String)
     */
    public CMISTypeDefinition findTypeForTable(String tableName)
    {
        CMISTypeDefinition typeDef = registry.typeDefsByTable.get(tableName.toLowerCase());
        return typeDef;
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#getAllTypes()
     */
    public Collection<CMISTypeDefinition> getAllTypes()
    {
        return Collections.unmodifiableCollection(registry.typeDefsByTypeId.values());
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#getProperty(org.alfresco.service.namespace.QName, org.alfresco.cmis.dictionary.CMISTypeDefinition)
     */
    public CMISPropertyDefinition findProperty(QName property, CMISTypeDefinition matchingType)
    {
        CMISPropertyDefinition propDef = registry.propDefsByQName.get(property);
        return getProperty(propDef, matchingType);
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#getProperty(java.lang.String, org.alfresco.cmis.dictionary.CMISTypeDefinition)
     */
    public CMISPropertyDefinition findProperty(String property, CMISTypeDefinition matchingType)
    {
        CMISPropertyDefinition propDef = registry.propDefsByName.get(property.toLowerCase());
        return getProperty(propDef, matchingType);
    }
    
    /**
     * Return property definition if part of specified type definition
     * 
     * @param property
     * @param matchingType
     * @return  property definition (if matches), or null (if not matches)
     */
    private CMISPropertyDefinition getProperty(CMISPropertyDefinition property, CMISTypeDefinition matchingType)
    {
        boolean isMatchingType = (matchingType == null);
        if (property != null && matchingType != null)
        {
            Map<CMISPropertyId, CMISPropertyDefinition> props = matchingType.getPropertyDefinitions();
            if (props.containsKey(property.getPropertyId()))
            {
                isMatchingType = true;
            }
        }
        return isMatchingType ? property : null;
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.cmis.dictionary.CMISDictionaryService#getDataType(org.alfresco.service.namespace.QName)
     */
    public CMISDataTypeEnum findDataType(QName dataType)
    {
        return cmisMapping.getDataType(dataType);
    }


    /**
     * Factory for creating CMIS Definitions
     * 
     * @param registry
     */
    abstract protected void createDefinitions(DictionaryRegistry registry);
    

    /**
     * Dictionary Initialisation - creates a new registry
     */
    private void init()
    {
        DictionaryRegistry registry = new DictionaryRegistry();

        if (logger.isDebugEnabled())
            logger.debug("Creating type definitions...");
        
        // phase 1: construct type definitions
        createDefinitions(registry);
        for (CMISAbstractTypeDefinition objectTypeDef : registry.objectDefsByTypeId.values())
        {
            Map<CMISPropertyId, CMISPropertyDefinition> propDefs = objectTypeDef.createProperties(cmisMapping, dictionaryService);
            for (CMISPropertyDefinition propDef : propDefs.values())
            {
                registry.registerPropertyDefinition(propDef);
            }
            objectTypeDef.createSubTypes(cmisMapping, dictionaryService);
        }

        if (logger.isDebugEnabled())
            logger.debug("Linking type definitions...");

        // phase 2: link together
        for (CMISAbstractTypeDefinition objectTypeDef : registry.objectDefsByTypeId.values())
        {
            objectTypeDef.resolveDependencies(registry);
        }

        if (logger.isDebugEnabled())
            logger.debug("Resolving type inheritance...");

        // phase 3: resolve inheritance
        Map<Integer,List<CMISAbstractTypeDefinition>> order = new TreeMap<Integer, List<CMISAbstractTypeDefinition>>();
        for (CMISAbstractTypeDefinition typeDef : registry.objectDefsByTypeId.values())
        {
            // calculate class depth in hierarchy
            int depth = 0;
            CMISAbstractTypeDefinition parent = typeDef.getInternalParentType();
            while (parent != null)
            {
                depth = depth +1;
                parent = parent.getInternalParentType();
            }

            // map class to depth
            List<CMISAbstractTypeDefinition> classes = order.get(depth);
            if (classes == null)
            {
                classes = new ArrayList<CMISAbstractTypeDefinition>();
                order.put(depth, classes);
            }
            classes.add(typeDef);
        }
        for (int depth = 0; depth < order.size(); depth++)
        {
            for (CMISAbstractTypeDefinition typeDef : order.get(depth))
            {
                typeDef.resolveInheritance(registry);
            }
        }

        // publish new registry
        this.registry = registry;
        
        if (logger.isInfoEnabled())
            logger.info("Initialized CMIS Dictionary. Types:" + registry.typeDefsByTypeId.size() + ", Properties:" + registry.propDefsByPropId.size());
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryListener#onInit()
     */
    public void onDictionaryInit()
    {
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryListener#afterInit()
     */
    public void afterDictionaryInit()
    {
        init();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.util.AbstractLifecycleBean#onBootstrap(org.springframework.context.ApplicationEvent)
     */
    protected void onBootstrap(ApplicationEvent event)
    {
        afterDictionaryInit();
        dictionaryDAO.register(this);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.util.AbstractLifecycleBean#onShutdown(org.springframework.context.ApplicationEvent)
     */
    protected void onShutdown(ApplicationEvent event)
    {
    }

}
