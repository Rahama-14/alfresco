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
package org.alfresco.jcr.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.alfresco.jcr.item.JCRMixinTypesProperty;
import org.alfresco.jcr.item.JCRPrimaryTypeProperty;
import org.alfresco.jcr.item.JCRUUIDProperty;
import org.alfresco.jcr.item.NodeImpl;
import org.alfresco.jcr.item.PropertyImpl;
import org.alfresco.jcr.session.SessionImpl;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.view.Exporter;
import org.alfresco.service.cmr.view.ExporterException;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Base64;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * Alfresco Implementation of JCR Document XML Exporter
 * 
 * @author David Caruana
 */
public class JCRDocumentXMLExporter implements Exporter
{

    private SessionImpl session;
    private ContentHandler contentHandler;
    private List<QName> currentProperties = new ArrayList<QName>();
    private List<Object> currentValues = new ArrayList<Object>();
    

    /**
     * Construct
     * 
     * @param namespaceService  namespace service
     * @param nodeService  node service
     * @param contentHandler  content handler
     */
    public JCRDocumentXMLExporter(SessionImpl session, ContentHandler contentHandler)
    {
        this.session = session;
        this.contentHandler = contentHandler;
    }
    
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#start()
     */
    public void start()
    {
        try
        {
            contentHandler.startDocument();
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process export start event", e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startNamespace(java.lang.String, java.lang.String)
     */
    public void startNamespace(String prefix, String uri)
    {
        try
        {
            contentHandler.startPrefixMapping(prefix, uri);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process start namespace event - prefix " + prefix + " uri " + uri, e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endNamespace(java.lang.String)
     */
    public void endNamespace(String prefix)
    {
        try
        {
            contentHandler.endPrefixMapping(prefix);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end namespace event - prefix " + prefix, e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startNode(NodeRef nodeRef)
    {
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endNode(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endNode(NodeRef nodeRef)
    {
        try
        {
            QName nodeName = getNodeName(nodeRef);
            contentHandler.endElement(nodeName.getNamespaceURI(), nodeName.getLocalName(), toPrefixString(nodeName));
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end node event - node ref " + nodeRef.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAspects(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startAspects(NodeRef nodeRef)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAspects(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endAspects(NodeRef nodeRef)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAspect(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void startAspect(NodeRef nodeRef, QName aspect)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAspect(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void endAspect(NodeRef nodeRef, QName aspect)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startProperties(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startProperties(NodeRef nodeRef)
    {
        currentProperties.clear();
        currentValues.clear();
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endProperties(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endProperties(NodeRef nodeRef)
    {
        try
        {
            // create node attributes
            AttributesImpl attrs = new AttributesImpl(); 
            
            // primary type
            NodeImpl nodeImpl = new NodeImpl(session, nodeRef);
            PropertyImpl primaryType = new JCRPrimaryTypeProperty(nodeImpl);
            attrs.addAttribute(JCRPrimaryTypeProperty.PROPERTY_NAME.getNamespaceURI(), JCRPrimaryTypeProperty.PROPERTY_NAME.getLocalName(), 
                toPrefixString(JCRPrimaryTypeProperty.PROPERTY_NAME), null, getValue(primaryType.getValue().getString()));
            
            // mixin type
            PropertyImpl mixinTypes = new JCRMixinTypesProperty(nodeImpl);
            Collection<String> mixins = new ArrayList<String>();
            for (Value value : mixinTypes.getValues())
            {
                mixins.add(value.getString());
            }
            attrs.addAttribute(JCRMixinTypesProperty.PROPERTY_NAME.getNamespaceURI(), JCRMixinTypesProperty.PROPERTY_NAME.getLocalName(), 
                toPrefixString(JCRMixinTypesProperty.PROPERTY_NAME), null, getCollectionValue(mixins));

            // uuid (for mix:referencable)
            attrs.addAttribute(JCRUUIDProperty.PROPERTY_NAME.getNamespaceURI(), JCRUUIDProperty.PROPERTY_NAME.getLocalName(), 
                toPrefixString(JCRUUIDProperty.PROPERTY_NAME), null, getValue(nodeRef.getId()));
            
            // node properties
            for (int i = 0; i < currentProperties.size(); i++)
            {
                Object value = currentValues.get(i);
                String strValue = (value instanceof Collection) ? getCollectionValue((Collection)value) : getValue(value);
                attrs.addAttribute(currentProperties.get(i).getNamespaceURI(), currentProperties.get(i).getLocalName(),
                    toPrefixString(currentProperties.get(i)), null, strValue);
            }
            
            // emit node element
            QName nodeName = getNodeName(nodeRef);
            contentHandler.startElement(nodeName.getNamespaceURI(), nodeName.getLocalName(), toPrefixString(nodeName), attrs);
        }
        catch (ValueFormatException e)
        {
            throw new ExporterException("Failed to process properties event - nodeRef " + nodeRef);
        }
        catch (RepositoryException e)
        {
            throw new ExporterException("Failed to process properties event - nodeRef " + nodeRef);
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process properties event - nodeRef " + nodeRef);
        }
    }    
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startProperty(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void startProperty(NodeRef nodeRef, QName property)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endProperty(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void endProperty(NodeRef nodeRef, QName property)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#value(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName, java.io.Serializable)
     */
    public void value(NodeRef nodeRef, QName property, Object value)
    {
        currentProperties.add(property);
        currentValues.add(value);
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#value(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName, java.util.Collection)
     */
    public void value(NodeRef nodeRef, QName property, Collection values)
    {
        currentProperties.add(property);
        currentValues.add(values);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#content(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName, java.io.InputStream)
     */
    public void content(NodeRef nodeRef, QName property, InputStream content, ContentData contentData)
    {
        try
        {
            StringBuffer strValue = new StringBuffer(9 * 1024);
            if (content != null)
            {
                // emit base64 encoded content
                InputStream base64content = new Base64.InputStream(content, Base64.ENCODE | Base64.DONT_BREAK_LINES);
                byte[] buffer = new byte[9 * 1024];
                int read;
                while ((read = base64content.read(buffer, 0, buffer.length)) > 0)
                {
                    String characters = new String(buffer, 0, read);
                    strValue.append(characters);
                }
            }
            currentProperties.add(property);
            currentValues.add(strValue.toString());
        }
        catch (IOException e)
        {
            throw new ExporterException("Failed to process content event - nodeRef " + nodeRef + "; property " + toPrefixString(property));
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAssoc(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void startAssoc(NodeRef nodeRef, QName assoc)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAssoc(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
     */
    public void endAssoc(NodeRef nodeRef, QName assoc)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#startAssocs(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void startAssocs(NodeRef nodeRef)
    {
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#endAssocs(org.alfresco.service.cmr.repository.NodeRef)
     */
    public void endAssocs(NodeRef nodeRef)
    {
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#warning(java.lang.String)
     */
    public void warning(String warning)
    {
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.Exporter#end()
     */
    public void end()
    {
        try
        {
            contentHandler.endDocument();
        }
        catch (SAXException e)
        {
            throw new ExporterException("Failed to process end export event", e);
        }
    }

    /**
     * Get the prefix for the specified URI
     * @param uri  the URI
     * @return  the prefix (or null, if one is not registered)
     */
    private String toPrefixString(QName qname)
    {
        return qname.toPrefixString(session.getNamespaceResolver());
    }
    
    /**
     * Get name of Node
     * 
     * @param nodeRef  node reference
     * @return  node name
     */
    private QName getNodeName(NodeRef nodeRef)
    {
        // establish name of node
        String childName;
        NodeService nodeService = session.getRepositoryImpl().getServiceRegistry().getNodeService();
        NodeRef rootNode = nodeService.getRootNode(nodeRef.getStoreRef());
        if (rootNode.equals(nodeRef))
        {
            childName = "jcr:root";
        }
        else
        {
            Path path = nodeService.getPath(nodeRef);
            childName = path.last().getElementString();
        }
        QName childQName = QName.createQName(childName);
        return childQName;
    }
    
    /**
     * Get single-valued property
     * 
     * @param value
     * @return
     */
    private String getValue(Object value)
    {
        String strValue = session.getTypeConverter().getConverter().convert(String.class, value);
        return encodeBlanks(strValue);
    }
    
    /**
     * Get multi-valued property
     * 
     * @param values
     * @return
     */
    private String getCollectionValue(Collection values)
    {
        Collection<String> strValues = session.getTypeConverter().getConverter().convert(String.class, values);
        StringBuffer buffer = new StringBuffer();
        int i = 0;
        for (String strValue : strValues)
        {
            buffer.append(encodeBlanks(strValue));
            i++;
            if (i < strValues.size())
            {
                buffer.append(" ");
            }
        }
        return buffer.toString();
    }
    
    /**
     * Encode blanks in value
     * 
     * @param value
     * @return
     */
    private String encodeBlanks(String value)
    {
        return value.replaceAll(" ", "_x0020_");    
    }

}
