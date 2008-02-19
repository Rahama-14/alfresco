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
package org.alfresco.web.scripts.atom;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.alfresco.web.scripts.DescriptionExtension;
import org.alfresco.web.scripts.WebScriptException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


/**
 * ATOM Web Script Description Extensions
 *
 * Extract...
 * 
 * <atom>
 *   <qname name="UUID">{http://www.alfresco.org}uuid</qname>
 *   <qname ... </qname>
 * </atom>
 *
 * @author davidc
 */
public class AtomDescription implements DescriptionExtension 
{
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.DescriptionExtension#parseExtensions(java.io.InputStream)
     */
    public Map<String, Serializable> parseExtensions(String serviceDescPath, InputStream serviceDesc)
    {
        SAXReader reader = new SAXReader();
        try
        {
            Map<String, Serializable> extensions = null;
            Document document = reader.read(serviceDesc);
            Element rootElement = document.getRootElement();
            Element atomElement = rootElement.element("atom");
            if (atomElement != null)
            {
                extensions = new HashMap<String, Serializable>();
                List qnameElements = atomElement.elements("qname");
                if (qnameElements != null && qnameElements.size() > 0)
                {
                    HashMap<String,QName> qnames = new HashMap<String,QName>();
                    Iterator iterElements = qnameElements.iterator();
                    while(iterElements.hasNext())
                    {
                        Element qnameElement = (Element)iterElements.next();
                        String name = qnameElement.attributeValue("name");
                        if (name == null || name.length() == 0)
                        {
                            throw new WebScriptException("Expected 'name' attribute on <qname> element");
                        }
                        String qnameStr = qnameElement.getTextTrim();
                        if (qnameStr == null || qnameStr.length() == 0)
                        {
                            throw new WebScriptException("Expected <qname> element value");
                        }
                        qnames.put(name, QName.valueOf(qnameStr));
                    }
                    extensions.put("qnames", qnames);
                }
            }
            return extensions;
        }
        catch(DocumentException e)
        {
            throw new WebScriptException("Failed to parse web script description document " + serviceDescPath, e);
        }
    }

}
