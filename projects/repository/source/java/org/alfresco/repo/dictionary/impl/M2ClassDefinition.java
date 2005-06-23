/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.dictionary.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ChildAssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;


/**
 * Compiled Class Definition
 * 
 * @author David Caruana
 */
/*package*/ class M2ClassDefinition implements ClassDefinition
{
    protected M2Class m2Class;
    protected QName name;
    protected QName parentName = null;
    
    private Map<QName, M2PropertyOverride> propertyOverrides = new HashMap<QName, M2PropertyOverride>();
    private Map<QName, PropertyDefinition> properties = new HashMap<QName, PropertyDefinition>();
    private Map<QName, PropertyDefinition> inheritedProperties = new HashMap<QName, PropertyDefinition>();
    private Map<QName, AssociationDefinition> associations = new HashMap<QName, AssociationDefinition>();
    private Map<QName, AssociationDefinition> inheritedAssociations = new HashMap<QName, AssociationDefinition>();
    private Map<QName, ChildAssociationDefinition> inheritedChildAssociations = new HashMap<QName, ChildAssociationDefinition>();
    

    /**
     * Construct
     * 
     * @param m2Class  class definition
     * @param resolver  namepsace resolver
     * @param modelProperties  global list of model properties
     * @param modelAssociations  global list of model associations
     */
    /*package*/ M2ClassDefinition(M2Class m2Class, NamespacePrefixResolver resolver, Map<QName, PropertyDefinition> modelProperties, Map<QName, AssociationDefinition> modelAssociations)
    {
        this.m2Class = m2Class;
        
        // Resolve Names
        this.name = QName.createQName(m2Class.getName(), resolver);
        if (m2Class.getParentName() != null && m2Class.getParentName().length() > 0)
        {
            this.parentName = QName.createQName(m2Class.getParentName(), resolver);
        }
        
        // Construct Properties
        for (M2Property property : m2Class.getProperties())
        {
            PropertyDefinition def = new M2PropertyDefinition(this, property, resolver);
            if (properties.containsKey(def.getName()))
            {
                throw new DictionaryException("Found duplicate property definition " + def.getName().toPrefixString() + " within class " + name.toPrefixString());
            }
            
            // Check for existence of property elsewhere within the model
            PropertyDefinition existingDef = modelProperties.get(def.getName());
            if (existingDef != null)
            {
                // TODO: Consider sharing property, if property definitions are equal
                throw new DictionaryException("Found duplicate property definition " + def.getName().toPrefixString() + " within class " 
                    + name.toPrefixString() + " and class " + existingDef.getContainerClass().getName().toPrefixString());
            }
            
            properties.put(def.getName(), def);
            modelProperties.put(def.getName(), def);
        }
        
        // Construct Associations
        for (M2ClassAssociation assoc : m2Class.getAssociations())
        {
            AssociationDefinition def;
            if (assoc instanceof M2ChildAssociation)
            {
                def = new M2ChildAssociationDefinition(this, (M2ChildAssociation)assoc, resolver);
            }
            else
            {
                def = new M2AssociationDefinition(this, assoc, resolver);
            }
            if (associations.containsKey(def.getName()))
            {
                throw new DictionaryException("Found duplicate association definition " + def.getName().toPrefixString() + " within class " + name.toPrefixString());
            }
            
            // Check for existence of association elsewhere within the model
            AssociationDefinition existingDef = modelAssociations.get(def.getName());
            if (existingDef != null)
            {
                // TODO: Consider sharing association, if association definitions are equal
                throw new DictionaryException("Found duplicate association definition " + def.getName().toPrefixString() + " within class " 
                    + name.toPrefixString() + " and class " + existingDef.getSourceClass().getName().toPrefixString());
            }
            
            associations.put(def.getName(), def);
            modelAssociations.put(def.getName(), def);
        }
        
        // Construct Property overrides
        for (M2PropertyOverride override : m2Class.getPropertyOverrides())
        {
            QName overrideName = QName.createQName(override.getName(), resolver);
            if (properties.containsKey(overrideName))
            {
                throw new DictionaryException("Found duplicate property and property override definition " + overrideName.toPrefixString() + " within class " + name.toPrefixString());
            }
            if (propertyOverrides.containsKey(overrideName))
            {
                throw new DictionaryException("Found duplicate property override definition " + overrideName.toPrefixString() + " within class " + name.toPrefixString());
            }
            propertyOverrides.put(overrideName, override);
        }
    }
    
    
    /*package*/ void resolveDependencies(ModelQuery query)
    {
        if (parentName != null)
        {
            ClassDefinition parent = query.getClass(parentName);
            if (parent == null)
            {
                throw new DictionaryException("Parent class " + parentName.toPrefixString() + " of class " + name.toPrefixString() + " is not found");
            }
        }
        
        for (PropertyDefinition def : properties.values())
        {
            ((M2PropertyDefinition)def).resolveDependencies(query);
        }
        for (AssociationDefinition def : associations.values())
        {
            ((M2AssociationDefinition)def).resolveDependencies(query);
        }
    }
    

    /*package*/ void resolveInheritance(ModelQuery query)
    {
        // Retrieve parent class
        ClassDefinition parentClass = (parentName == null) ? null : query.getClass(parentName);
        
        // Build list of inherited properties (and process overridden values)
        if (parentClass != null)
        {
            for (PropertyDefinition def : parentClass.getProperties().values())
            {
                M2PropertyOverride override = propertyOverrides.get(def.getName());
                if (override == null)
                {
                    inheritedProperties.put(def.getName(), def);
                }
                else
                {
                    inheritedProperties.put(def.getName(), new M2PropertyDefinition(this, def, override));
                }
            }
        }
        
        // Append list of defined properties
        for (PropertyDefinition def : properties.values())
        {
            if (inheritedProperties.containsKey(def.getName()))
            {
                throw new DictionaryException("Duplicate property definition " + def.getName().toPrefixString() + " found in class hierarchy of " + name.toPrefixString()); 
            }
            inheritedProperties.put(def.getName(), def);
        }
        
        // Build list of inherited associations
        if (parentClass != null)
        {
            inheritedAssociations.putAll(parentClass.getAssociations());
        }
        
        // Append list of defined associations
        for (AssociationDefinition def : associations.values())
        {
            if (inheritedAssociations.containsKey(def.getName()))
            {
                throw new DictionaryException("Duplicate association definition " + def.getName().toPrefixString() + " found in class hierarchy of " + name.toPrefixString()); 
            }
            inheritedAssociations.put(def.getName(), def);
        }

        // Derive Child Associations
        for (AssociationDefinition def : inheritedAssociations.values())
        {
            if (def instanceof ChildAssociationDefinition)
            {
                inheritedChildAssociations.put(def.getName(), (ChildAssociationDefinition)def);
            }
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getName()
     */
    public QName getName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getTitle()
     */
    public String getTitle()
    {
        return m2Class.getTitle();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getDescription()
     */
    public String getDescription()
    {
        return m2Class.getDescription();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#isAspect()
     */
    public boolean isAspect()
    {
        return (m2Class instanceof M2Aspect);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getParentName()
     */
    public QName getParentName()
    {
        return parentName;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getProperties()
     */
    public Map<QName, PropertyDefinition> getProperties()
    {
        return Collections.unmodifiableMap(inheritedProperties);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getAssociations()
     */
    public Map<QName, AssociationDefinition> getAssociations()
    {
        return Collections.unmodifiableMap(inheritedAssociations);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.dictionary.ClassDefinition#isContainer()
     */
    public boolean isContainer()
    {
        return !inheritedChildAssociations.isEmpty();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.dictionary.ClassDefinition#getChildAssociations()
     */
    public Map<QName, ChildAssociationDefinition> getChildAssociations()
    {
        return Collections.unmodifiableMap(inheritedChildAssociations);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof M2ClassDefinition))
        {
            return false;
        }
        return name.equals(((M2ClassDefinition)obj).name);
    }

}
