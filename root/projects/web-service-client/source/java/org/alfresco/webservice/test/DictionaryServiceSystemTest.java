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
package org.alfresco.webservice.test;

import org.alfresco.webservice.dictionary.ClassPredicate;
import org.alfresco.webservice.types.AssociationDefinition;
import org.alfresco.webservice.types.ClassDefinition;
import org.alfresco.webservice.types.PropertyDefinition;
import org.alfresco.webservice.util.WebServiceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DictionaryServiceSystemTest extends BaseWebServiceSystemTest
{
    @SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(DictionaryServiceSystemTest.class);

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    public void testGetClasses() throws Exception
    {
        ClassDefinition[] classDefs = WebServiceFactory.getDictionaryService().getClasses(null, null);
        assertNotNull(classDefs);
        assertTrue(classDefs.length >= 1);
    }

    public void testSingleTypePredicate() throws Exception
    {
        ClassPredicate types = new ClassPredicate(new String[] {"cm:content"}, false, false);
        ClassPredicate aspects = new ClassPredicate(new String[] {}, false, false);
        
        ClassDefinition[] classDefs = WebServiceFactory.getDictionaryService().getClasses(types, aspects);
        assertNotNull(classDefs);
        assertEquals(1, classDefs.length);
        assertEquals("{http://www.alfresco.org/model/content/1.0}content", classDefs[0].getName());
    }

    public void testSingleAspectPredicate() throws Exception
    {
        ClassPredicate types = new ClassPredicate(new String[] {}, false, false);
        ClassPredicate aspects = new ClassPredicate(new String[] {"cm:auditable"}, false, false);
        
        ClassDefinition[] classDefs = WebServiceFactory.getDictionaryService().getClasses(types, aspects);
        assertNotNull(classDefs);
        assertEquals(1, classDefs.length);
        assertEquals("{http://www.alfresco.org/model/content/1.0}auditable", classDefs[0].getName());
    }
    
    public void testSingleTypeAspectPredicate() throws Exception
    {
        ClassPredicate types = new ClassPredicate(new String[] {"cm:content"}, false, false);
        ClassPredicate aspects = new ClassPredicate(new String[] {"cm:auditable"}, false, false);
        
        ClassDefinition[] classDefs = WebServiceFactory.getDictionaryService().getClasses(types, aspects);
        assertNotNull(classDefs);
        assertEquals(2, classDefs.length);
    }
    
    public void testSingleTypeAllAspectsPredicate() throws Exception
    {
        ClassPredicate types = new ClassPredicate(new String[] {"cm:content"}, false, false);
        ClassDefinition[] classDefs = WebServiceFactory.getDictionaryService().getClasses(types, null);
        assertNotNull(classDefs);
        assertTrue(classDefs.length > 1);
    }

    public void testSingleAspectAllTypesPredicate() throws Exception
    {
        ClassPredicate aspects = new ClassPredicate(new String[] {"cm:auditable"}, false, false);
        ClassDefinition[] classDefs = WebServiceFactory.getDictionaryService().getClasses(null, aspects);
        assertNotNull(classDefs);
        assertTrue(classDefs.length > 1);
    }

    public void testTypeWithSubTypesPredicate() throws Exception
    {
        ClassPredicate types = new ClassPredicate(new String[] {"cm:content"}, true, false);
        ClassDefinition[] classDefs = WebServiceFactory.getDictionaryService().getClasses(types, null);
        assertNotNull(classDefs);
        assertTrue(classDefs.length > 1);
    }
    
    public void testGetProperties() throws Exception
    {
        PropertyDefinition[] propDefs = WebServiceFactory.getDictionaryService().getProperties(new String[] {"cm:modified", "cm:creator"});
        assertNotNull(propDefs);
        assertTrue(propDefs.length == 2);
        assertEquals("{http://www.alfresco.org/model/content/1.0}modified", propDefs[0].getName());
        assertEquals("{http://www.alfresco.org/model/content/1.0}creator", propDefs[1].getName());
    }
    
    public void testGetAssociations() throws Exception
    {
        AssociationDefinition[] assocDefs = WebServiceFactory.getDictionaryService().getAssociations(new String[] {"sys:children", "cm:contains"});
        assertNotNull(assocDefs);
        assertTrue(assocDefs.length == 2);
        assertEquals("{http://www.alfresco.org/model/system/1.0}children", assocDefs[0].getName());
        assertEquals("{http://www.alfresco.org/model/content/1.0}contains", assocDefs[1].getName());
    }

    public void testisSubClass() throws Exception
    {
        boolean test1 = WebServiceFactory.getDictionaryService().isSubClass("cm:content", "sys:base");
        assertTrue(test1);
        boolean test2 = WebServiceFactory.getDictionaryService().isSubClass("sys:base", "cm:content");
        assertTrue(!test2);
    }
    
}
