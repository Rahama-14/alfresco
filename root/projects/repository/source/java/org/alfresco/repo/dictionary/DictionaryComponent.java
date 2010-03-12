/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.dictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.alfresco.repo.tenant.TenantDeployer;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.surf.util.ParameterCheck;


/**
 * Data Dictionary Service Implementation
 * 
 * @author David Caruana
 */
public class DictionaryComponent implements DictionaryService, TenantDeployer
{
    private DictionaryDAO dictionaryDAO;
    
    // TODO: Check passed arguments are valid
    
    /**
     * Sets the Meta Model DAO
     * 
     * @param metaModelDAO  meta model DAO
     */
    public void setDictionaryDAO(DictionaryDAO dictionaryDAO)
    {
        this.dictionaryDAO = dictionaryDAO;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAllModels()
     */
    public Collection<QName> getAllModels()
    {
        return dictionaryDAO.getModels();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getModel(org.alfresco.repo.ref.QName)
     */
    public ModelDefinition getModel(QName model)
    {
        return dictionaryDAO.getModel(model);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAllPropertyTypes()
     */
    public Collection<QName> getAllDataTypes()
    {
        Collection<QName> propertyTypes = new ArrayList<QName>();
        for (QName model : getAllModels())
        {
            propertyTypes.addAll(getDataTypes(model));
        }
        return propertyTypes;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getPropertyTypes(org.alfresco.repo.ref.QName)
     */
    public Collection<QName> getDataTypes(QName model)
    {
        Collection<DataTypeDefinition> propertyTypes = dictionaryDAO.getDataTypes(model);
        Collection<QName> qnames = new ArrayList<QName>(propertyTypes.size());
        for (DataTypeDefinition def : propertyTypes)
        {
            qnames.add(def.getName());
        }
        return qnames;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAllTypes()
     */
    public Collection<QName> getAllTypes()
    {
        Collection<QName> types = new ArrayList<QName>(100);
        for (QName model : getAllModels())
        {
            types.addAll(getTypes(model));
        }
        return types;
    }

    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getSubTypes(org.alfresco.service.namespace.QName, boolean)
     */
    public Collection<QName> getSubTypes(QName superType, boolean follow)
    {
    	return dictionaryDAO.getSubTypes(superType, follow);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getTypes(org.alfresco.repo.ref.QName)
     */
    public Collection<QName> getTypes(QName model)
    {
        Collection<TypeDefinition> types = dictionaryDAO.getTypes(model);
        Collection<QName> qnames = new ArrayList<QName>(types.size());
        for (TypeDefinition def : types)
        {
            qnames.add(def.getName());
        }
        return qnames;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAllAspects()
     */
    public Collection<QName> getAllAspects()
    {
        Collection<QName> aspects = new ArrayList<QName>(64);
        for (QName model : getAllModels())
        {
            aspects.addAll(getAspects(model));
        }
        return aspects;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAllAssociations()
     */
    public Collection<QName> getAllAssociations()
    {
        Collection<QName> associations = new ArrayList<QName>(64);
        for (QName model : getAllModels())
        {
            associations.addAll(getAssociations(model));
        }
        return associations;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getSubAspects(org.alfresco.service.namespace.QName, boolean)
     */
    public Collection<QName> getSubAspects(QName superAspect, boolean follow)
    {
    	return dictionaryDAO.getSubAspects(superAspect, follow);
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAspects(org.alfresco.repo.ref.QName)
     */
    public Collection<QName> getAspects(QName model)
    {
        Collection<AspectDefinition> aspects = dictionaryDAO.getAspects(model);
        Collection<QName> qnames = new ArrayList<QName>(aspects.size());
        for (AspectDefinition def : aspects)
        {
            qnames.add(def.getName());
        }
        return qnames;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAssociations(org.alfresco.repo.ref.QName)
     */
    public Collection<QName> getAssociations(QName model)
    {
        Collection<AssociationDefinition> associations = dictionaryDAO.getAssociations(model);
        Collection<QName> qnames = new ArrayList<QName>(associations.size());
        for (AssociationDefinition def : associations)
        {
            qnames.add(def.getName());
        }
        return qnames;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#isSubClass(org.alfresco.repo.ref.QName, org.alfresco.repo.ref.QName)
     */
    public boolean isSubClass(QName className, QName ofClassName)
    {
        // Validate arguments
        ParameterCheck.mandatory("className", className);
        ParameterCheck.mandatory("ofClassName", ofClassName);
        ClassDefinition classDef = getClass(className);
        if (classDef == null)
        {
            return false;
        }
        ClassDefinition ofClassDef = getClass(ofClassName);
        if (ofClassDef == null)
        {
            return false;
        }
        
        // Only check if both ends are either a type or an aspect
        boolean subClassOf = false;
        if (classDef.isAspect() == ofClassDef.isAspect())
        {
            while (classDef != null)
            {
                if (classDef.equals(ofClassDef))
                {
                    subClassOf = true;
                    break;
                }
                
                // No match yet, so go to parent class
                QName parentClassName = classDef.getParentName();
                classDef = (parentClassName == null) ? null : getClass(parentClassName);
            }
        }        
        return subClassOf;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getPropertyType(org.alfresco.repo.ref.QName)
     */
    public DataTypeDefinition getDataType(QName name)
    {
        return dictionaryDAO.getDataType(name);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getDataType(java.lang.Class)
     */
    public DataTypeDefinition getDataType(Class javaClass)
    {
        return dictionaryDAO.getDataType(javaClass);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getType(org.alfresco.repo.ref.QName)
     */
    public TypeDefinition getType(QName name)
    {
        return dictionaryDAO.getType(name);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAspect(org.alfresco.repo.ref.QName)
     */
    public AspectDefinition getAspect(QName name)
    {
        return dictionaryDAO.getAspect(name);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getClass(org.alfresco.repo.ref.QName)
     */
    public ClassDefinition getClass(QName name)
    {
        return dictionaryDAO.getClass(name);
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAnonymousType(org.alfresco.repo.ref.QName, java.util.Collection)
     */
    public TypeDefinition getAnonymousType(QName type, Collection<QName> aspects)
    {
        return dictionaryDAO.getAnonymousType(type, aspects);
    }

    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getProperty(org.alfresco.repo.ref.QName, org.alfresco.repo.ref.QName)
     */
    public PropertyDefinition getProperty(QName className, QName propertyName)
    {
        PropertyDefinition propDef = null;
        ClassDefinition classDef = dictionaryDAO.getClass(className);
        if (classDef != null)
        {
            Map<QName,PropertyDefinition> propDefs = classDef.getProperties();
            propDef = propDefs.get(propertyName);
        }
        return propDef;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getPropertyDefs(org.alfresco.service.namespace.QName)
     */
    public Map<QName,PropertyDefinition> getPropertyDefs(QName className)
    {
        ClassDefinition classDef = dictionaryDAO.getClass(className);
        if (classDef != null)
        {
            return classDef.getProperties();
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getProperty(org.alfresco.repo.ref.QName)
     */
    public PropertyDefinition getProperty(QName propertyName)
    {
        return dictionaryDAO.getProperty(propertyName);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryService#getAssociation(org.alfresco.repo.ref.QName)
     */
    public AssociationDefinition getAssociation(QName associationName)
    {
        return dictionaryDAO.getAssociation(associationName);
    }
    
   /*
    * (non-Javadoc)
    * @see org.alfresco.service.cmr.dictionary.DictionaryService#getAllProperties(org.alfresco.service.namespace.QName)
    */
    public Collection<QName> getAllProperties(QName dataType)
    {
        Collection<QName> aspects = new HashSet<QName>(64);
        for (QName model : getAllModels())
        {
            aspects.addAll(getProperties(model, dataType));
        }
        return aspects;
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getAllProperties(org.alfresco.service.namespace.QName, org.alfresco.service.namespace.QName)
     */
    public Collection<QName> getProperties(QName model, QName dataType)
    {
        Collection<PropertyDefinition> propDefs = dictionaryDAO.getProperties(model, dataType);
        HashSet<QName> props = new HashSet<QName>(propDefs.size());
        for(PropertyDefinition def : propDefs)
        {
            props.add(def.getName());
        }
        return props;
        
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getProperties(org.alfresco.service.namespace.QName)
     */
    public Collection<QName> getProperties(QName model)
    {
        Collection<PropertyDefinition> propDefs = dictionaryDAO.getProperties(model);
        HashSet<QName> props = new HashSet<QName>(propDefs.size());
        for(PropertyDefinition def : propDefs)
        {
            props.add(def.getName());
        }
        return props;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getConstraint(org.alfresco.service.namespace.QName)
     */
    public ConstraintDefinition getConstraint(QName constraintQName)
    {
        return dictionaryDAO.getConstraint(constraintQName);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getConstraints(org.alfresco.service.namespace.QName)
     */
    public Collection<ConstraintDefinition> getConstraints(QName model)
    {
        return dictionaryDAO.getConstraints(model);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.DictionaryService#getConstraints(org.alfresco.service.namespace.QName, boolean)
     */
    public Collection<ConstraintDefinition> getConstraints(QName model, boolean referenceableDefsOnly)
    {
        return dictionaryDAO.getConstraints(model, referenceableDefsOnly);
    }

    public void init()
    {
        dictionaryDAO.init();
    }
    
    public void destroy()
    {
        dictionaryDAO.destroy();
    }
    
    public void onEnableTenant()
    {
        dictionaryDAO.reset(); // to initialise empty dictionary and re-populate
    }
    
    public void onDisableTenant()
    {
        dictionaryDAO.destroy();
    }
    
    
}
