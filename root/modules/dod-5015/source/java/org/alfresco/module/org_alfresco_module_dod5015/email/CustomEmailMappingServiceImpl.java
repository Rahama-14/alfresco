package org.alfresco.module.org_alfresco_module_dod5015.email;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.metadata.RFC822MetadataExtracter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class CustomEmailMappingServiceImpl implements CustomEmailMappingService
{
    
    private RFC822MetadataExtracter extracter;
    private NodeService nodeService;
    private NamespacePrefixResolver nspr;
    private PolicyComponent policyComponent;
    private ContentService contentService;
    private TransactionService transactionService;
    
    private Set<CustomMapping> customMappings = new HashSet<CustomMapping>();
    
    private static Log logger = LogFactory.getLog(CustomEmailMappingServiceImpl.class);
    
    
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * Get the name space prefix resolver
     * @return the name space prefix resolver
     */
    public NamespacePrefixResolver getNamespacePrefixResolver()
    {
        return nspr;
    }

    /**
     * Set the name space prefix resolver
     * @param nspr
     */
    public void setNamespacePrefixResolver(NamespacePrefixResolver nspr)
    {
        this.nspr = nspr;
    }
    
    /**
     * 
     */
    public void init()
    {
//        RetryingTransactionHelper helper = transactionService.getRetryingTransactionHelper();
        
//        RetryingTransactionCallback<Integer> cb = new RetryingTransactionCallback<Integer>()
//        {
//           public Integer execute() throws Throwable
//           {
               // TODO Need to retrieve custom properties  
               NodeRef configNode = getConfigNode();
               if(configNode != null)
               {
   
                   /**
                    * Get any custom mappings.
                    */
                   customMappings = readConfig(configNode);
                   
                   // Get the read only existing configuration
                   Map<String, Set<QName>> currentMapping = extracter.getCurrentMapping();
                   
                   Map<String, Set<QName>> newMapping = new HashMap<String, Set<QName>>(17);
                   newMapping.putAll(currentMapping);

// TODO MER - Commented out until RMC namespace is available here.                   
//                   for(CustomMapping mapping : customMappings)
//                   {
//                       QName newQName = QName.createQName(mapping.getTo(), nspr);
//                       Set<QName> values = newMapping.get(mapping.getFrom());
//                       if(values == null)
//                       {
//                           values = new HashSet<QName>();
//                           newMapping.put(mapping.getFrom(), values);
//                       }
//                       values.add(newQName);
//                   }
                       
                   // Now update the metadata extracter
                   extracter.setMapping(newMapping);       
               }
//                return 0;    
//           }
//        };
//        helper.doInTransaction(cb);
         
        // Register interest in the onContentUpdate policy
        policyComponent.bindClassBehaviour(
                ContentServicePolicies.ON_CONTENT_UPDATE,
                RecordsManagementModel.TYPE_EMAIL_CONFIG,
                new JavaBehaviour(this, "onContentUpdate"));
        
//        // Register interest in the beforeDeleteNode policy
//        policyComponent.bindClassBehaviour(
//                QName.createQName(NamespaceService.ALFRESCO_URI, "beforeDeleteNode"),
//                RecordsManagementModel.TYPE_EMAIL_CONFIG,
//                new JavaBehaviour(this, "beforeDeleteNode"));
//        
//        // Register interest in the onCreateNode policy
//        policyComponent.bindClassBehaviour(
//                QName.createQName(NamespaceService.ALFRESCO_URI, "onCreateNode"),
//                RecordsManagementModel.TYPE_EMAIL_CONFIG,
//                new JavaBehaviour(this, "onCreateNode"));
        
        
    }
    
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
        NodeRef configNode = getConfigNode();
        if(configNode != null)
        {
            customMappings = readConfig(configNode);
        }
    }
    
    public void beforeDeleteNode(NodeRef nodeRef)
    {
    }
    
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {

    }
    
    public Set<CustomMapping> getCustomMappings()
    {  
        // add all the lists data to a Map
        Set<CustomMapping> emailMap = new HashSet<CustomMapping>();
        
        Map<String, Set<QName>> currentMapping = extracter.getCurrentMapping();
        
        for(String key : currentMapping.keySet())
        {
            Set<QName> set = currentMapping.get(key);
            
            for(QName qname : set)
            {
                CustomMapping value = new CustomMapping();
                value.setFrom(key);
                QName resolvedQname = qname.getPrefixedQName(nspr);
                value.setTo(resolvedQname.toPrefixString());  
                emailMap.add(value);
            }
        }
        
        return emailMap;
    }
    

    public void addCustomMapping(String from, String to)
    {
        // Get the read only existing configuration
        Map<String, Set<QName>> currentMapping = extracter.getCurrentMapping();
        
        Map<String, Set<QName>> newMapping = new HashMap<String, Set<QName>>(17);
        newMapping.putAll(currentMapping);
        
        QName newQName = QName.createQName(to, nspr);
        
        Set<QName> values = newMapping.get(from);
        if(values == null)
        {
            values = new HashSet<QName>();
            newMapping.put(from, values);
        }
        values.add(newQName);
        
        CustomMapping xxx = new CustomMapping();
        xxx.setFrom(from);
        xxx.setTo(to);
        customMappings.add(xxx);
        
        updateOrCreateEmailConfig(customMappings);
        
        // Crash in the new config.
        extracter.setMapping(newMapping);
    }

    public void deleteCustomMapping(String from, String to)
    {
        // Get the read only existing configuration
        Map<String, Set<QName>> currentMapping = extracter.getCurrentMapping();
        
        Map<String, Set<QName>> newMapping = new HashMap<String, Set<QName>>(17);
        newMapping.putAll(currentMapping);
        
        QName oldQName = QName.createQName(to, nspr);
        
        Set<QName> values = newMapping.get(from);
        if(values != null)
        {
            values.remove(oldQName);
        }
        
        
        for(CustomMapping mapping : customMappings)
        {
            //TODO need to worry about qnames comparison here
            if(mapping.getFrom().equalsIgnoreCase(from) && mapping.getTo().equalsIgnoreCase(to))
            {
                customMappings.remove(mapping);
            }
        }
        
        updateOrCreateEmailConfig(customMappings);
        
        // Crash in the new config.
        extracter.setMapping(newMapping);
    }

    public void setExtracter(RFC822MetadataExtracter extractor)
    {
        this.extracter = extractor;
    }

    public RFC822MetadataExtracter getExtracter()
    {
        return extracter;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public NodeService getNodeService()
    {
        return nodeService;
    }
    
    // Default
    private StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
    private static final String CONFIG_NAME = "imapConfig.json";
    
    private static final QName DATATYPE_TEXT = DataTypeDefinition.TEXT;
    
    /**
     * 
     * @param nodeRef
     * @return
     */
    private Set<CustomMapping> readConfig(NodeRef nodeRef)
    {
        Set<CustomMapping> newMappings = new HashSet<CustomMapping>();
        
        ContentReader cr = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (cr != null)
        {
            String text = cr.getContentString();
            
            try
            {  
                JSONArray jsonArray = new JSONArray(new JSONTokener(text));
                for(int i = 0 ; i < jsonArray.length(); i++)
                {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    CustomMapping mapping = new CustomMapping();
                    mapping.setFrom(obj.getString("from"));
                    mapping.setTo(obj.getString("to"));
                    newMappings.add(mapping);
                }
                return newMappings;
            }
            catch (JSONException je)
            {
                logger.warn("unable to read custom email configuration", je);
                return newMappings;
            }
            
        }
        return newMappings;
    } 
    
    public NodeRef updateOrCreateEmailConfig(Set<CustomMapping> customMappings)
    {
        NodeRef caveatConfig = updateOrCreateEmailConfig();
        
        try
        {
            JSONArray mappings = new JSONArray();
            for(CustomMapping mapping : customMappings)
            {
                JSONObject obj = new JSONObject();
                obj.put("from", mapping.getFrom());
                obj.put("to", mapping.getTo());
                mappings.put(obj);
            }
         
            // Update the content
            ContentWriter writer = this.contentService.getWriter(caveatConfig, ContentModel.PROP_CONTENT, true);
            writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
            writer.setEncoding("UTF-8");
            writer.putContent(mappings.toString());
        }
        catch (JSONException je)
        {
            
        }
    
        
        return caveatConfig;
    }
    
    public NodeRef updateOrCreateEmailConfig(String txt)
    {
        NodeRef caveatConfig = updateOrCreateEmailConfig();
        
        // Update the content
        ContentWriter writer = this.contentService.getWriter(caveatConfig, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        writer.setEncoding("UTF-8");
        writer.putContent(txt);
        
        return caveatConfig;
    }
    
    private NodeRef updateOrCreateEmailConfig()
    {
        NodeRef caveatConfig = getConfigNode();
        if (caveatConfig == null)
        {
            logger.debug("custom email configuration does not exist - creating new");
            NodeRef rootNode = nodeService.getRootNode(storeRef);
            //nodeService.addAspect(rootNode, VersionModel.ASPECT_VERSION_STORE_ROOT, null);
            
            // Create caveat config
            caveatConfig = nodeService.createNode(rootNode,
                                                  RecordsManagementModel.ASSOC_EMAIL_CONFIG,
                                                  QName.createQName(RecordsManagementModel.RM_URI, CONFIG_NAME),
                                                  RecordsManagementModel.TYPE_EMAIL_CONFIG).getChildRef();
            
            nodeService.setProperty(caveatConfig, ContentModel.PROP_NAME, CONFIG_NAME);
        }
        
        return caveatConfig;
    }
    
    public NodeRef getConfigNode()
    {
        NodeRef rootNode = nodeService.getRootNode(storeRef);
        return nodeService.getChildByName(rootNode, RecordsManagementModel.ASSOC_EMAIL_CONFIG, CONFIG_NAME);
    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public ContentService getContentService()
    {
        return contentService;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    public TransactionService getTransactionService()
    {
        return transactionService;
    }
    
    
}
