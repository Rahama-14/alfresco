/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.repo.jscript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;


/**
 * Value conversion allowing safe usage of values in Script and Java.
 */
public class ValueConverter
{
    private static final String TYPE_DATE = "Date";
    
    /**
     * Convert an object from any repository serialized value to a valid script object.
     * This includes converting Collection multi-value properties into JavaScript Array objects.
     *
     * @param services  Repository Services Registry
     * @param scope     Scripting scope
     * @param qname     QName of the property value for conversion
     * @param value     Property value
     * 
     * @return Value safe for scripting usage
     */
    public Serializable convertValueForScript(ServiceRegistry services, Scriptable scope, QName qname, Serializable value)
    {
        // perform conversions from Java objects to JavaScript scriptable instances
        if (value == null)
        {
            return null;
        }
        else if (value instanceof NodeRef)
        {
            // NodeRef object properties are converted to new Node objects
            // so they can be used as objects within a template
            value = new ScriptNode(((NodeRef)value), services, scope);
        }
        else if (value instanceof QName || value instanceof StoreRef)
        {
        	   value = value.toString();
        }
        else if (value instanceof ChildAssociationRef)
        {
        	   value = new ChildAssociation(services, (ChildAssociationRef)value, scope);
        }
        else if (value instanceof AssociationRef)
        {
        	   value = new Association(services, (AssociationRef)value, scope);
        }
        else if (value instanceof Date)
        {
            // convert Date to JavaScript native Date object
            // call the "Date" constructor on the root scope object - passing in the millisecond
            // value from the Java date - this will construct a JavaScript Date with the same value
            Date date = (Date)value;
            Object val = ScriptRuntime.newObject(
                    Context.getCurrentContext(), scope, TYPE_DATE, new Object[] {date.getTime()});
            value = (Serializable)val;
        }
        else if (value instanceof Collection)
        {
            // recursively convert each value in the collection
            Collection<Serializable> collection = (Collection<Serializable>)value;
            Object[] array = new Object[collection.size()];
            int index = 0;
            for (Serializable obj : collection)
            {
                array[index++] = convertValueForScript(services, scope, qname, obj);
            }
            // convert array to a native JavaScript Array
            value = (Serializable)Context.getCurrentContext().newArray(scope, array);
        }
        // simple numbers and strings are wrapped automatically by Rhino
        
        return value;
    }
    
    
    /**
     * Convert an object from any script wrapper value to a valid repository serializable value.
     * This includes converting JavaScript Array objects to Lists of valid objects.
     * 
     * @param value     Value to convert from script wrapper object to repo serializable value
     * 
     * @return valid repo value
     */
    public Serializable convertValueForRepo(Serializable value)
    {
        if (value == null)
        {
            return null;
        }
        else if (value instanceof ScriptNode)
        {
            // convert back to NodeRef
            value = ((ScriptNode)value).getNodeRef();
        }
        else if (value instanceof ChildAssociation)
        {
        	   value = ((ChildAssociation)value).getChildAssociationRef();
        }
        else if (value instanceof Association)
        {
        	   value = ((Association)value).getAssociationRef();
        }
        else if (value instanceof Wrapper)
        {
            // unwrap a Java object from a JavaScript wrapper
            // recursively call this method to convert the unwrapped value
            value = convertValueForRepo((Serializable)((Wrapper)value).unwrap());
        }
        else if (value instanceof ScriptableObject)
        {
            // a scriptable object will probably indicate a multi-value property
            // set using a JavaScript Array object
            ScriptableObject values = (ScriptableObject)value;
            
            if (value instanceof IdScriptableObject)
            {
                // TODO: add code here to use the dictionary and convert to correct value type
                if (TYPE_DATE.equals(((IdScriptableObject)value).getClassName()))
                {
                    Object javaObj = Context.jsToJava(value, Date.class);
                    if (javaObj instanceof Serializable)
                    {
                        value = (Serializable)javaObj;
                    }
                }
                else if (value instanceof NativeArray)
                {
                    // convert JavaScript array of values to a List of Serializable objects
                    Object[] propIds = values.getIds();
                    if (isArray(propIds) == true)
                    {                    
                        List<Serializable> propValues = new ArrayList<Serializable>(propIds.length);
                        for (int i=0; i<propIds.length; i++)
                        {
                            // work on each key in turn
                            Object propId = propIds[i];
                            
                            // we are only interested in keys that indicate a list of values
                            if (propId instanceof Integer)
                            {
                                // get the value out for the specified key
                                Serializable val = (Serializable)values.get((Integer)propId, values);
                                // recursively call this method to convert the value
                                propValues.add(convertValueForRepo(val));
                            }
                        }

                        value = (Serializable)propValues;
                    }
                    else
                    {
                        Map<Serializable, Serializable> propValues = new HashMap<Serializable, Serializable>(propIds.length);
                        for (Object propId : propIds)
                        {
                            // Get the value and add to the map
                            Serializable val = (Serializable)values.get(propId.toString(), values);
                            propValues.put(convertValueForRepo((Serializable)propId), convertValueForRepo(val));
                        }
                        
                        value = (Serializable)propValues;
                    }
                }
                else
                {
                    // convert JavaScript map to values to a Map of Serializable objects
                    Object[] propIds = values.getIds();
                    Map<String, Serializable> propValues = new HashMap<String, Serializable>(propIds.length);
                    for (int i=0; i<propIds.length; i++)
                    {
                        // work on each key in turn
                        Object propId = propIds[i];
                        
                        // we are only interested in keys that indicate a list of values
                        if (propId instanceof String)
                        {
                            // get the value out for the specified key
                            Serializable val = (Serializable)values.get((String)propId, values);
                            // recursively call this method to convert the value
                            propValues.put((String)propId, convertValueForRepo(val));
                        }
                    }
                    value = (Serializable)propValues;
                }
            }
        }
        else if (value instanceof Serializable[])
        {
            // convert back a list of Java values
            Serializable[] array = (Serializable[])value;
            ArrayList<Serializable> list = new ArrayList<Serializable>(array.length);
            for (int i=0; i<array.length; i++)
            {
                list.add(convertValueForRepo(array[i]));
            }
            value = list;
        }
        return value;
    }
    
    /**
     * Look at the id's of a native array and try to determine whether it's actually an Array or a Hashmap
     * 
     * @param ids       id's of the native array
     * @return boolean  true if it's an array, false otherwise (ie it's a map)
     */
    private boolean isArray(Object[] ids)
    {
        boolean result = true;
        for (Object id : ids)
        {
            if (id instanceof Integer == false)
            {
               result = false;
               break;
            }
        }
        return result;
    }
    
}
