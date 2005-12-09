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
package org.alfresco.jcr.importer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.importer.view.ElementContext;
import org.alfresco.repo.importer.view.NodeContext;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.view.ImporterException;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;


/**
 * Maintains state about currently imported Property
 * 
 * @author David Caruana
 *
 */
public class PropertyContext extends ElementContext
{
    private NodeContext parentContext;
    private QName propertyName;
    private QName propertyType;
    
    private List<StringBuffer> values = new ArrayList<StringBuffer>();
    private Map<QName, FileWriter> contentWriters = new HashMap<QName, FileWriter>();
    
    
    /**
     * Construct
     * 
     * @param elementName
     * @param parentContext
     * @param propertyName
     * @param propertyType
     */
    public PropertyContext(QName elementName, NodeContext parentContext, QName propertyName, QName propertyType)
    {
        super(elementName, parentContext.getDictionaryService(), parentContext.getImporter());
        this.parentContext = parentContext;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    /**
     * Get node containing property
     * 
     * @return  node
     */
    public NodeContext getNode()
    {
        return parentContext;
    }
    
    /**
     * Get property name
     * 
     * @return  property name
     */
    public QName getName()
    {
        return propertyName;
    }

    /**
     * Get property type
     * 
     * @return  property type
     */
    public QName getType()
    {
        return propertyType;
    }
    
    /**
     * Is property multi-valued?
     * 
     * @return  true => multi-valued; false => single value
     */
    public boolean isMultiValue()
    {
        return values.size() > 1;
    }
    
    /**
     * Is null property value
     * 
     * @return  true => value has not been provided
     */
    public boolean isNull()
    {
        return values.size() == 0;
    }
    
    /**
     * Get property values
     * 
     * @return  values
     */
    public List<StringBuffer> getValues()
    {
        return values;
    }
    
    /**
     * Start a new property value
     */
    public void startValue()
    {
        StringBuffer buffer = new StringBuffer(128);
        if (propertyType.equals(DataTypeDefinition.CONTENT))
        {            
            // create temporary file to hold content
            File tempFile = TempFileProvider.createTempFile("import", ".tmp");
            try
            {
                FileWriter tempWriter = new FileWriter(tempFile);
                contentWriters.put(propertyName, tempWriter);
                ContentData contentData = new ContentData(tempFile.getAbsolutePath(), MimetypeMap.MIMETYPE_BINARY, 0, tempWriter.getEncoding());
                buffer.append(contentData.toString());
            }
            catch(IOException e)
            {
                throw new ImporterException("Failed to create temporary content holder for property " + propertyName, e);
            }
        }
        values.add(buffer);
    }

    /**
     * End a property value
     */
    public void endValue()
    {
        if (propertyType.equals(DataTypeDefinition.CONTENT))
        {
            // close content writer
            FileWriter tempWriter = contentWriters.get(propertyName);
            try
            {
                tempWriter.close();
                contentWriters.remove(propertyName);
            }
            catch(IOException e)
            {
                throw new ImporterException("Failed to create temporary content holder for property " + propertyName, e);
            }
        }
    }
    
    /**
     * Append property value characters
     * 
     * @param ch
     * @param start
     * @param length
     */
    public void appendCharacters(char[] ch, int start, int length)
    {
        if (propertyType.equals(DataTypeDefinition.CONTENT))
        {
            FileWriter tempWriter = contentWriters.get(propertyName);
            try
            {
                tempWriter.write(ch, start, length);
            }
            catch(IOException e)
            {
                throw new ImporterException("Failed to write temporary content for property " + propertyName, e);
            }
        }
        else
        {
            StringBuffer buffer = values.get(values.size() -1);
            buffer.append(ch, start, length);
        }
    }
    
}
