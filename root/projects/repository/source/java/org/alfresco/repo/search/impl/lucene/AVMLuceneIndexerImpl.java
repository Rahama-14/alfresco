/**
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
package org.alfresco.repo.search.impl.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.model.WCMModel;
import org.alfresco.repo.avm.AVMDAOs;
import org.alfresco.repo.avm.AVMNode;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.avm.util.SimplePath;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.search.IndexMode;
import org.alfresco.repo.search.impl.lucene.fts.FTSIndexerAware;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.avm.AVMException;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.GUID;
import org.alfresco.util.ISO9075;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Searcher;

/**
 * Update the index after a snap shot to an AVM store. (Revert is dealt with as a new snap shot is created)
 * 
 * @author andyh
 */
public class AVMLuceneIndexerImpl extends AbstractLuceneIndexerImpl<String> implements AVMLuceneIndexer
{
    private enum IndexChannel
    {
        MAIN, DELTA;
    }

    private static String SNAP_SHOT_ID = "SnapShot";

    static Logger s_logger = Logger.getLogger(AVMLuceneIndexerImpl.class);

    private AVMService avmService;

    private AVMSyncService avmSyncService;

    @SuppressWarnings("unused")
    private ContentStore contentStore;

    private ContentService contentService;

    private FTSIndexerAware callBack;

    private FullTextSearchIndexer fullTextSearchIndexer;

    private int remainingCount;

    private int startVersion = -1;

    private int endVersion = -1;

    /**
     * Set the AVM Service
     * 
     * @param avmService
     */
    public void setAvmService(AVMService avmService)
    {
        this.avmService = avmService;
    }

    /**
     * Set the AVM sync service
     * 
     * @param avmSyncService
     */
    public void setAvmSyncService(AVMSyncService avmSyncService)
    {
        this.avmSyncService = avmSyncService;
    }

    /**
     * Set the content service
     * 
     * @param contentStore
     */
    public void setContentStore(ContentStore contentStore)
    {
        this.contentStore = contentStore;
    }

    /**
     * @param contentService
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * Generate an indexer
     * 
     * @param storeRef
     * @param deltaId
     * @param config
     * @return - the indexer instance
     * @throws LuceneIndexException
     */
    public static AVMLuceneIndexerImpl getUpdateIndexer(StoreRef storeRef, String deltaId, LuceneConfig config) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Creating indexer");
        }
        AVMLuceneIndexerImpl indexer = new AVMLuceneIndexerImpl();
        indexer.setLuceneConfig(config);
        indexer.initialise(storeRef, deltaId);
        return indexer;
    }

    /**
     * Index a specified change to a store
     * 
     * @param store
     * @param srcVersion
     * @param dstVersion
     */
    public void index(String store, int srcVersion, int dstVersion, IndexMode mode)
    {
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            switch (mode)
            {
            case ASYNCHRONOUS:
                asynchronousIndex(store, srcVersion, dstVersion);
                break;
            case SYNCHRONOUS:
                synchronousIndex(store, srcVersion, dstVersion);
                break;
            case UNINDEXED:
                // nothing to do
                break;
            }
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("snapshot index failed", e);
        }
    }

    private void asynchronousIndex(String store, int srcVersion, int dstVersion)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Async index for " + store + " from " + srcVersion + " to " + dstVersion);
        }
        index("\u0000BG:STORE:" + store + ":" + srcVersion + ":" + dstVersion + ":" + GUID.generate());
        fullTextSearchIndexer.requiresIndex(AVMNodeConverter.ToStoreRef(store));
    }

    private void synchronousIndex(String store, int srcVersion, int dstVersion)
    {
        if (startVersion == -1)
        {
            startVersion = srcVersion;
        }

        endVersion = dstVersion;

        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Sync index for " + store + " from " + srcVersion + " to " + dstVersion);
        }
        String path = store + ":/";
        List<AVMDifference> changeList = avmSyncService.compare(srcVersion, path, dstVersion, path, null);
        for (AVMDifference difference : changeList)
        {
            switch (difference.getDifferenceCode())
            {
            case AVMDifference.CONFLICT:
            case AVMDifference.NEWER:
            case AVMDifference.OLDER:
                AVMNodeDescriptor srcDesc = avmService.lookup(difference.getSourceVersion(), difference.getSourcePath(), true);
                AVMNodeDescriptor dstDesc = avmService.lookup(difference.getDestinationVersion(), difference.getDestinationPath(), true);
                // New
                if (srcDesc == null)
                {
                    index(difference.getDestinationPath());
                    if (dstDesc.isDirectory())
                    {
                        indexDirectory(dstDesc);
                    }
                    reindexAllAncestors(difference.getDestinationPath());
                }
                // New Delete
                else if (!srcDesc.isDeleted() && ((dstDesc == null) || dstDesc.isDeleted()))
                {
                    delete(difference.getSourcePath());
                    delete(difference.getDestinationPath());
                    reindexAllAncestors(difference.getDestinationPath());
                }
                // Existing delete
                else if (srcDesc.isDeleted() && dstDesc.isDeleted())
                {
                    // Nothing to do for this case
                }
                // Anything else then we reindex
                else
                {
                    if (!difference.getSourcePath().equals(difference.getDestinationPath()))
                    {
                        reindex(difference.getSourcePath(), srcDesc.isDirectory());
                        reindex(difference.getDestinationPath(), dstDesc.isDirectory());
                        reindexAllAncestors(difference.getSourcePath());
                        reindexAllAncestors(difference.getDestinationPath());
                    }
                    else
                    {
                        // If it is a directory, it is at the same path,
                        // so no cascade update is required for the bridge table data.
                        reindex(difference.getDestinationPath(), false);
                        reindexAllAncestors(difference.getDestinationPath());
                    }
                }
                break;
            case AVMDifference.DIRECTORY:
                // Never seen
                break;
            case AVMDifference.SAME:
                // No action
                break;

            }
        }
        // record the snap shotid
        reindex(SNAP_SHOT_ID + ":" + store + ":" + srcVersion + ":" + dstVersion, false);
    }

    /*
     * Nasty catch all fix up (as changes imply the parents may all have changed
     */
    private void reindexAllAncestors(String destinationPath)
    {
        String[] splitPath = splitPath(destinationPath);
        String store = splitPath[0];
        String pathInStore = splitPath[1];
        SimplePath simplePath = new SimplePath(pathInStore);

        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(store).append(":/");
        reindex(pathBuilder.toString(), false);
        boolean requiresSep = false;
        for (int i = 0; i < simplePath.size() - 1; i++)
        {
            if (requiresSep)
            {
                pathBuilder.append("/");
            }
            else
            {
                requiresSep = true;
            }
            pathBuilder.append(simplePath.get(i));
            reindex(pathBuilder.toString(), false);
        }
    }

    private void indexDirectory(AVMNodeDescriptor dir)
    {
        Map<String, AVMNodeDescriptor> children = avmService.getDirectoryListing(dir);
        for (AVMNodeDescriptor child : children.values())
        {
            index(child.getPath());
            if (child.isDirectory())
            {
                indexDirectory(child);
            }
        }

    }

    @Override
    protected List<Document> createDocuments(String stringNodeRef, boolean isNew, boolean indexAllProperties, boolean includeDirectoryDocuments)
    {
        List<Document> docs = new ArrayList<Document>();
        if (stringNodeRef.startsWith("\u0000"))
        {
            Document idoc = new Document();
            idoc.add(new Field("ID", stringNodeRef, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            docs.add(idoc);
            return docs;
        }
        else if (stringNodeRef.startsWith(SNAP_SHOT_ID))
        {
            Document sdoc = new Document();
            sdoc.add(new Field("ID", stringNodeRef, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            docs.add(sdoc);
            return docs;
        }

        AVMNodeDescriptor desc = avmService.lookup(endVersion, stringNodeRef);
        if (desc == null)
        {
            return docs;
        }
        if (desc.isLayeredDirectory() || desc.isLayeredFile())
        {
            return docs;
        }

        // Naughty, Britt should come up with a fix that doesn't require this.
        AVMNode node = AVMDAOs.Instance().fAVMNodeDAO.getByID(desc.getId());

        if (desc != null)
        {
    
            NodeRef nodeRef = AVMNodeConverter.ToNodeRef(endVersion, stringNodeRef);
    
            Document xdoc = new Document();
            xdoc.add(new Field("ID", nodeRef.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("ID", stringNodeRef, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
            xdoc.add(new Field("TX", AlfrescoTransactionSupport.getTransactionId(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
    
            boolean isAtomic = true;
    
            Map<QName, Serializable> properties = getIndexableProperties(desc, nodeRef, endVersion, stringNodeRef);
            for (QName propertyName : properties.keySet())
            {
                Serializable value = properties.get(propertyName);
                if (indexAllProperties)
                {
                    indexProperty(nodeRef, propertyName, value, xdoc, false, properties);
                }
                else
                {
                    isAtomic &= indexProperty(nodeRef, propertyName, value, xdoc, true, properties);
                }
            }
    
            StringBuilder qNameBuffer = new StringBuilder(64);
            if (node.getIsRoot())
            {
    
            }
            // pseudo roots?
            else
            {
                String[] splitPath = splitPath(stringNodeRef);
                String store = splitPath[0];
                String pathInStore = splitPath[1];
                SimplePath simplePath = new SimplePath(pathInStore);

                StringBuilder xpathBuilder = new StringBuilder();
                for (int i = 0; i < simplePath.size(); i++)
                {
                    xpathBuilder.append("/{}").append(simplePath.get(i));
                }
                String xpath = xpathBuilder.toString();

                if (qNameBuffer.length() > 0)
                {
                    qNameBuffer.append(";/");
                }
                // Get the parent

                ArrayList<String> ancestors = new ArrayList<String>();

                StringBuilder pathBuilder = new StringBuilder();
                pathBuilder.append(store).append(":/");
                ancestors.add(pathBuilder.toString());
                boolean requiresSep = false;
                for (int i = 0; i < simplePath.size() - 1; i++)
                {
                    if (requiresSep)
                    {
                        pathBuilder.append("/");
                    }
                    else
                    {
                        requiresSep = true;
                    }
                    pathBuilder.append(simplePath.get(i));
                    ancestors.add(pathBuilder.toString());
                }

                qNameBuffer.append(ISO9075.getXPathName(QName.createQName("", simplePath.get(simplePath.size() - 1))));
                xdoc.add(new Field("PARENT", ancestors.get(ancestors.size() - 1), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                // TODO: Categories and LINKASPECT

                if (includeDirectoryDocuments)
                {
                    if (desc.isDirectory())
                    {
                        // TODO: Exclude category paths

                        Document directoryEntry = new Document();
                        directoryEntry.add(new Field("ID", nodeRef.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));

                        directoryEntry.add(new Field("ID", stringNodeRef, Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));

                        directoryEntry.add(new Field("PATH", xpath, Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));

                        // Find all parent nodes.

                        for (String toAdd : ancestors)
                        {
                            directoryEntry.add(new Field("ANCESTOR", toAdd, Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                        }
                        directoryEntry.add(new Field("ISCONTAINER", "T", Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));

                        docs.add(directoryEntry);

                    }
                }
            }

            if (node.getIsRoot())
            {
                // TODO: Does the root element have a QName?
                xdoc.add(new Field("ISCONTAINER", "T", Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                xdoc.add(new Field("PATH", "", Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
                xdoc.add(new Field("QNAME", "", Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
                xdoc.add(new Field("ISROOT", "T", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                xdoc.add(new Field("ISNODE", "T", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                docs.add(xdoc);

            }
            else
                // not a root node
            {
                xdoc.add(new Field("QNAME", qNameBuffer.toString(), Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));

                QName typeQName = getType(desc);

                xdoc.add(new Field("TYPE", ISO9075.getXPathName(typeQName), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));

                for (QName classRef : avmService.getAspects(desc))
                {
                    xdoc.add(new Field("ASPECT", ISO9075.getXPathName(classRef), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                }

                xdoc.add(new Field("ISROOT", "F", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                xdoc.add(new Field("ISNODE", "T", Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));

                docs.add(xdoc);
            }
        }
        else
        {
            boolean root = node.getIsRoot();
            boolean deleted = desc.isDeleted();
            System.out.println("Is Root " + root);
            System.out.println("Is deleted " + deleted);
        }
        return docs;
    }

    private String[] splitPath(String path)
    {
        String[] pathParts = path.split(":");
        if (pathParts.length != 2)
        {
            throw new AVMException("Invalid path: " + path);
        }
        return pathParts;
    }

    private QName getType(AVMNodeDescriptor desc)
    {
        if (desc.isPlainDirectory())
        {
            return WCMModel.TYPE_AVM_PLAIN_FOLDER;
        }
        else if (desc.isPlainFile())
        {
            return WCMModel.TYPE_AVM_PLAIN_CONTENT;
        }
        else if (desc.isLayeredDirectory())
        {
            return WCMModel.TYPE_AVM_LAYERED_FOLDER;
        }
        else
        {
            return WCMModel.TYPE_AVM_LAYERED_CONTENT;
        }
    }

    private Map<QName, Serializable> getIndexableProperties(AVMNodeDescriptor desc, NodeRef nodeRef, Integer version, String path)
    {
        Map<QName, PropertyValue> properties = avmService.getNodeProperties(desc);

        Map<QName, Serializable> result = new HashMap<QName, Serializable>();
        for (QName qName : properties.keySet())
        {
            PropertyValue value = properties.get(qName);
            PropertyDefinition def = getDictionaryService().getProperty(qName);
            result.put(qName, makeSerializableValue(def, value));
        }
        // Now spoof properties that are built in.
        result.put(ContentModel.PROP_CREATED, new Date(desc.getCreateDate()));
        result.put(ContentModel.PROP_CREATOR, desc.getCreator());
        result.put(ContentModel.PROP_MODIFIED, new Date(desc.getModDate()));
        result.put(ContentModel.PROP_MODIFIER, desc.getLastModifier());
        result.put(ContentModel.PROP_OWNER, desc.getOwner());
        result.put(ContentModel.PROP_NAME, desc.getName());
        result.put(ContentModel.PROP_NODE_UUID, "UNKNOWN");
        result.put(ContentModel.PROP_NODE_DBID, new Long(desc.getId()));
        result.put(ContentModel.PROP_STORE_PROTOCOL, "avm");
        result.put(ContentModel.PROP_STORE_IDENTIFIER, nodeRef.getStoreRef().getIdentifier());
        if (desc.isLayeredDirectory())
        {
            result.put(WCMModel.PROP_AVM_DIR_INDIRECTION, AVMNodeConverter.ToNodeRef(endVersion, desc.getIndirection()));
        }
        if (desc.isLayeredFile())
        {
            result.put(WCMModel.PROP_AVM_FILE_INDIRECTION, AVMNodeConverter.ToNodeRef(endVersion, desc.getIndirection()));
        }
        if (desc.isFile())
        {
            try
            {
                ContentData contentData = null;
                if (desc.isPlainFile())
                {
                    contentData = avmService.getContentDataForRead(desc);
                }
                else
                {
                    contentData = avmService.getContentDataForRead(endVersion, path);
                }
                result.put(ContentModel.PROP_CONTENT, contentData);
            }
            catch (AVMException e)
            {
                // TODO For now ignore.
            }
        }
        return result;

    }

    protected Serializable makeSerializableValue(PropertyDefinition propertyDef, PropertyValue propertyValue)
    {
        if (propertyValue == null)
        {
            return null;
        }
        // get property attributes
        QName propertyTypeQName = null;
        if (propertyDef == null)
        {
            // allow this for now
            propertyTypeQName = DataTypeDefinition.ANY;
        }
        else
        {
            propertyTypeQName = propertyDef.getDataType().getName();
        }
        try
        {
            Serializable value = propertyValue.getValue(propertyTypeQName);
            // done
            return value;
        }
        catch (TypeConversionException e)
        {
            throw new TypeConversionException("The property value is not compatible with the type defined for the property: \n"
                    + "   property: " + (propertyDef == null ? "unknown" : propertyDef) + "\n" + "   property value: " + propertyValue, e);
        }
    }

    protected boolean indexProperty(NodeRef banana, QName propertyName, Serializable value, Document doc, boolean indexAtomicPropertiesOnly, Map<QName, Serializable> properties)
    {
        String attributeName = "@" + QName.createQName(propertyName.getNamespaceURI(), ISO9075.encode(propertyName.getLocalName()));

        boolean store = true;
        boolean index = true;
        boolean tokenise = true;
        @SuppressWarnings("unused")
        boolean atomic = true;
        boolean isContent = false;
        boolean isMultiLingual = false;
        boolean isText = false;

        PropertyDefinition propertyDef = getDictionaryService().getProperty(propertyName);
        if (propertyDef != null)
        {
            index = propertyDef.isIndexed();
            store = propertyDef.isStoredInIndex();
            tokenise = propertyDef.isTokenisedInIndex();
            atomic = propertyDef.isIndexedAtomically();
            isContent = propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT);
            isMultiLingual = propertyDef.getDataType().getName().equals(DataTypeDefinition.MLTEXT);
            isText = propertyDef.getDataType().getName().equals(DataTypeDefinition.TEXT);
        }
        if (value == null)
        {
            // the value is null
            return true;
        }
        // else if (indexAtomicPropertiesOnly && !atomic)
        // {
        // we are only doing atomic properties and the property is definitely non-atomic
        // return false;
        // }

        if (!indexAtomicPropertiesOnly)
        {
            doc.removeFields(propertyName.toString());
        }
        // boolean wereAllAtomic = true;
        // convert value to String
        for (Serializable serializableValue : DefaultTypeConverter.INSTANCE.getCollection(Serializable.class, value))
        {
            String strValue = null;
            try
            {
                strValue = DefaultTypeConverter.INSTANCE.convert(String.class, serializableValue);
            }
            catch (TypeConversionException e)
            {
                doc.add(new Field(attributeName, NOT_INDEXED_NO_TYPE_CONVERSION, Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                continue;
            }
            if (strValue == null)
            {
                // nothing to index
                continue;
            }

            if (isContent)
            {
                ContentData contentData = DefaultTypeConverter.INSTANCE.convert(ContentData.class, serializableValue);
                if (!index || contentData.getMimetype() == null)
                {
                    // no mimetype or property not indexed
                    continue;
                }
                // store mimetype in index - even if content does not index it is useful
                // Added szie and locale - size needs to be tokenised correctly
                doc.add(new Field(attributeName + ".mimetype", contentData.getMimetype(), Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
                doc.add(new Field(attributeName + ".size", Long.toString(contentData.getSize()), Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));

                // TODO: Use the node locale in preferanced to the system locale
                Locale locale = contentData.getLocale();
                // No default locale in AVM
                if (locale == null)
                {
                    locale = I18NUtil.getLocale();
                }
                doc.add(new Field(attributeName + ".locale", locale.toString().toLowerCase(), Field.Store.NO, Field.Index.UN_TOKENIZED, Field.TermVector.NO));

                ContentReader reader = contentService.getReader(banana, propertyName);
                if (reader != null && reader.exists())
                {
                    boolean readerReady = true;
                    // transform if necessary (it is not a UTF-8 text document)
                    if (!EqualsHelper.nullSafeEquals(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN) || !EqualsHelper.nullSafeEquals(reader.getEncoding(), "UTF-8"))
                    {
                        // get the transformer
                        ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN);
                        // is this transformer good enough?
                        if (transformer == null)
                        {
                            // log it
                            if (s_logger.isDebugEnabled())
                            {
                                s_logger.debug("Not indexed: No transformation: \n" + "   source: " + reader + "\n" + "   target: " + MimetypeMap.MIMETYPE_TEXT_PLAIN);
                            }
                            // don't index from the reader
                            readerReady = false;
                            // not indexed: no transformation
                            // doc.add(new Field("TEXT", NOT_INDEXED_NO_TRANSFORMATION, Field.Store.NO,
                            // Field.Index.TOKENIZED, Field.TermVector.NO));
                            doc.add(new Field(attributeName, NOT_INDEXED_NO_TRANSFORMATION, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
                        }
                        // else if (indexAtomicPropertiesOnly
                        // && transformer.getTransformationTime() > maxAtomicTransformationTime)
                        // {
                        // only indexing atomic properties
                        // indexing will take too long, so push it to the background
                        // wereAllAtomic = false;
                        // }
                        else
                        {
                            // We have a transformer that is fast enough
                            ContentWriter writer = contentService.getTempWriter();
                            writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                            // this is what the analyzers expect on the stream
                            writer.setEncoding("UTF-8");
                            try
                            {

                                transformer.transform(reader, writer);
                                // point the reader to the new-written content
                                reader = writer.getReader();
                            }
                            catch (ContentIOException e)
                            {
                                // log it
                                if (s_logger.isDebugEnabled())
                                {
                                    s_logger.debug("Not indexed: Transformation failed", e);
                                }
                                // don't index from the reader
                                readerReady = false;
                                // not indexed: transformation
                                // failed
                                // doc.add(new Field("TEXT", NOT_INDEXED_TRANSFORMATION_FAILED, Field.Store.NO,
                                // Field.Index.TOKENIZED, Field.TermVector.NO));
                                doc.add(new Field(attributeName, NOT_INDEXED_TRANSFORMATION_FAILED, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
                            }
                        }
                    }
                    // add the text field using the stream from the
                    // reader, but only if the reader is valid
                    if (readerReady)
                    {
                        InputStreamReader isr = null;
                        InputStream ris = reader.getReader().getContentInputStream();
                        try
                        {
                            isr = new InputStreamReader(ris, "UTF-8");
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            isr = new InputStreamReader(ris);
                        }
                        StringBuilder builder = new StringBuilder();
                        builder.append("\u0000").append(locale.toString()).append("\u0000");
                        StringReader prefix = new StringReader(builder.toString());
                        Reader multiReader = new MultiReader(prefix, isr);
                        doc.add(new Field(attributeName, multiReader, Field.TermVector.NO));
                    }
                }
                else
                // URL not present (null reader) or no content at the URL (file missing)
                {
                    // log it
                    if (s_logger.isDebugEnabled())
                    {
                        s_logger.debug("Not indexed: Content Missing \n"
                                + "   node: " + banana + "\n" + "   reader: " + reader + "\n" + "   content exists: "
                                + (reader == null ? " --- " : Boolean.toString(reader.exists())));
                    }
                    // not indexed: content missing
                    doc.add(new Field("TEXT", NOT_INDEXED_CONTENT_MISSING, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
                    doc.add(new Field(attributeName, NOT_INDEXED_CONTENT_MISSING, Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
                }
            }
            else
            {
                Field.Store fieldStore = store ? Field.Store.YES : Field.Store.NO;
                Field.Index fieldIndex;

                if (index)
                {
                    if (tokenise)
                    {
                        fieldIndex = Field.Index.TOKENIZED;
                    }
                    else
                    {
                        fieldIndex = Field.Index.UN_TOKENIZED;
                    }
                }
                else
                {
                    fieldIndex = Field.Index.NO;
                }

                if ((fieldIndex != Field.Index.NO) || (fieldStore != Field.Store.NO))
                {
                    if (isMultiLingual)
                    {
                        MLText mlText = DefaultTypeConverter.INSTANCE.convert(MLText.class, serializableValue);
                        for (Locale locale : mlText.getLocales())
                        {
                            String localeString = mlText.getValue(locale);
                            StringBuilder builder = new StringBuilder();
                            builder.append("\u0000").append(locale.toString()).append("\u0000").append(localeString);
                            doc.add(new Field(attributeName, builder.toString(), fieldStore, fieldIndex, Field.TermVector.NO));
                        }
                    }
                    else if (isText)
                    {
                        // Temporary special case for uids and gids
                        if (propertyName.equals(ContentModel.PROP_USER_USERNAME)
                                || propertyName.equals(ContentModel.PROP_USERNAME) || propertyName.equals(ContentModel.PROP_AUTHORITY_NAME)
                                || propertyName.equals(ContentModel.PROP_MEMBERS))
                        {
                            doc.add(new Field(attributeName, strValue, fieldStore, fieldIndex, Field.TermVector.NO));
                        }

                        // TODO: Use the node locale in preferanced to the system locale
                        Locale locale = null;

                        Serializable localeProperty = properties.get(ContentModel.PROP_LOCALE);
                        if (localeProperty != null)
                        {
                            locale = DefaultTypeConverter.INSTANCE.convert(Locale.class, localeProperty);
                        }

                        if (locale == null)
                        {
                            locale = I18NUtil.getLocale();
                        }
                        if (tokenise)
                        {
                            StringBuilder builder = new StringBuilder();
                            builder.append("\u0000").append(locale.toString()).append("\u0000").append(strValue);
                            doc.add(new Field(attributeName, builder.toString(), fieldStore, fieldIndex, Field.TermVector.NO));
                        }
                        else
                        {
                            doc.add(new Field(attributeName, strValue, fieldStore, fieldIndex, Field.TermVector.NO));
                        }
                    }
                    else
                    {
                        doc.add(new Field(attributeName, strValue, fieldStore, fieldIndex, Field.TermVector.NO));
                    }
                }
            }
        }

        // return wereAllAtomic;
        return true;
    }

    @Override
    protected void doPrepare() throws IOException
    {
        saveDelta();
        flushPending();
    }

    @Override
    protected void doCommit() throws IOException
    {
        if (indexUpdateStatus == IndexUpdateStatus.ASYNCHRONOUS)
        {
            setInfo(docs, getDeletions(), false);
            // FTS does not trigger indexing request
        }
        else
        {
            setInfo(docs, getDeletions(), false);
            // TODO: only register if required
            fullTextSearchIndexer.requiresIndex(store);
        }
        if (callBack != null)
        {
            callBack.indexCompleted(store, remainingCount, null);
        }

        setInfo(docs, deletions, false);
    }

    @Override
    protected void doRollBack() throws IOException
    {
        if (callBack != null)
        {
            callBack.indexCompleted(store, 0, null);
        }
    }

    @Override
    protected void doSetRollbackOnly() throws IOException
    {

    }

    // The standard indexer API - although implemented it is not likely to be used at the moment
    // Batch indexing makes more sense for AVM at snaphot time

    public void createNode(ChildAssociationRef relationshipRef)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Create node " + relationshipRef.getChildRef());
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            NodeRef childRef = relationshipRef.getChildRef();
            Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(childRef);
            index(versionPath.getSecond());
            // TODO: Deal with a create on the root node deleting the index.
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Create node failed", e);
        }
    }

    public void updateNode(NodeRef nodeRef)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Update node " + nodeRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(nodeRef);
            reindex(versionPath.getSecond(), false);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Update node failed", e);
        }
    }

    public void deleteNode(ChildAssociationRef relationshipRef)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Delete node " + relationshipRef.getChildRef());
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            NodeRef childRef = relationshipRef.getChildRef();
            Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(childRef);
            reindex(versionPath.getSecond(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Delete node failed", e);
        }
    }

    public void createChildRelationship(ChildAssociationRef relationshipRef)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Create child " + relationshipRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            NodeRef childRef = relationshipRef.getChildRef();
            Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(childRef);
            reindex(versionPath.getSecond(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to create child relationship", e);
        }
    }

    public void updateChildRelationship(ChildAssociationRef relationshipBeforeRef, ChildAssociationRef relationshipAfterRef)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Update child " + relationshipBeforeRef + " to " + relationshipAfterRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            NodeRef childRef = relationshipBeforeRef.getChildRef();
            Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(childRef);
            reindex(versionPath.getSecond(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to update child relationship", e);
        }
    }

    public void deleteChildRelationship(ChildAssociationRef relationshipRef)
    {

        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Delete child " + relationshipRef);
        }
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            NodeRef childRef = relationshipRef.getChildRef();
            Pair<Integer, String> versionPath = AVMNodeConverter.ToAVMVersionPath(childRef);
            reindex(versionPath.getSecond(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to delete child relationship", e);
        }

    }

    public void deleteIndex(String store, IndexMode mode)
    {
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            switch (mode)
            {
            case ASYNCHRONOUS:
                asyncronousDeleteIndex(store);
                break;
            case SYNCHRONOUS:
                syncronousDeleteIndex(store);
                break;
            case UNINDEXED:
                // nothing to do
                break;
            }
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Delete index failed", e);
        }
    }

    /**
     * Sync delete of this index
     * 
     * @param store
     */
    public void syncronousDeleteIndex(String store)
    {

        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Sync delete for " + store);
        }
        deleteAll();
    }

    /**
     * Support to delete all entries frmo the idnex in the background
     * 
     * @param store
     */
    public void asyncronousDeleteIndex(String store)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Async delete for " + store);
        }
        index("\u0000BG:DELETE:" + store + ":" + GUID.generate());
        fullTextSearchIndexer.requiresIndex(AVMNodeConverter.ToStoreRef(store));
    }

    public void createIndex(String store, IndexMode mode)
    {
        checkAbleToDoWork(IndexUpdateStatus.SYNCRONOUS);
        try
        {
            switch (mode)
            {
            case ASYNCHRONOUS:
                asyncronousCreateIndex(store);
                break;
            case SYNCHRONOUS:
                syncronousCreateIndex(store);
                break;
            case UNINDEXED:
                // nothing to do
                break;
            }
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Create index failed", e);
        }
    }

    /**
     * Sync create index
     * 
     * @param store
     */
    public void syncronousCreateIndex(String store)
    {

        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Sync create for " + store);
        }
        @SuppressWarnings("unused")
        AVMNodeDescriptor rootDesc = avmService.getStoreRoot(-1, store);
        index(store + ":/");

    }

    /**
     * Asyn create index
     * 
     * @param store
     */
    public void asyncronousCreateIndex(String store)
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Async create for " + store);
        }
        index("\u0000BG:CREATE:" + store + ":" + GUID.generate());
        fullTextSearchIndexer.requiresIndex(AVMNodeConverter.ToStoreRef(store));
    }

    public void registerCallBack(FTSIndexerAware callBack)
    {
        this.callBack = callBack;
    }

    public int updateFullTextSearch(int size)
    {
        checkAbleToDoWork(IndexUpdateStatus.ASYNCHRONOUS);

        try
        {
            PrefixQuery query = new PrefixQuery(new Term("ID", "\u0000BG:"));

            String action = null;

            Searcher searcher = null;
            try
            {
                searcher = getSearcher(null);
                // commit on another thread - appears like there is no index ...try later
                if (searcher == null)
                {
                    remainingCount = size;
                    return 0;
                }
                Hits hits;
                try
                {
                    hits = searcher.search(query);
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException("Failed to execute query to find content which needs updating in the index", e);
                }

                if (hits.length() > 0)
                {
                    Document doc = hits.doc(0);
                    action = doc.getField("ID").stringValue();
                    String[] split = action.split(":");
                    if (split[1].equals("DELETE"))
                    {
                        deleteAll("\u0000BG:");
                    }
                    else if (split[1].equals("CREATE"))
                    {
                        syncronousCreateIndex(split[2]);
                    }
                    else if (split[1].equals("STORE"))
                    {
                        synchronousIndex(split[2], Integer.parseInt(split[3]), Integer.parseInt(split[4]));
                    }
                    deletions.add(action);
                    remainingCount = hits.length() - 1;
                    return 1;
                }
                else
                {
                    remainingCount = 0;
                    return 0;
                }

            }
            finally
            {
                if (searcher != null)
                {
                    try
                    {
                        searcher.close();
                    }
                    catch (IOException e)
                    {
                        throw new LuceneIndexException("Failed to close searcher", e);
                    }
                }
            }
        }
        catch (IOException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed FTS update", e);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed FTS update", e);
        }
    }

    public void setFullTextSearchIndexer(FullTextSearchIndexer fullTextSearchIndexer)
    {
        this.fullTextSearchIndexer = fullTextSearchIndexer;
    }

    public int getLastIndexedSnapshot(String store)
    {
        int last = getLastAsynchronousSnapshot(store);
        if (last > 0)
        {
            return last;
        }
        last = getLastSynchronousSnapshot(store);
        if (last > 0)
        {
            return last;
        }
        return hasIndexBeenCreated(store) ? 0 : -1;
    }

    public boolean isSnapshotIndexed(String store, int id)
    {
        if (id == 0)
        {
            return hasIndexBeenCreated(store);
        }
        else
        {
            return (id <= getLastAsynchronousSnapshot(store)) || (id <= getLastSynchronousSnapshot(store));
        }
    }

    public boolean isSnapshotSearchable(String store, int id)
    {
        if (id == 0)
        {
            return hasIndexBeenCreated(store);
        }
        else
        {
            return (id <= getLastSynchronousSnapshot(store));
        }
    }

    private int getLastSynchronousSnapshot(String store)
    {
        int answer = getLastSynchronousSnapshot(store, IndexChannel.DELTA);
        if (answer >= 0)
        {
            return answer;
        }
        answer = getLastSynchronousSnapshot(store, IndexChannel.MAIN);
        if (answer >= 0)
        {
            return answer;
        }
        return -1;
    }

    private int getLastSynchronousSnapshot(String store, IndexChannel channel)
    {
        String prefix = SNAP_SHOT_ID + ":" + store + ":";
        IndexReader reader = null;
        int end = -1;
        try
        {
            if (channel == IndexChannel.DELTA)
            {
                flushPending();
                reader = getDeltaReader();
            }
            else
            {
                reader = getReader();
            }

            TermEnum terms = null;
            try
            {
                terms = reader.terms();

                if (terms.skipTo(new Term("ID", prefix)))
                {

                    do
                    {
                        Term term = terms.term();
                        if (term.text().startsWith(prefix))
                        {
                            TermDocs docs = null;
                            try
                            {
                                docs = reader.termDocs(term);
                                if (docs.next())
                                {
                                    String[] split = term.text().split(":");
                                    end = Integer.parseInt(split[3]);
                                }
                            }
                            finally
                            {
                                if (docs != null)
                                {
                                    docs.close();
                                }
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                    while (terms.next());
                }

            }
            finally
            {
                if (terms != null)
                {
                    terms.close();
                }
            }
            return end;
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("IO error", e);
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    if (channel == IndexChannel.DELTA)
                    {
                        closeDeltaReader();
                    }
                    else
                    {
                        reader.close();
                    }
                }
            }
            catch (IOException e)
            {
                s_logger.warn("Failed to close main reader", e);
            }
        }
    }

    private int getLastAsynchronousSnapshot(String store)
    {
        int answer = getLastAsynchronousSnapshot(store, IndexChannel.DELTA);
        if (answer >= 0)
        {
            return answer;
        }
        answer = getLastAsynchronousSnapshot(store, IndexChannel.MAIN);
        if (answer >= 0)
        {
            return answer;
        }
        return -1;
    }

    private int getLastAsynchronousSnapshot(String store, IndexChannel channel)
    {
        String prefix = "\u0000BG:STORE:" + store + ":";
        IndexReader reader = null;
        int end = -1;
        try
        {
            if (channel == IndexChannel.DELTA)
            {
                flushPending();
                reader = getDeltaReader();
            }
            else
            {
                reader = getReader();
            }
            TermEnum terms = null;
            try
            {
                terms = reader.terms();

                if (terms.skipTo(new Term("ID", prefix)))
                {
                    do
                    {
                        Term term = terms.term();
                        if (term.text().startsWith(prefix))
                        {
                            TermDocs docs = null;
                            try
                            {
                                docs = reader.termDocs(term);
                                if (docs.next())
                                {
                                    String[] split = term.text().split(":");
                                    end = Integer.parseInt(split[4]);
                                }
                            }
                            finally
                            {
                                if (docs != null)
                                {
                                    docs.close();
                                }
                            }

                        }
                        else
                        {
                            break;
                        }
                    }
                    while (terms.next());
                }
            }
            finally
            {
                if (terms != null)
                {
                    terms.close();
                }
            }
            return end;
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("IO error", e);
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    if (channel == IndexChannel.DELTA)
                    {
                        closeDeltaReader();
                    }
                    else
                    {
                        reader.close();
                    }
                }
            }
            catch (IOException e)
            {
                s_logger.warn("Failed to close main reader", e);
            }
        }
    }

    public boolean hasIndexBeenCreated(String store)
    {
        IndexReader mainReader = null;
        try
        {
            mainReader = getReader();
            TermDocs termDocs = null;
            try
            {
                termDocs = mainReader.termDocs(new Term("ISROOT", "T"));
                return termDocs.next();
            }
            finally
            {
                if (termDocs != null)
                {
                    termDocs.close();
                }
            }
        }
        catch (IOException e)
        {
            throw new AlfrescoRuntimeException("IO error", e);
        }
        finally
        {
            try
            {
                if (mainReader != null)
                {
                    mainReader.close();
                }
            }
            catch (IOException e)
            {
                s_logger.warn("Failed to close main reader", e);
            }
        }

    }
}
