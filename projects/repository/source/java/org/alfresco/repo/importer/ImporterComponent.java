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
package org.alfresco.repo.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.ChildAssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.XPathException;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.view.ImportPackageHandler;
import org.alfresco.service.cmr.view.ImporterBinding;
import org.alfresco.service.cmr.view.ImporterException;
import org.alfresco.service.cmr.view.ImporterProgress;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;


/**
 * Default implementation of the Importer Service
 *  
 * @author David Caruana
 */
public class ImporterComponent
    implements ImporterService
{
    // default importer
    // TODO: Allow registration of plug-in parsers (by namespace)
    private Parser viewParser;

    // supporting services
    private NamespaceService namespaceService;
    private DictionaryService dictionaryService;
    private BehaviourFilter behaviourFilter;
    private NodeService nodeService;
    private SearchService searchService;
    private ContentService contentService;
    private RuleService ruleService;

    // binding markers    
    private static final String START_BINDING_MARKER = "${";
    private static final String END_BINDING_MARKER = "}"; 
    
    
    /**
     * @param viewParser  the default parser
     */
    public void setViewParser(Parser viewParser)
    {
        this.viewParser = viewParser;
    }
    
    /**
     * @param nodeService  the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param searchService the service to perform path searches
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

    /**
     * @param behaviourFilter  policy behaviour filter 
     */
    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    /**
     * TODO: Remove this in favour of appropriate rule disabling
     * 
     * @param ruleService  rule service
     */
    public void setRuleService(RuleService ruleService)
    {
        this.ruleService = ruleService;
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ImporterService#importView(java.io.InputStreamReader, org.alfresco.service.cmr.view.Location, java.util.Properties, org.alfresco.service.cmr.view.ImporterProgress)
     */
    public void importView(Reader viewReader, Location location, ImporterBinding binding, ImporterProgress progress)
    {
        NodeRef nodeRef = getNodeRef(location, binding);
        performImport(nodeRef, location.getChildAssocType(), viewReader, new DefaultStreamHandler(), binding, progress);       
    }

    /* (non-Javadoc)
     * @see org.alfresco.service.cmr.view.ImporterService#importView(org.alfresco.service.cmr.view.ImportPackageHandler, org.alfresco.service.cmr.view.Location, org.alfresco.service.cmr.view.ImporterBinding, org.alfresco.service.cmr.view.ImporterProgress)
     */
    public void importView(ImportPackageHandler importHandler, Location location, ImporterBinding binding, ImporterProgress progress) throws ImporterException
    {
        importHandler.startImport();
        Reader dataFileReader = importHandler.getDataStream(); 
        NodeRef nodeRef = getNodeRef(location, binding);
        performImport(nodeRef, location.getChildAssocType(), dataFileReader, importHandler, binding, progress);
        importHandler.endImport();
    }
    
    /**
     * Get Node Reference from Location
     *  
     * @param location the location to extract node reference from
     * @param binding import configuration
     * @return node reference
     */
    private NodeRef getNodeRef(Location location, ImporterBinding binding)
    {
        ParameterCheck.mandatory("Location", location);
    
        // Establish node to import within
        NodeRef nodeRef = location.getNodeRef();
        if (nodeRef == null)
        {
            // If a specific node has not been provided, default to the root
            nodeRef = nodeService.getRootNode(location.getStoreRef());
        }
        
        // Resolve to path within node, if one specified
        String path = location.getPath();
        if (path != null && path.length() >0)
        {
            // Create a valid path and search
            path = bindPlaceHolder(path, binding);
            path = createValidPath(path);
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
    
    /**
     * Perform the actual import
     * 
     * @param nodeRef node reference to import under
     * @param childAssocType the child association type to import under
     * @param inputStream the input stream to import from
     * @param streamHandler the content property import stream handler
     * @param binding import configuration
     * @param progress import progress
     */
    private void performImport(NodeRef nodeRef, QName childAssocType, Reader viewReader, ImportPackageHandler streamHandler, ImporterBinding binding, ImporterProgress progress)
    {
        ParameterCheck.mandatory("Node Reference", nodeRef);
        ParameterCheck.mandatory("View Reader", viewReader);
        ParameterCheck.mandatory("Stream Handler", streamHandler);
        
        try
        {
            Importer defaultImporter = new DefaultImporter(nodeRef, childAssocType, binding, streamHandler, progress);
            defaultImporter.start();
            viewParser.parse(viewReader, defaultImporter);
            defaultImporter.end();
        }
        finally
        {
            behaviourFilter.enableAllBehaviours();
        }
    }
    
    /**
     * Bind the specified value to the passed configuration values if it is a place holder
     * 
     * @param value  the value to bind
     * @param binding  the configuration properties to bind to
     * @return  the bound value
     */
    private String bindPlaceHolder(String value, ImporterBinding binding)
    {
        if (binding != null)
        {
            int iStartBinding = value.indexOf(START_BINDING_MARKER);
            while (iStartBinding != -1)
            {
                int iEndBinding = value.indexOf(END_BINDING_MARKER, iStartBinding + START_BINDING_MARKER.length());
                if (iEndBinding == -1)
                {
                    throw new ImporterException("Cannot find end marker " + END_BINDING_MARKER + " within value " + value);
                }
                
                String key = value.substring(iStartBinding + START_BINDING_MARKER.length(), iEndBinding);
                String keyValue = binding.getValue(key);
                value = StringUtils.replace(value, START_BINDING_MARKER + key + END_BINDING_MARKER, keyValue == null ? "" : keyValue);
                iStartBinding = value.indexOf(START_BINDING_MARKER);
            }
        }
        return value;
    }
    
    /**
     * Create a valid path
     * 
     * @param path
     * @return
     */
    private String createValidPath(String path)
    {
        StringBuffer validPath = new StringBuffer(path.length());
        String[] segments = StringUtils.delimitedListToStringArray(path, "/");
        for (int i = 0; i < segments.length; i++)
        {
            if (segments[i] != null && segments[i].length() > 0)
            {
                String[] qnameComponents = QName.splitPrefixedQName(segments[i]);
                QName segmentQName = QName.createQName(qnameComponents[0], QName.createValidLocalName(qnameComponents[1]), namespaceService); 
                validPath.append(segmentQName.toPrefixString());
            }
            if (i < (segments.length -1))
            {
                validPath.append("/");
            }
        }
        return validPath.toString();
    }
    
    /**
     * Default Importer strategy
     * 
     * @author David Caruana
     */
    private class DefaultImporter
        implements Importer
    {
        private NodeRef rootRef;
        private QName rootAssocType;
        private ImporterBinding binding;
        private ImporterProgress progress;
        private ImportPackageHandler streamHandler;
        private List<ImportedNodeRef> nodeRefs = new ArrayList<ImportedNodeRef>();

        // Flush threshold
        private int flushThreshold = 500;
        private int flushCount = 0;
        
        
        /**
         * Construct
         * 
         * @param rootRef
         * @param rootAssocType
         * @param binding
         * @param progress
         */
        private DefaultImporter(NodeRef rootRef, QName rootAssocType, ImporterBinding binding, ImportPackageHandler streamHandler, ImporterProgress progress)
        {
            this.rootRef = rootRef;
            this.rootAssocType = rootAssocType;
            this.binding = binding;
            this.progress = progress;
            this.streamHandler = streamHandler;
        }
        
        /* (non-Javadoc)
         * @see org.alfresco.repo.importer.Importer#getRootRef()
         */
        public NodeRef getRootRef()
        {
            return rootRef;
        }

        /* (non-Javadoc)
         * @see org.alfresco.repo.importer.Importer#getRootAssocType()
         */
        public QName getRootAssocType()
        {
            return rootAssocType;
        }
        
        /* (non-Javadoc)
         * @see org.alfresco.repo.importer.Importer#start()
         */
        public void start()
        {
        }
       
        /* (non-Javadoc)
         * @see org.alfresco.repo.importer.Importer#importNode(org.alfresco.repo.importer.ImportNode)
         */
        public NodeRef importNode(ImportNode context)
        {
            TypeDefinition nodeType = context.getTypeDefinition();
            NodeRef parentRef = context.getParentContext().getParentRef();
            QName assocType = getAssocType(context);
            QName childQName = null;

            // Determine child name
            String childName = context.getChildName();
            if (childName != null)
            {
                childName = bindPlaceHolder(childName, binding);
                String[] qnameComponents = QName.splitPrefixedQName(childName);
                childQName = QName.createQName(qnameComponents[0], QName.createValidLocalName(qnameComponents[1]), namespaceService); 
            }
            else
            {
                Map<QName, Serializable> typeProperties = context.getProperties(nodeType.getName());
                String name = (String)typeProperties.get(ContentModel.PROP_NAME);
                if (name == null || name.length() == 0)
                {
                    throw new ImporterException("Cannot import node of type " + nodeType.getName() + " - it does not have a name");
                }
                
                name = bindPlaceHolder(name, binding);
                String localName = QName.createValidLocalName(name);
                childQName = QName.createQName(assocType.getNamespaceURI(), localName);
            }
            
            // Build initial map of properties
            Map<QName, Serializable> initialProperties = bindProperties(context);

            // Create initial node (but, first disable behaviour for the node to be created)
            Set<QName> disabledBehaviours = getDisabledBehaviours(context);
            for (QName disabledBehaviour: disabledBehaviours)
            {
                boolean alreadyDisabled = behaviourFilter.disableBehaviour(disabledBehaviour);
                if (alreadyDisabled)
                {
                    disabledBehaviours.remove(disabledBehaviour);
                }
            }
            ChildAssociationRef assocRef = nodeService.createNode(parentRef, assocType, childQName, nodeType.getName(), initialProperties);
            for (QName disabledBehaviour : disabledBehaviours)
            {
                behaviourFilter.enableBehaviour(disabledBehaviour);
            }
            
            // Report creation
            NodeRef nodeRef = assocRef.getChildRef();
            reportNodeCreated(assocRef);
            reportPropertySet(nodeRef, initialProperties);

            // Disable behaviour for the node until the complete node (and its children have been imported)
            for (QName disabledBehaviour : disabledBehaviours)
            {
                behaviourFilter.disableBehaviour(nodeRef, disabledBehaviour);
            }
            // TODO: Replace this with appropriate rule/action import handling
            ruleService.disableRules(nodeRef);
            
            // Apply aspects
            for (QName aspect : context.getNodeAspects())
            {
                if (nodeService.hasAspect(nodeRef, aspect) == false)
                {
                    nodeService.addAspect(nodeRef, aspect, null);   // all properties previously added
                    reportAspectAdded(nodeRef, aspect);
                }
            }

            // import content, if applicable
            for (Map.Entry<QName,Serializable> property : context.getProperties().entrySet())
            {
                PropertyDefinition propertyDef = dictionaryService.getProperty(property.getKey());
                if (propertyDef != null && propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT))
                {
                    importContent(nodeRef, property.getKey(), (String)property.getValue());
                }
            }
            
            // Do we need to flush?
            flushCount++;
            if (flushCount > flushThreshold)
            {
                AlfrescoTransactionSupport.flush();
                flushCount = 0;
            }
            
            return nodeRef;
        }
        
        /**
         * Import Node Content.
         * <p>
         * The content URL, if present, will be a local URL.  This import copies the content
         * from the local URL to a server-assigned location.
         *
         * @param nodeRef containing node
         * @param propertyName the name of the content-type property
         * @param contentData the identifier of the content to import
         */
        private void importContent(NodeRef nodeRef, QName propertyName, String importContentData)
        {
            // bind import content data description
            DataTypeDefinition dataTypeDef = dictionaryService.getDataType(DataTypeDefinition.CONTENT);
            importContentData = bindPlaceHolder(importContentData, binding);
            ContentData contentData = (ContentData)DefaultTypeConverter.INSTANCE.convert(dataTypeDef, importContentData);
            
            String contentUrl = contentData.getContentUrl();
            if (contentUrl != null && contentUrl.length() > 0)
            {
                // import the content from the url
                InputStream contentStream = streamHandler.importStream(contentUrl);
                ContentWriter writer = contentService.getWriter(nodeRef, propertyName, true);
                writer.setEncoding(contentData.getEncoding());
                writer.setMimetype(contentData.getMimetype());
                writer.putContent(contentStream);
                reportContentCreated(nodeRef, contentUrl);
            }
        }
        
        /* (non-Javadoc)
         * @see org.alfresco.repo.importer.Importer#childrenImported(org.alfresco.service.cmr.repository.NodeRef)
         */
        public void childrenImported(NodeRef nodeRef)
        {
            behaviourFilter.enableBehaviours(nodeRef);
            ruleService.enableRules(nodeRef);
        }
        
        /* (non-Javadoc)
         * @see org.alfresco.repo.importer.Importer#end()
         */
        public void end()
        {
            // Bind all node references to destination space
            for (ImportedNodeRef importedRef : nodeRefs)
            {
                // Resolve path to node reference
                NodeRef contextNode = (importedRef.value.startsWith("/")) ? nodeService.getRootNode(rootRef.getStoreRef()) : importedRef.context.getNodeRef();
                NodeRef nodeRef = null;
                try
                {
                    List<NodeRef> nodeRefs = searchService.selectNodes(contextNode, importedRef.value, null, namespaceService, false);
                    if (nodeRefs.size() > 0)
                    {
                        nodeRef = nodeRefs.get(0);
                    }
                }
                catch(XPathException e)
                {
                    // attempt to resolve as a node reference
                    try
                    {
                        NodeRef directRef = new NodeRef(importedRef.value);
                        if (nodeService.exists(directRef))
                        {
                            nodeRef = directRef;
                        }
                    }
                    catch(AlfrescoRuntimeException e1)
                    {
                        // Note: Invalid reference format
                    }
                }

                // check that reference could be bound
                if (nodeRef == null)
                {
                    // TODO: Probably need an alternative mechanism here e.g. report warning
                    throw new ImporterException("Failed to find item referenced as " + importedRef.value);
                }
                
                // Set node reference on source node
                Set<QName> disabledBehaviours = getDisabledBehaviours(importedRef.context);
                for (QName disabledBehaviour: disabledBehaviours)
                {
                    boolean alreadyDisabled = behaviourFilter.disableBehaviour(importedRef.context.getNodeRef(), disabledBehaviour);
                    if (alreadyDisabled)
                    {
                        disabledBehaviours.remove(disabledBehaviour);
                    }
                }
                nodeService.setProperty(importedRef.context.getNodeRef(), importedRef.property, nodeRef);
                behaviourFilter.enableBehaviours(importedRef.context.getNodeRef());
            }
        }
        
        /**
         * Get appropriate child association type for node to import under
         * 
         * @param context  node to import
         * @return  child association type name
         */
        private QName getAssocType(ImportNode context)
        {
            QName assocType = context.getParentContext().getAssocType();
            if (assocType != null)
            {
                // return explicitly set association type
                return assocType;
            }
            
            //
            // Derive association type
            //
            
            // build type and aspect list for node
            List<QName> nodeTypes = new ArrayList<QName>();
            nodeTypes.add(context.getTypeDefinition().getName());
            for (QName aspect : context.getNodeAspects())
            {
                nodeTypes.add(aspect);
            }
            
            // build target class types for parent
            Map<QName, QName> targetTypes = new HashMap<QName, QName>();
            QName parentType = nodeService.getType(context.getParentContext().getParentRef());
            ClassDefinition classDef = dictionaryService.getClass(parentType);
            Map<QName, ChildAssociationDefinition> childAssocDefs = classDef.getChildAssociations();
            for (ChildAssociationDefinition childAssocDef : childAssocDefs.values())
            {
                targetTypes.put(childAssocDef.getTargetClass().getName(), childAssocDef.getName());
            }
            Set<QName> parentAspects = nodeService.getAspects(context.getParentContext().getParentRef());
            for (QName parentAspect : parentAspects)
            {
                classDef = dictionaryService.getClass(parentAspect);
                childAssocDefs = classDef.getChildAssociations();
                for (ChildAssociationDefinition childAssocDef : childAssocDefs.values())
                {
                    targetTypes.put(childAssocDef.getTargetClass().getName(), childAssocDef.getName());
                }
            }
            
            // find target class that is closest to node type or aspects
            QName closestAssocType = null;
            int closestHit = 1;
            for (QName nodeType : nodeTypes)
            {
                for (QName targetType : targetTypes.keySet())
                {
                    QName testType = nodeType;
                    int howClose = 1;
                    while (testType != null)
                    {
                        howClose--;
                        if (targetType.equals(testType) && howClose < closestHit)
                        {
                            closestAssocType = targetTypes.get(targetType);
                            closestHit = howClose;
                            break;
                        }
                        ClassDefinition testTypeDef = dictionaryService.getClass(testType);
                        testType = (testTypeDef == null) ? null : testTypeDef.getParentName();
                    }
                }
            }
            
            return closestAssocType;
        }

        /**
         * For the given import node, return the behaviours to disable during import
         * 
         * @param context  import node
         * @return  the disabled behaviours
         */
        private Set<QName> getDisabledBehaviours(ImportNode context)
        {
            Set<QName> classNames = new HashSet<QName>();
            
            // disable the type
            TypeDefinition typeDef = context.getTypeDefinition();
            classNames.add(typeDef.getName());

            // disable the aspects imported on the node
            classNames.addAll(context.getNodeAspects());
            
            // note: do not disable default aspects that are not imported on the node.
            //       this means they'll be added on import
            
            return classNames;
        }
        
        /**
         * Bind properties
         * 
         * @param properties
         * @return
         */
        private Map<QName, Serializable> bindProperties(ImportNode context)
        {
            Map<QName, Serializable> properties = context.getProperties();
            Map<QName, DataTypeDefinition> datatypes = context.getPropertyDatatypes();
            Map<QName, Serializable> boundProperties = new HashMap<QName, Serializable>(properties.size());
            for (QName property : properties.keySet())
            {
                // get property definition
                PropertyDefinition propDef = dictionaryService.getProperty(property);
                if (propDef == null)
                {
                    throw new ImporterException("Property " + property + " does not exist in the repository dictionary");
                }
                
                // get property datatype
                DataTypeDefinition valueDataType = datatypes.get(property);
                if (valueDataType == null)
                {
                    valueDataType = propDef.getDataType();
                }

                // filter out content properties (they're imported later)
                if (valueDataType.getName().equals(DataTypeDefinition.CONTENT))
                {
                    continue;
                }
                
                // bind property value to configuration and convert to appropriate type
                Serializable value = properties.get(property);
                if (value instanceof Collection)
                {
                    List<Serializable> boundCollection = new ArrayList<Serializable>();
                    for (String collectionValue : (Collection<String>)value)
                    {
                        Serializable objValue = bindValue(context, property, valueDataType, collectionValue);
                        boundCollection.add(objValue);
                    }
                    value = (Serializable)boundCollection;
                }
                else
                {
                    value = bindValue(context, property, valueDataType, (String)value);
                }
                boundProperties.put(property, value);
            }
            
            return boundProperties;
        }

        /**
         * Bind property value
         * 
         * @param valueType  value type
         * @param value  string form of value
         * @return  the bound value
         */
        private Serializable bindValue(ImportNode context, QName property, DataTypeDefinition valueType, String value)
        {
            Serializable objValue = null;
            String strValue = bindPlaceHolder(value, binding);
            if (valueType.getName().equals(DataTypeDefinition.NODE_REF) || valueType.getName().equals(DataTypeDefinition.CATEGORY))
            {
                // record node reference for end-of-import binding
                ImportedNodeRef importedRef = new ImportedNodeRef(context, property, strValue);
                nodeRefs.add(importedRef);
                objValue = new NodeRef(rootRef.getStoreRef(), "unresolved reference");
            }
            else
            {
                objValue = (Serializable)DefaultTypeConverter.INSTANCE.convert(valueType, strValue);
            }
            return objValue;
        }
        
        /**
         * Helper to report node created progress
         * 
         * @param progress
         * @param childAssocRef
         */
        private void reportNodeCreated(ChildAssociationRef childAssocRef)
        {
            if (progress != null)
            {
                progress.nodeCreated(childAssocRef.getChildRef(), childAssocRef.getParentRef(), childAssocRef.getTypeQName(), childAssocRef.getQName());
            }
        }

        /**
         * Helper to report content created progress
         * 
         * @param progress
         * @param nodeRef
         * @param sourceUrl
         */
        private void reportContentCreated(NodeRef nodeRef, String sourceUrl)
        {
            if (progress != null)
            {
                progress.contentCreated(nodeRef, sourceUrl);
            }
        }
        
        /**
         * Helper to report aspect added progress
         *  
         * @param progress
         * @param nodeRef
         * @param aspect
         */
        private void reportAspectAdded(NodeRef nodeRef, QName aspect)
        {
            if (progress != null)
            {
                progress.aspectAdded(nodeRef, aspect);
            }        
        }

        /**
         * Helper to report property set progress
         * 
         * @param progress
         * @param nodeRef
         * @param properties
         */
        private void reportPropertySet(NodeRef nodeRef, Map<QName, Serializable> properties)
        {
            if (progress != null)
            {
                for (QName property : properties.keySet())
                {
                    progress.propertySet(nodeRef, property, properties.get(property));
                }
            }
        }
    }

    /**
     * Imported Node Reference
     * 
     * @author David Caruana
     */
    private static class ImportedNodeRef
    {
        /**
         * Construct
         * 
         * @param context
         * @param property
         * @param value
         */
        private ImportedNodeRef(ImportNode context, QName property, String value)
        {
            this.context = context;
            this.property = property;
            this.value = value;
        }
        
        private ImportNode context;
        private QName property;
        private String value;
    }

    /**
     * Default Import Stream Handler
     * 
     * @author David Caruana
     */
    private static class DefaultStreamHandler
        implements ImportPackageHandler
    {
        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.view.ImportPackageHandler#startImport()
         */
        public void startImport()
        {
        }

        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.view.ImportStreamHandler#importStream(java.lang.String)
         */
        public InputStream importStream(String content)
        {
            ResourceLoader loader = new DefaultResourceLoader();
            Resource resource = loader.getResource(content);
            if (resource.exists() == false)
            {
                throw new ImporterException("Content URL " + content + " does not exist.");
            }
            
            try
            {
                return resource.getInputStream();
            }
            catch(IOException e)
            {
                throw new ImporterException("Failed to retrieve input stream for content URL " + content);
            }
        }

        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.view.ImportPackageHandler#getDataStream()
         */
        public Reader getDataStream()
        {
            return null;
        }

        /* (non-Javadoc)
         * @see org.alfresco.service.cmr.view.ImportPackageHandler#endImport()
         */
        public void endImport()
        {
        }
    }

}
