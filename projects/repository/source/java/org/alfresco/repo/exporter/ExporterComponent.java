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
package org.alfresco.repo.exporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.view.ExportPackageHandler;
import org.alfresco.service.cmr.view.Exporter;
import org.alfresco.service.cmr.view.ExporterCrawler;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterException;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.cmr.view.ImporterException;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;



/**
 * Default implementation of the Exporter Service.
 * 
 * @author David Caruana
 */
public class ExporterComponent
    implements ExporterService
{
    // Supporting services
    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private SearchService searchService;
    private ContentService contentService;

    /** Indent Size */
    private int indentSize = 2;
    
    
    /**
     * @param nodeService  the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param searchService  the service to perform path searches
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * @param contentService  the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }
    
    /**
     * @param dictionaryService  the dictionary service
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
    
    /**
     * @param namespaceService  the namespace service
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ExporterService#exportView(java.io.OutputStream, org.alfresco.service.cmr.view.ExporterCrawlerParameters, org.alfresco.service.cmr.view.Exporter)
     */
    public void exportView(OutputStream viewWriter, ExporterCrawlerParameters parameters, Exporter progress)
    {
        ParameterCheck.mandatory("View Writer", viewWriter);
        
        // Construct a basic XML Exporter
        Exporter xmlExporter = createXMLExporter(viewWriter);

        // Export
        exportView(xmlExporter, parameters, progress);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ExporterService#exportView(org.alfresco.service.cmr.view.ExportPackageHandler, org.alfresco.service.cmr.view.ExporterCrawlerParameters, org.alfresco.service.cmr.view.Exporter)
     */
    public void exportView(ExportPackageHandler exportHandler, ExporterCrawlerParameters parameters, Exporter progress)
    {
        ParameterCheck.mandatory("Stream Handler", exportHandler);

        // create exporter around export handler
        exportHandler.startExport();
        OutputStream dataFile = exportHandler.createDataStream();
        Exporter xmlExporter = createXMLExporter(dataFile);
        URLExporter urlExporter = new URLExporter(xmlExporter, exportHandler);

        // export        
        exportView(urlExporter, parameters, progress);
        
        // end export
        exportHandler.endExport();
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ExporterService#exportView(org.alfresco.service.cmr.view.Exporter, org.alfresco.service.cmr.view.ExporterCrawler, org.alfresco.service.cmr.view.Exporter)
     */
    public void exportView(Exporter exporter, ExporterCrawlerParameters parameters, Exporter progress)
    {
        ParameterCheck.mandatory("Exporter", exporter);
        
        ChainedExporter chainedExporter = new ChainedExporter(new Exporter[] {exporter, progress});
        DefaultCrawler crawler = new DefaultCrawler();
        crawler.export(parameters, chainedExporter);
    }
    
    /**
     * Create an XML Exporter that exports repository information to the specified
     * output stream in xml format.
     * 
     * @param viewWriter  the output stream to write to
     * @return  the xml exporter
     */
    private Exporter createXMLExporter(OutputStream viewWriter)
    {
        // Define output format
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(false);
        format.setIndentSize(indentSize);
        format.setEncoding("UTF-8");

        // Construct an XML Exporter
        try
        {
            XMLWriter writer = new XMLWriter(viewWriter, format);
            return new ViewXMLExporter(namespaceService, nodeService, dictionaryService, writer);
        }
        catch (UnsupportedEncodingException e)        
        {
            throw new ExporterException("Failed to create XML Writer for export", e);            
        }
    }
    
    
    /**
     * Responsible for navigating the Repository from specified location and invoking
     * the provided exporter call-back for the actual export implementation.
     * 
     * @author David Caruana
     */
    private class DefaultCrawler implements ExporterCrawler
    {
        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.view.ExporterCrawler#export(org.alfresco.service.cmr.view.Exporter)
         */
        public void export(ExporterCrawlerParameters parameters, Exporter exporter)
        {
            NodeRef nodeRef = getNodeRef(parameters.getExportFrom());
                    
            exporter.start();
            
            if (parameters.isCrawlSelf())
            {
                walkStartNamespaces(parameters, exporter);
                walkNode(nodeRef, parameters, exporter);
                walkEndNamespaces(parameters, exporter);
            }
            else
            {
                // export child nodes only
                List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
                for (ChildAssociationRef childAssoc : childAssocs)
                {
                    walkStartNamespaces(parameters, exporter);
                    walkNode(childAssoc.getChildRef(), parameters, exporter);
                    walkEndNamespaces(parameters, exporter);
                }
            }
            
            exporter.end();
        }
        
        /**
         * Call-backs for start of Namespace scope
         */
        private void walkStartNamespaces(ExporterCrawlerParameters parameters, Exporter exporter)
        {
            Collection<String> prefixes = namespaceService.getPrefixes();
            for (String prefix : prefixes)
            {
                if (!prefix.equals("xml"))
                {
                    String uri = namespaceService.getNamespaceURI(prefix);
                    exporter.startNamespace(prefix, uri);
                }
            }
        }
        
        /**
         * Call-backs for end of Namespace scope
         */
        private void walkEndNamespaces(ExporterCrawlerParameters parameters, Exporter exporter)
        {
            Collection<String> prefixes = namespaceService.getPrefixes();
            for (String prefix : prefixes)
            {
                if (!prefix.equals("xml"))
                {
                    exporter.endNamespace(prefix);
                }
            }
        }
        
        /**
         * Navigate a Node.
         * 
         * @param nodeRef  the node to navigate
         */
        private void walkNode(NodeRef nodeRef, ExporterCrawlerParameters parameters, Exporter exporter)
        {
            // Export node (but only if it's not excluded from export)
            QName type = nodeService.getType(nodeRef);
            if (isExcludedURI(parameters.getExcludeNamespaceURIs(), type.getNamespaceURI()))
            {
                return;
            }
            exporter.startNode(nodeRef);

            // Export node aspects
            exporter.startAspects(nodeRef);
            Set<QName> aspects = nodeService.getAspects(nodeRef);
            for (QName aspect : aspects)
            {
                if (!isExcludedURI(parameters.getExcludeNamespaceURIs(), aspect.getNamespaceURI()))                
                {
                    exporter.startAspect(nodeRef, aspect);
                    exporter.endAspect(nodeRef, aspect);
                }
            }
            exporter.endAspects(nodeRef);
            
            // Export node properties
            exporter.startProperties(nodeRef);
            Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
            for (QName property : properties.keySet())
            {
                // filter out properties whose namespace is excluded
                if (isExcludedURI(parameters.getExcludeNamespaceURIs(), property.getNamespaceURI()))
                {
                    continue;
                }
                
                // filter out properties whose value is null, if not required
                Object value = properties.get(property);
                if (!parameters.isCrawlNullProperties() && value == null)
                {
                    continue;
                }
                
                // start export of property
                exporter.startProperty(nodeRef, property);

                // get the property type
                PropertyDefinition propertyDef = dictionaryService.getProperty(property);
                boolean isContentProperty = propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT);
                
                // TODO: This should test for datatype.content
                if (dictionaryService.isSubClass(type, ContentModel.TYPE_CMOBJECT) && isContentProperty)
                {
                    // export property of datatype CONTENT
                    ContentReader reader = contentService.getReader(nodeRef, property);
                    if (reader == null || reader.exists() == false)
                    {
                        exporter.warning("Failed to read content for property " + property + " on node " + nodeRef);
                    }
                    else
                    {
                        // filter out content if not required
                        if (parameters.isCrawlContent())
                        {
                            InputStream inputStream = reader.getContentInputStream();
                            try
                            {
                                exporter.content(nodeRef, property, inputStream, reader.getContentData());
                            }
                            finally
                            {
                                try
                                {
                                    inputStream.close();
                                }
                                catch(IOException e)
                                {
                                    throw new ExporterException("Failed to export node content for node " + nodeRef, e);
                                }
                            }
                        }
                        else
                        {
                            // skip content values
                            exporter.content(nodeRef, property, null, null);
                        }
                    }
                }
                else
                {
                    // Export all other datatypes
                    try
                    {
                        if (value instanceof Collection)
                        {
                            exporter.value(nodeRef, property, (Collection)value);
                        }
                        else
                        {
                            exporter.value(nodeRef, property, value);
                        }
                    }
                    catch(UnsupportedOperationException e)
                    {
                        exporter.warning("Value of property " + property + " could not be converted to xml string");
                        exporter.value(nodeRef, property, properties.get(property).toString());
                    }
                }

                // end export of property
                exporter.endProperty(nodeRef, property);
            }
            exporter.endProperties(nodeRef);
            
            // Export node children
            if (parameters.isCrawlChildNodes())
            {
                exporter.startAssocs(nodeRef);
                List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
                for (int i = 0; i < childAssocs.size(); i++)
                {
                    ChildAssociationRef childAssoc = childAssocs.get(i);
                    QName childAssocType = childAssoc.getTypeQName();
                    if (isExcludedURI(parameters.getExcludeNamespaceURIs(), childAssocType.getNamespaceURI()))
                    {
                        continue;
                    }

                    if (i == 0 || childAssocs.get(i - 1).getTypeQName().equals(childAssocType) == false)
                    {
                        exporter.startAssoc(nodeRef, childAssocType);
                    }
                    
                    if (!isExcludedURI(parameters.getExcludeNamespaceURIs(), childAssoc.getQName().getNamespaceURI()))
                    {
                        walkNode(childAssoc.getChildRef(), parameters, exporter);
                    }
                    
                    if (i == childAssocs.size() - 1 || childAssocs.get(i + 1).getTypeQName().equals(childAssocType) == false)
                    {
                        exporter.endAssoc(nodeRef, childAssocType);
                    }
                }
                exporter.endAssocs(nodeRef);
            }
            
            // TODO: Export node associations

            // Signal end of node
            exporter.endNode(nodeRef);
        }
        
        /**
         * Is the specified URI an excluded URI?
         * 
         * @param uri  the URI to test
         * @return  true => it's excluded from the export
         */
        private boolean isExcludedURI(String[] excludeNamespaceURIs, String uri)
        {
            for (String excludedURI : excludeNamespaceURIs)
            {
                if (uri.equals(excludedURI))
                {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Get the Node Ref from the specified Location
         * 
         * @param location  the location
         * @return  the node reference
         */
        private NodeRef getNodeRef(Location location)
        {
            ParameterCheck.mandatory("Location", location);
        
            // Establish node to import within
            NodeRef nodeRef = (location == null) ? null : location.getNodeRef();
            if (nodeRef == null)
            {
                // If a specific node has not been provided, default to the root
                nodeRef = nodeService.getRootNode(location.getStoreRef());
            }
        
            // Resolve to path within node, if one specified
            String path = (location == null) ? null : location.getPath();
            if (path != null && path.length() >0)
            {
                // Create a valid path and search
                List<NodeRef> nodeRefs = searchService.selectNodes(nodeRef, path, null, namespaceService, false);
                if (nodeRefs.size() == 0)
                {
                    throw new ImporterException("Path " + path + " within node " + nodeRef + " does not exist - the path must resolve to a valid location");
                }
                if (nodeRefs.size() > 1)
                {
                    throw new ImporterException("Path " + path + " within node " + nodeRef + " found too many locations - the path must resolve to one location");
                }
                nodeRef = nodeRefs.get(0);
            }
        
            // TODO: Check Node actually exists
        
            return nodeRef;
        }
    }

    
}
