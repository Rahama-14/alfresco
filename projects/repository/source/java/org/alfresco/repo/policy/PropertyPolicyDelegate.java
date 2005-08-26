/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;


/**
 * Delegate for a Class Feature-level (Property and Association) Policies.  Provides 
 * access to Policy Interface implementations which invoke the appropriate bound behaviours.
 *  
 * @author David Caruana
 *
 * @param <P>  the policy interface
 */
public class PropertyPolicyDelegate<P extends PropertyPolicy>
{
    private DictionaryService dictionary;
    private CachedPolicyFactory<ClassFeatureBehaviourBinding, P> factory;


    /**
     * Construct.
     * 
     * @param dictionary  the dictionary service
     * @param policyClass  the policy interface class
     * @param index  the behaviour index to query against
     */
    PropertyPolicyDelegate(DictionaryService dictionary, Class<P> policyClass, BehaviourIndex<ClassFeatureBehaviourBinding> index)
    {
        // Get list of all pre-registered behaviours for the policy and
        // ensure they are valid.
        Collection<BehaviourDefinition> definitions = index.getAll();
        for (BehaviourDefinition definition : definitions)
        {
            definition.getBehaviour().getInterface(policyClass);
        }

        // Rely on cached implementation of policy factory
        // Note: Could also use PolicyFactory (without caching)
        this.factory = new CachedPolicyFactory<ClassFeatureBehaviourBinding, P>(policyClass, index);
        this.dictionary = dictionary;
    }
    
    /**
     * Ensures the validity of the given property type
     * 
     * @param assocTypeQName
     * @throws IllegalArgumentException
     */
    private void checkPropertyType(QName propertyQName) throws IllegalArgumentException
    {
        PropertyDefinition propertyDef = dictionary.getProperty(propertyQName);
        if (propertyDef == null)
        {
            throw new IllegalArgumentException("Property " + propertyQName + " has not been defined in the data dictionary");
        }
    }


    /**
     * Gets the Policy implementation for the specified Class and Propery
     * 
     * When multiple behaviours are bound to the policy for the class feature, an
     * aggregate policy implementation is returned which invokes each policy
     * in turn.
     * 
     * @param classQName  the class qualified name
     * @param propertyQName  the property qualified name
     * @return  the policy
     */
    public P get(QName classQName, QName propertyQName)
    {
        checkPropertyType(propertyQName);
        return factory.create(new ClassFeatureBehaviourBinding(dictionary, classQName, propertyQName));
    }

    
    /**
     * Gets the collection of Policy implementations for the specified Class and Property
     * 
     * @param classQName  the class qualified name
     * @param propertyQName  the property qualified name
     * @return  the collection of policies
     */
    public Collection<P> getList(QName classQName, QName propertyQName)
    {
        checkPropertyType(propertyQName);
        return factory.createList(new ClassFeatureBehaviourBinding(dictionary, classQName, propertyQName));
    }

    /**
     * Gets a <tt>Policy</tt> for all the given Class and Property
     * 
     * @param classQNames the class qualified names
     * @param propertyQName the property qualified name
     * @return Return the policy
     */
    public P get(Set<QName> classQNames, QName propertyQName)
    {
        checkPropertyType(propertyQName);
        return factory.toPolicy(getList(classQNames, propertyQName));
    }
    
    /**
     * Gets the <tt>Policy</tt> instances for all the given Classes and Properties
     * 
     * @param classQNames the class qualified names
     * @param propertyQName the property qualified name
     * @return Return the policies
     */
    public Collection<P> getList(Set<QName> classQNames, QName propertyQName)
    {
        checkPropertyType(propertyQName);
        Collection<P> policies = new HashSet<P>();
        for (QName classQName : classQNames)
        {
            P policy = factory.create(new ClassFeatureBehaviourBinding(dictionary, classQName, propertyQName));
			if (policy instanceof PolicyList)
			{
				policies.addAll(((PolicyList<P>)policy).getPolicies());
			}
			else
			{
				policies.add(policy);
			}
        }
        return policies;
    }
}
