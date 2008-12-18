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
package org.alfresco.repo.content;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.content.ContentServicePolicies.OnContentReadPolicy;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.content.transform.ContentTransformerRegistry;
import org.alfresco.repo.content.transform.magick.ImageMagickContentTransformer;
import org.alfresco.repo.policy.ClassPolicyDelegate;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.cmr.usage.ContentQuotaException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * A content service that determines at runtime the store that the
 * content associated with a node should be routed to.
 * 
 * @author Derek Hulley
 */
public class RoutingContentService implements ContentService, ApplicationContextAware
{
    private static Log logger = LogFactory.getLog(RoutingContentService.class);
    
    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private AVMService avmService;
    private RetryingTransactionHelper transactionHelper;
    private ApplicationContext applicationContext;
    
    /** a registry of all available content transformers */
    private ContentTransformerRegistry transformerRegistry;
    /** TEMPORARY until we have a map to choose from at runtime */
    private ContentStore store;
    /** the store for all temporarily created content */
    private ContentStore tempStore;
    private ImageMagickContentTransformer imageMagickContentTransformer;
    
    /**
     * The policy component
     */
    private PolicyComponent policyComponent;
    
    /**
     * Policies delegate
     */
    ClassPolicyDelegate<ContentServicePolicies.OnContentUpdatePolicy> onContentUpdateDelegate;
    ClassPolicyDelegate<ContentServicePolicies.OnContentReadPolicy> onContentReadDelegate;
    
    /**
     * @deprecated  Replaced by {@link #setRetryingTransactionHelper(RetryingTransactionHelper)}
     */
    public void setTransactionService(TransactionService transactionService)
    {
        logger.warn("Property 'transactionService' has been replaced by 'retryingTransactionHelper'.");
    }
    
    public void setRetryingTransactionHelper(RetryingTransactionHelper helper)
    {
        this.transactionHelper = helper;
    }
    
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setTransformerRegistry(ContentTransformerRegistry transformerRegistry)
    {
        this.transformerRegistry = transformerRegistry;
    }
    
    public void setStore(ContentStore store)
    {
        this.store = store;
    }
    
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    public void setAvmService(AVMService service)
    {
        this.avmService = service;
    }
    
    public void setImageMagickContentTransformer(ImageMagickContentTransformer imageMagickContentTransformer) 
    {
        this.imageMagickContentTransformer = imageMagickContentTransformer;
    }
    
    
    /* (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    /**
     * Service initialise 
     */
    public void init()
    {
        // Set up a temporary store
        this.tempStore = new FileContentStore((ConfigurableApplicationContext) this.applicationContext,
                TempFileProvider.getTempDir().getAbsolutePath());

        // Bind on update properties behaviour
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"),
                this,
                new JavaBehaviour(this, "onUpdateProperties"));
        
        // Register on content update policy
        this.onContentUpdateDelegate = this.policyComponent.registerClassPolicy(OnContentUpdatePolicy.class);
        this.onContentReadDelegate = this.policyComponent.registerClassPolicy(OnContentReadPolicy.class);
    }
    
    /**
     * Update properties policy behaviour
     * 
     * @param nodeRef    the node reference
     * @param before    the before values of the properties
     * @param after        the after values of the properties
     */
    public void onUpdateProperties(
            NodeRef nodeRef,
            Map<QName, Serializable> before,
            Map<QName, Serializable> after)
    {
        boolean fire = false;
        boolean newContent = false;
        // check if any of the content properties have changed
        for (QName propertyQName : after.keySet())
        {
            // is this a content property?
            PropertyDefinition propertyDef = dictionaryService.getProperty(propertyQName);
            if (propertyDef == null)
            {
                // the property is not recognised
                continue;
            }
            if (!propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT))
            {
                // not a content type
                continue;
            }
            
            try
            {
                ContentData beforeValue = (ContentData) before.get(propertyQName);
                ContentData afterValue = (ContentData) after.get(propertyQName);
                if (afterValue != null && afterValue.getContentUrl() == null)
                {
                    // no URL - ignore
                }
                else if (!EqualsHelper.nullSafeEquals(beforeValue, afterValue))
                {
                    // So debug ...
                    if (logger.isDebugEnabled() == true)
                    {
                        String beforeString = "";
                        if (beforeValue != null)
                        {
                            beforeString = beforeValue.toString();
                        }
                        String afterString = "";
                        if (afterValue != null)
                        {
                            afterString = afterValue.toString();
                        }
                        logger.debug("onContentUpate: before = " + beforeString + "; after = " + afterString);
                    }
                    
                    // Figure out if the content is new or not
                    String beforeContentUrl = null;
                    if (beforeValue != null)
                    {
                        beforeContentUrl = beforeValue.getContentUrl();
                    }
                    String afterContentUrl = null;
                    if (afterValue != null)
                    {
                        afterContentUrl = afterValue.getContentUrl();
                    }
                    if (beforeContentUrl == null && afterContentUrl != null)
                    {
                        newContent = true;
                    }
                    
                    // the content changed
                    // at the moment, we are only interested in this one change
                    fire = true;
                    break;
                }
            }
            catch (ClassCastException e)
            {
                // properties don't conform to model
                continue;
            }
        }
        // fire?
        if (fire)
        {
            // Fire the content update policy
            Set<QName> types = new HashSet<QName>(this.nodeService.getAspects(nodeRef));
            types.add(this.nodeService.getType(nodeRef));
            OnContentUpdatePolicy policy = this.onContentUpdateDelegate.get(nodeRef, types);
            policy.onContentUpdate(nodeRef, newContent);
        }
    }

    /** {@inheritDoc} */
    public ContentReader getRawReader(String contentUrl)
    {
        ContentReader reader = null;
        try
        {
            reader = store.getReader(contentUrl);
        }
        catch (UnsupportedContentUrlException e)
        {
            // The URL is not supported, so we spoof it
            reader = new EmptyContentReader(contentUrl);
        }
        if (reader == null)
        {
            throw new AlfrescoRuntimeException("ContentStore implementations may not return null ContentReaders");
        }
        // set extra data on the reader
        reader.setMimetype(MimetypeMap.MIMETYPE_BINARY);
        reader.setEncoding("UTF-8");
        reader.setLocale(I18NUtil.getLocale());
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Direct request for reader: \n" +
                    "   Content URL: " + contentUrl + "\n" +
                    "   Reader:      " + reader);
        }
        return reader;
    }

    public ContentReader getReader(NodeRef nodeRef, QName propertyQName)
    {
        return getReader(nodeRef, propertyQName, true);
    }
        
    @SuppressWarnings("unchecked")
    private ContentReader getReader(NodeRef nodeRef, QName propertyQName, boolean fireContentReadPolicy)
    {   
        ContentData contentData = null;
        Serializable propValue = nodeService.getProperty(nodeRef, propertyQName);
        if (propValue instanceof Collection)
        {
            Collection colPropValue = (Collection)propValue;
            if (colPropValue.size() > 0)
            {
                propValue = (Serializable)colPropValue.iterator().next();
            }
        }

        if (propValue instanceof ContentData)
        {
            contentData = (ContentData)propValue;
        }

        if (contentData == null)
        {
            PropertyDefinition contentPropDef = dictionaryService.getProperty(propertyQName);
            
            // if no value or a value other content, and a property definition has been provided, ensure that it's CONTENT or ANY            
            if (contentPropDef != null && 
                (!(contentPropDef.getDataType().getName().equals(DataTypeDefinition.CONTENT) ||
                   contentPropDef.getDataType().getName().equals(DataTypeDefinition.ANY))))
            {
                throw new InvalidTypeException("The node property must be of type content: \n" +
                        "   node: " + nodeRef + "\n" +
                        "   property name: " + propertyQName + "\n" +
                        "   property type: " + ((contentPropDef == null) ? "unknown" : contentPropDef.getDataType()),
                        propertyQName);
            }
        }

        // check that the URL is available
        if (contentData == null || contentData.getContentUrl() == null)
        {
            // there is no URL - the interface specifies that this is not an error condition
            return null;                                
        }
        String contentUrl = contentData.getContentUrl();
        
        // The context of the read is entirely described by the URL
        ContentReader reader = store.getReader(contentUrl);
        if (reader == null)
        {
            throw new AlfrescoRuntimeException("ContentStore implementations may not return null ContentReaders");
        }
        
        // set extra data on the reader
        reader.setMimetype(contentData.getMimetype());
        reader.setEncoding(contentData.getEncoding());
        reader.setLocale(contentData.getLocale());
        
        // Fire the content read policy
        if (reader != null && fireContentReadPolicy == true)
        {
            // Fire the content update policy
            Set<QName> types = new HashSet<QName>(this.nodeService.getAspects(nodeRef));
            types.add(this.nodeService.getType(nodeRef));
            OnContentReadPolicy policy = this.onContentReadDelegate.get(nodeRef, types);
            policy.onContentRead(nodeRef);
        }
        
        // we don't listen for anything
        // result may be null - but interface contract says we may return null
        return reader;
    }

    public ContentWriter getWriter(NodeRef nodeRef, QName propertyQName, boolean update)
    {
        if (nodeRef == null)
        {
            ContentContext ctx = new ContentContext(null, null);
            // for this case, we just give back a valid URL into the content store
            ContentWriter writer = store.getWriter(ctx);
            // done
            return writer;
        }
        
        // check for an existing URL - the get of the reader will perform type checking
        ContentReader existingContentReader = getReader(nodeRef, propertyQName, false);
        
        // get the content using the (potentially) existing content - the new content
        // can be wherever the store decides.
        ContentContext ctx = new NodeContentContext(existingContentReader, null, nodeRef, propertyQName);
        ContentWriter writer = store.getWriter(ctx);

        // Special case for AVM repository.   
        Serializable contentValue = null;
        if (nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_AVM))
        {
            Pair<Integer, String> avmVersionPath = AVMNodeConverter.ToAVMVersionPath(nodeRef);
            contentValue = avmService.getContentDataForWrite(avmVersionPath.getSecond());
        }
        else
        {
            contentValue = nodeService.getProperty(nodeRef, propertyQName);
        }

        // set extra data on the reader if the property is pre-existing
        if (contentValue != null && contentValue instanceof ContentData)
        {
            ContentData contentData = (ContentData)contentValue;
            writer.setMimetype(contentData.getMimetype());
            writer.setEncoding(contentData.getEncoding());
            writer.setLocale(contentData.getLocale());
        }
        
        // attach a listener if required
        if (update)
        {
            // need a listener to update the node when the stream closes
            WriteStreamListener listener = new WriteStreamListener(nodeService, nodeRef, propertyQName, writer);
            writer.addListener(listener);
            writer.setRetryingTransactionHelper(transactionHelper);
        }
        
        // give back to the client
        return writer;
    }

    /**
     * @return Returns a writer to an anonymous location
     */
    public ContentWriter getTempWriter()
    {
        // there is no existing content and we don't specify the location of the new content
        return tempStore.getWriter(ContentContext.NULL_CONTEXT);
    }

    /**
     * @see org.alfresco.repo.content.transform.ContentTransformerRegistry
     * @see org.alfresco.repo.content.transform.ContentTransformer
     * @see org.alfresco.service.cmr.repository.ContentService#transform(org.alfresco.service.cmr.repository.ContentReader, org.alfresco.service.cmr.repository.ContentWriter)
     */
    public void transform(ContentReader reader, ContentWriter writer)
    {
        // Call transform with no options
        TransformationOptions options = null;
        this.transform(reader, writer, options);
    }
    
    /**
     * @see org.alfresco.repo.content.transform.ContentTransformerRegistry
     * @see org.alfresco.repo.content.transform.ContentTransformer
     * @deprecated
     */    
    @SuppressWarnings("deprecation")
    public void transform(ContentReader reader, ContentWriter writer, Map<String, Object> options)
            throws NoTransformerException, ContentIOException
    {
        transform(reader, writer, new TransformationOptions(options));
    }
    
    /**
     * @see org.alfresco.repo.content.transform.ContentTransformerRegistry
     * @see org.alfresco.repo.content.transform.ContentTransformer
     */
    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) 
        throws NoTransformerException, ContentIOException
    {
        // check that source and target mimetypes are available
        String sourceMimetype = reader.getMimetype();
        if (sourceMimetype == null)
        {
            throw new AlfrescoRuntimeException("The content reader mimetype must be set: " + reader);
        }
        String targetMimetype = writer.getMimetype();
        if (targetMimetype == null)
        {
            throw new AlfrescoRuntimeException("The content writer mimetype must be set: " + writer);
        }
        // look for a transformer
        ContentTransformer transformer = transformerRegistry.getTransformer(sourceMimetype, targetMimetype, options);
        if (transformer == null)
        {
            throw new NoTransformerException(sourceMimetype, targetMimetype);
        }
        // we have a transformer, so do it
        transformer.transform(reader, writer, options);
        // done
    }
    
    /**
     * @see org.alfresco.repo.content.transform.ContentTransformerRegistry
     * @see org.alfresco.repo.content.transform.ContentTransformer
     */
    public ContentTransformer getTransformer(String sourceMimetype, String targetMimetype)
    {
        return getTransformer(sourceMimetype, targetMimetype, new TransformationOptions());
    }
    
    /**
     * @see org.alfresco.service.cmr.repository.ContentService#getTransformer(java.lang.String, java.lang.String, org.alfresco.service.cmr.repository.TransformationOptions)
     */
    public ContentTransformer getTransformer(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        return transformerRegistry.getTransformer(sourceMimetype, targetMimetype, options);
    }
    
    /**
     * @see org.alfresco.service.cmr.repository.ContentService#getImageTransformer()
     */
    public ContentTransformer getImageTransformer()
    {
        return imageMagickContentTransformer;
    }

    /**
     * @see org.alfresco.repo.content.transform.ContentTransformerRegistry
     * @see org.alfresco.repo.content.transform.ContentTransformer
     */
    public boolean isTransformable(ContentReader reader, ContentWriter writer)
    {
       return isTransformable(reader, writer, new TransformationOptions());
    }
    
    /**
     * @see org.alfresco.service.cmr.repository.ContentService#isTransformable(org.alfresco.service.cmr.repository.ContentReader, org.alfresco.service.cmr.repository.ContentWriter, org.alfresco.service.cmr.repository.TransformationOptions)
     */
    public boolean isTransformable(ContentReader reader, ContentWriter writer, TransformationOptions options)
    {
     // check that source and target mimetypes are available
        String sourceMimetype = reader.getMimetype();
        if (sourceMimetype == null)
        {
            throw new AlfrescoRuntimeException("The content reader mimetype must be set: " + reader);
        }
        String targetMimetype = writer.getMimetype();
        if (targetMimetype == null)
        {
            throw new AlfrescoRuntimeException("The content writer mimetype must be set: " + writer);
        }
        
        // look for a transformer
        ContentTransformer transformer = transformerRegistry.getTransformer(sourceMimetype, targetMimetype, options);
        return (transformer != null);
    }

    /**
     * Ensures that, upon closure of the output stream, the node is updated with
     * the latest URL of the content to which it refers.
     * <p>
     * The listener close operation does not need a transaction as the 
     * <code>ContentWriter</code> takes care of that.
     * 
     * @author Derek Hulley
     */
    private static class WriteStreamListener implements ContentStreamListener
    {
        private NodeService nodeService;
        private NodeRef nodeRef;
        private QName propertyQName;
        private ContentWriter writer;
        
        public WriteStreamListener(
                NodeService nodeService,
                NodeRef nodeRef,
                QName propertyQName,
                ContentWriter writer)
        {
            this.nodeService = nodeService;
            this.nodeRef = nodeRef;
            this.propertyQName = propertyQName;
            this.writer = writer;
        }
        
        public void contentStreamClosed() throws ContentIOException
        {
            try
            {
                // set the full content property
                ContentData contentData = writer.getContentData();
                // Bypass NodeService for avm stores.
                if (nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_AVM))
                {
                    nodeService.setProperty(nodeRef, ContentModel.PROP_CONTENT, contentData);
                }
                else
                {
                    nodeService.setProperty(
                        nodeRef,
                        propertyQName,
                        contentData);
                }
                // done
                if (logger.isDebugEnabled())
                {
                    logger.debug("Stream listener updated node: \n" +
                            "   node: " + nodeRef + "\n" +
                            "   property: " + propertyQName + "\n" +
                            "   value: " + contentData);
                }
            }
            catch (ContentQuotaException qe)
            {
                throw qe;
            }
            catch (Throwable e)
            {
                throw new ContentIOException("Failed to set content property on stream closure: \n" +
                        "   node: " + nodeRef + "\n" +
                        "   property: " + propertyQName + "\n" +
                        "   writer: " + writer,
                        e);
            }
        }
    }
}