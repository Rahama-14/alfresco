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
package org.alfresco.repo.search.impl.lucene;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.IndexerException;
import org.alfresco.repo.search.impl.lucene.fts.FTSIndexerAware;
import org.alfresco.repo.search.impl.lucene.fts.FullTextSearchIndexer;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.ValueConverter;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;

/**
 * The implementation of the lucene based indexer. Supports basic transactional
 * behaviour if used on its own.
 * 
 * @author andyh
 * 
 */
public class LuceneIndexerImpl extends LuceneBase implements LuceneIndexer
{
    private static Logger s_logger = Logger.getLogger(LuceneIndexerImpl.class);

    private enum Action {
        INDEX, REINDEX, DELETE, CASCADEREINDEX
    };

    /**
     * The node service we use to get information about nodes
     */
    private NodeService nodeService;

    private ContentService contentService;

    /**
     * A list of all deletions we have made - at merge these deletions need to
     * be made against the main index.
     * 
     * TODO: Consider if this informantion needs to be persisted for recovery
     */

    private Set<NodeRef> deletions = new LinkedHashSet<NodeRef>();

    /**
     * A list of all nodes we have altered This list is used to drive the
     * background full text seach index which is to time consuming to do as part
     * of the transaction. The commit of the list of nodes to reindex is done as
     * part of the transaction.
     * 
     * TODO: Condsider persistence and recovery
     */

    private Set<NodeRef> fts = new LinkedHashSet<NodeRef>();

    /**
     * The status of this index - follows javax.transaction.Status
     */

    private int status = Status.STATUS_UNKNOWN;

    /**
     * Has this index been modified?
     */

    private boolean isModified = false;

    /**
     * Flag to indicte if we are doing an in transactional delta or a batch
     * update to the index. If true, we are just fixing up non atomically
     * indexed things from one or more other updates.
     */

    private Boolean isFTSUpdate = null;

    private List<Command> commandList = new ArrayList<Command>(10000);

    private FTSIndexerAware callBack;

    private int remainingCount = 0;

    private ArrayList<Helper> toFTSIndex = new ArrayList<Helper>();

    private QName contentPropertyQName = ContentModel.PROP_CONTENT_URL;

    LuceneIndexerImpl()
    {
        super();
    }

    public void setDictionaryService(DictionaryService dictionaryService)
    {
        super.setDictionaryService(dictionaryService);
    }

    /**
     * Setter for getting the node service via IOC Used in the Spring container
     * 
     * @param nodeService
     */

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /*
     * Indexer Implementation
     */

    /**
     * Utility method to check we are in the correct state to do work Also keeps
     * track of the dirty flag.
     * 
     */

    private void checkAbleToDoWork(boolean isFTS)
    {
        if (isFTSUpdate == null)
        {
            isFTSUpdate = Boolean.valueOf(isFTS);
        }
        else
        {
            if (isFTS != isFTSUpdate.booleanValue())
            {
                throw new IndexerException("Can not mix FTS and transactional updates");
            }
        }

        switch (status)
        {
        case Status.STATUS_UNKNOWN:
            status = Status.STATUS_ACTIVE;
            break;
        case Status.STATUS_ACTIVE:
            // OK
            break;
        default:
            // All other states are a problem
            throw new IndexerException(buildErrorString());
        }
        isModified = true;
    }

    /**
     * Utility method to report errors about invalid state.
     * 
     * @return
     */
    private String buildErrorString()
    {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append("The indexer is unable to accept more work: ");
        switch (status)
        {
        case Status.STATUS_COMMITTED:
            buffer.append("The indexer has been committed");
            break;
        case Status.STATUS_COMMITTING:
            buffer.append("The indexer is committing");
            break;
        case Status.STATUS_MARKED_ROLLBACK:
            buffer.append("The indexer is marked for rollback");
            break;
        case Status.STATUS_PREPARED:
            buffer.append("The indexer is prepared to commit");
            break;
        case Status.STATUS_PREPARING:
            buffer.append("The indexer is preparing to commit");
            break;
        case Status.STATUS_ROLLEDBACK:
            buffer.append("The indexer has been rolled back");
            break;
        case Status.STATUS_ROLLING_BACK:
            buffer.append("The indexer is rolling back");
            break;
        case Status.STATUS_UNKNOWN:
            buffer.append("The indexer is in an unknown state");
            break;
        default:
            break;
        }
        return buffer.toString();
    }

    /*
     * Indexer Implementation
     */

    public void createNode(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Create node " + relationshipRef.getChildRef());
        }
        checkAbleToDoWork(false);
        try
        {
            NodeRef childRef = relationshipRef.getChildRef();
            // If we have the root node we delete all other root nodes first
            if ((relationshipRef.getParentRef() == null) && childRef.equals(nodeService.getRootNode(childRef.getStoreRef())))
            {
                addRootNodesToDeletionList();
                s_logger.warn("Detected root node addition: deleting all nodes from the index");
            }
            index(childRef);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Create node failed", e);
        }
    }

    private void addRootNodesToDeletionList()
    {
        IndexReader mainReader = null;
        try
        {
            try
            {
                mainReader = getReader();
                TermDocs td = mainReader.termDocs(new Term("ISROOT", "T"));
                while (td.next())
                {
                    int doc = td.doc();
                    Document document = mainReader.document(doc);
                    String id = document.get("ID");
                    NodeRef ref = new NodeRef(store, id);
                    deleteImpl(ref, false, true, mainReader);
                }
            }
            catch (IOException e)
            {
                throw new LuceneIndexException("Failed to delete all primary nodes", e);
            }
        }
        finally
        {
            if (mainReader != null)
            {
                try
                {
                    mainReader.close();
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException("Filed to close main reader", e);
                }
            }
        }
    }

    public void updateNode(NodeRef nodeRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Update node " + nodeRef);
        }
        checkAbleToDoWork(false);
        try
        {
            reindex(nodeRef, false);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Update node failed", e);
        }
    }

    public void deleteNode(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Delete node " + relationshipRef.getChildRef());
        }
        checkAbleToDoWork(false);
        try
        {
            delete(relationshipRef.getChildRef());
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Delete node failed", e);
        }
    }

    public void createChildRelationship(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Create child " + relationshipRef);
        }
        checkAbleToDoWork(false);
        try
        {
            // TODO: Optimise
            // reindex(relationshipRef.getParentRef());
            reindex(relationshipRef.getChildRef(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to create child relationship", e);
        }
    }

    public void updateChildRelationship(ChildAssociationRef relationshipBeforeRef, ChildAssociationRef relationshipAfterRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Update child " + relationshipBeforeRef + " to " + relationshipAfterRef);
        }
        checkAbleToDoWork(false);
        try
        {
            // TODO: Optimise
            if (relationshipBeforeRef.getParentRef() != null)
            {
                // reindex(relationshipBeforeRef.getParentRef());
            }
            reindex(relationshipBeforeRef.getChildRef(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to update child relationship", e);
        }
    }

    public void deleteChildRelationship(ChildAssociationRef relationshipRef) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Delete child " + relationshipRef);
        }
        checkAbleToDoWork(false);
        try
        {
            // TODO: Optimise
            if (relationshipRef.getParentRef() != null)
            {
                // reindex(relationshipRef.getParentRef());
            }
            reindex(relationshipRef.getChildRef(), true);
        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed to delete child relationship", e);
        }
    }

    /**
     * Generate an indexer
     * 
     * @param storeRef
     * @param deltaId
     * @return
     */
    public static LuceneIndexerImpl getUpdateIndexer(StoreRef storeRef, String deltaId, LuceneConfig config) throws LuceneIndexException
    {
        if (s_logger.isDebugEnabled())
        {
            s_logger.debug("Creating indexer");
        }
        LuceneIndexerImpl indexer = new LuceneIndexerImpl();
        indexer.setLuceneConfig(config);
        indexer.initialise(storeRef, deltaId, false);
        return indexer;
    }

    /*
     * Transactional support Used by the resource mananger for indexers.
     */

    /**
     * Commit this index
     */

    public void commit() throws LuceneIndexException
    {
        switch (status)
        {
        case Status.STATUS_COMMITTING:
            throw new LuceneIndexException("Unable to commit: Transaction is committing");
        case Status.STATUS_COMMITTED:
            throw new LuceneIndexException("Unable to commit: Transaction is commited ");
        case Status.STATUS_ROLLING_BACK:
            throw new LuceneIndexException("Unable to commit: Transaction is rolling back");
        case Status.STATUS_ROLLEDBACK:
            throw new LuceneIndexException("Unable to commit: Transaction is aleady rolled back");
        case Status.STATUS_MARKED_ROLLBACK:
            throw new LuceneIndexException("Unable to commit: Transaction is marked for roll back");
        case Status.STATUS_PREPARING:
            throw new LuceneIndexException("Unable to commit: Transaction is preparing");
        case Status.STATUS_ACTIVE:
            // special case - commit from active
            prepare();
        // drop through to do the commit;
        default:
            if (status != Status.STATUS_PREPARED)
            {
                throw new LuceneIndexException("Index must be prepared to commit");
            }
            status = Status.STATUS_COMMITTING;
            try
            {
                if (isModified())
                {
                    if (isFTSUpdate.booleanValue())
                    {
                        doFTSIndexCommit();
                        // FTS does not trigger indexing request
                    }
                    else
                    {
                        // Build the deletion terms
                        Set<Term> terms = new LinkedHashSet<Term>();
                        for (NodeRef nodeRef : deletions)
                        {
                            terms.add(new Term("ID", nodeRef.getId()));
                        }
                        // Merge
                        mergeDeltaIntoMain(terms);
                        luceneFullTextSearchIndexer.requiresIndex(store);
                    }
                }
                status = Status.STATUS_COMMITTED;
                if (callBack != null)
                {
                    callBack.indexCompleted(store, remainingCount, null);
                }
            }
            catch (LuceneIndexException e)
            {
                // If anything goes wrong we try and do a roll back
                rollback();
                throw new LuceneIndexException("Commit failed", e);
            }
            finally
            {
                // Make sure we tidy up
                deleteDelta();
            }
            break;
        }
    }

    private void doFTSIndexCommit() throws LuceneIndexException
    {
        IndexReader mainReader = null;
        IndexReader deltaReader = null;
        IndexSearcher mainSearcher = null;
        IndexSearcher deltaSearcher = null;

        try
        {
            mainReader = getReader();
            deltaReader = getDeltaReader();
            mainSearcher = new IndexSearcher(mainReader);
            deltaSearcher = new IndexSearcher(deltaReader);

            for (Helper helper : toFTSIndex)
            {
                BooleanQuery query = new BooleanQuery();
                query.add(new TermQuery(new Term("ID", helper.document.getField("ID").stringValue())), true, false);
                query.add(new TermQuery(new Term("TX", helper.document.getField("TX").stringValue())), true, false);

                try
                {
                    Hits hits = mainSearcher.search(query);
                    if (hits.length() > 0)
                    {
                        // No change
                        for (int i = 0; i < hits.length(); i++)
                        {
                            mainReader.delete(hits.id(i));
                        }
                    }
                    else
                    {
                        hits = deltaSearcher.search(query);
                        for (int i = 0; i < hits.length(); i++)
                        {
                            deltaReader.delete(hits.id(i));
                        }
                    }
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException("Failed to delete an FTS update from the original index", e);
                }
            }

        }
        finally
        {
            if (deltaSearcher != null)
            {
                try
                {
                    deltaSearcher.close();
                }
                catch (IOException e)
                {
                    s_logger.warn("Failed to close delta searcher", e);
                }
            }
            if (mainSearcher != null)
            {
                try
                {
                    mainSearcher.close();
                }
                catch (IOException e)
                {
                    s_logger.warn("Failed to close main searcher", e);
                }
            }
            try
            {
                closeDeltaReader();
            }
            catch (LuceneIndexException e)
            {
                s_logger.warn("Failed to close delta reader", e);
            }
            if (mainReader != null)
            {
                try
                {
                    mainReader.close();
                }
                catch (IOException e)
                {
                    s_logger.warn("Failed to close main reader", e);
                }
            }
        }

        mergeDeltaIntoMain(new LinkedHashSet<Term>());

    }

    /**
     * Prepare to commit
     * 
     * At the moment this makes sure we have all the locks
     * 
     * TODO: This is not doing proper serialisation against the index as would a
     * data base transaction.
     * 
     * @return
     */
    public int prepare() throws LuceneIndexException
    {

        switch (status)
        {
        case Status.STATUS_COMMITTING:
            throw new IndexerException("Unable to prepare: Transaction is committing");
        case Status.STATUS_COMMITTED:
            throw new IndexerException("Unable to prepare: Transaction is commited ");
        case Status.STATUS_ROLLING_BACK:
            throw new IndexerException("Unable to prepare: Transaction is rolling back");
        case Status.STATUS_ROLLEDBACK:
            throw new IndexerException("Unable to prepare: Transaction is aleady rolled back");
        case Status.STATUS_MARKED_ROLLBACK:
            throw new IndexerException("Unable to prepare: Transaction is marked for roll back");
        case Status.STATUS_PREPARING:
            throw new IndexerException("Unable to prepare: Transaction is already preparing");
        case Status.STATUS_PREPARED:
            throw new IndexerException("Unable to prepare: Transaction is already prepared");
        default:
            status = Status.STATUS_PREPARING;
            try
            {
                if (isModified())
                {
                    saveDelta();
                    flushPending();
                    prepareToMergeIntoMain();
                }
                status = Status.STATUS_PREPARED;
                return isModified ? XAResource.XA_OK : XAResource.XA_RDONLY;
            }
            catch (LuceneIndexException e)
            {
                setRollbackOnly();
                throw new LuceneIndexException("Index failed to prepare", e);
            }
        }
    }

    /**
     * Has this index been modified?
     * 
     * @return
     */
    public boolean isModified()
    {
        return isModified;
    }

    /**
     * Return the javax.transaction.Status integer status code
     * 
     * @return
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Roll back the index changes (this just means they are never added)
     * 
     */

    public void rollback() throws LuceneIndexException
    {
        switch (status)
        {

        case Status.STATUS_COMMITTED:
            throw new IndexerException("Unable to roll back: Transaction is commited ");
        case Status.STATUS_ROLLING_BACK:
            throw new IndexerException("Unable to roll back: Transaction is rolling back");
        case Status.STATUS_ROLLEDBACK:
            throw new IndexerException("Unable to roll back: Transaction is aleady rolled back");
        case Status.STATUS_COMMITTING:
        // Can roll back during commit
        default:
            status = Status.STATUS_ROLLING_BACK;
            if (isModified())
            {
                deleteDelta();
            }
            status = Status.STATUS_ROLLEDBACK;
            if (callBack != null)
            {
                callBack.indexCompleted(store, 0, null);
            }
            break;
        }
    }

    /**
     * Mark this index for roll back only. This action can not be reversed. It
     * will reject all other work and only allow roll back.
     * 
     */

    public void setRollbackOnly()
    {
        switch (status)
        {
        case Status.STATUS_COMMITTING:
            throw new IndexerException("Unable to mark for rollback: Transaction is committing");
        case Status.STATUS_COMMITTED:
            throw new IndexerException("Unable to mark for rollback: Transaction is commited");
        default:
            status = Status.STATUS_MARKED_ROLLBACK;
            break;
        }
    }

    /*
     * Implementation
     */

    private void index(NodeRef nodeRef) throws LuceneIndexException
    {
        addCommand(new Command(nodeRef, Action.INDEX));
    }

    private void reindex(NodeRef nodeRef, boolean cascadeReindexDirectories) throws LuceneIndexException
    {
        addCommand(new Command(nodeRef, cascadeReindexDirectories ? Action.CASCADEREINDEX : Action.REINDEX));
    }

    private void delete(NodeRef nodeRef) throws LuceneIndexException
    {
        addCommand(new Command(nodeRef, Action.DELETE));
    }

    private void addCommand(Command command)
    {
        purgeCommandList(command);
        commandList.add(command);

        if (commandList.size() > getLuceneConfig().getIndexerBatchSize())
        {
            flushPending();
        }
    }

    private void purgeCommandList(Command command)
    {
        if (command.action == Action.DELETE)
        {
            removeFromCommandList(command, false);
        }
        else if (command.action == Action.REINDEX)
        {
            removeFromCommandList(command, true);
        }
        else if (command.action == Action.INDEX)
        {
            removeFromCommandList(command, true);
        }
        else if (command.action == Action.CASCADEREINDEX)
        {
            removeFromCommandList(command, true);
        }
    }

    private void removeFromCommandList(Command command, boolean matchExact)
    {
        for (ListIterator<Command> it = commandList.listIterator(commandList.size()); it.hasPrevious(); /**/)
        {
            Command current = it.previous();
            if (matchExact)
            {
                if ((current.action == command.action) && (current.nodeRef.equals(command.nodeRef)))
                {
                    it.remove();
                    return;
                }
            }
            else
            {
                if (current.nodeRef.equals(command.nodeRef))
                {
                    it.remove();
                }
            }
        }
    }

    public void flushPending() throws LuceneIndexException
    {
        IndexReader mainReader = null;
        try
        {
            mainReader = getReader();
            Set<NodeRef> forIndex = new LinkedHashSet<NodeRef>();

            for (Command command : commandList)
            {
                if (command.action == Action.INDEX)
                {
                    // Indexing just requires the node to be added to the list
                    forIndex.add(command.nodeRef);
                }
                else if (command.action == Action.REINDEX)
                {
                    // Reindex is a delete and then and index
                    Set<NodeRef> set = deleteImpl(command.nodeRef, true, false, mainReader);

                    // Deleting any pending index actions
                    // - make sure we only do at most one index
                    forIndex.removeAll(set);
                    // Add the nodes for index
                    forIndex.addAll(set);
                }
                else if (command.action == Action.CASCADEREINDEX)
                {
                    // Reindex is a delete and then and index
                    Set<NodeRef> set = deleteImpl(command.nodeRef, true, true, mainReader);

                    // Deleting any pending index actions
                    // - make sure we only do at most one index
                    forIndex.removeAll(set);
                    // Add the nodes for index
                    forIndex.addAll(set);
                }
                else if (command.action == Action.DELETE)
                {
                    // Delete the nodes
                    Set<NodeRef> set = deleteImpl(command.nodeRef, false, true, mainReader);
                    // Remove any pending indexes
                    forIndex.removeAll(set);
                }
            }
            commandList.clear();
            indexImpl(forIndex, false);
        }
        finally
        {
            if (mainReader != null)
            {
                try
                {
                    mainReader.close();
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException("Filed to close main reader", e);
                }
            }
            closeDeltaWriter();
        }
    }

    private Set<NodeRef> deleteImpl(NodeRef nodeRef, boolean forReindex, boolean cascade, IndexReader mainReader) throws LuceneIndexException
    {
        // startTimer();
        getDeltaReader();
        // outputTime("Delete "+nodeRef+" size = "+getDeltaWriter().docCount());
        Set<NodeRef> refs = new LinkedHashSet<NodeRef>();

        refs.addAll(deleteContainerAndBelow(nodeRef, getDeltaReader(), true, cascade));
        refs.addAll(deleteContainerAndBelow(nodeRef, mainReader, false, cascade));

        if (!forReindex)
        {
            Set<NodeRef> leafrefs = new LinkedHashSet<NodeRef>();

            leafrefs.addAll(deletePrimary(refs, getDeltaReader(), true));
            leafrefs.addAll(deletePrimary(refs, mainReader, false));

            leafrefs.addAll(deleteReference(refs, getDeltaReader(), true));
            leafrefs.addAll(deleteReference(refs, mainReader, false));

            refs.addAll(leafrefs);
        }

        deletions.addAll(refs);

        return refs;

    }

    private Set<NodeRef> deletePrimary(Collection<NodeRef> nodeRefs, IndexReader reader, boolean delete) throws LuceneIndexException
    {

        Set<NodeRef> refs = new LinkedHashSet<NodeRef>();

        for (NodeRef nodeRef : nodeRefs)
        {

            try
            {
                TermDocs td = reader.termDocs(new Term("PRIMARYPARENT", nodeRef.getId()));
                while (td.next())
                {
                    int doc = td.doc();
                    Document document = reader.document(doc);
                    String id = document.get("ID");
                    NodeRef ref = new NodeRef(store, id);
                    refs.add(ref);
                    if (delete)
                    {
                        reader.delete(doc);
                    }
                }
            }
            catch (IOException e)
            {
                throw new LuceneIndexException("Failed to delete node by primary parent for " + nodeRef.getId(), e);
            }
        }

        return refs;

    }

    private Set<NodeRef> deleteReference(Collection<NodeRef> nodeRefs, IndexReader reader, boolean delete) throws LuceneIndexException
    {

        Set<NodeRef> refs = new LinkedHashSet<NodeRef>();

        for (NodeRef nodeRef : nodeRefs)
        {

            try
            {
                TermDocs td = reader.termDocs(new Term("PARENT", nodeRef.getId()));
                while (td.next())
                {
                    int doc = td.doc();
                    Document document = reader.document(doc);
                    String id = document.get("ID");
                    NodeRef ref = new NodeRef(store, id);
                    refs.add(ref);
                    if (delete)
                    {
                        reader.delete(doc);
                    }
                }
            }
            catch (IOException e)
            {
                throw new LuceneIndexException("Failed to delete node by parent for " + nodeRef.getId(), e);
            }
        }

        return refs;

    }

    private Set<NodeRef> deleteContainerAndBelow(NodeRef nodeRef, IndexReader reader, boolean delete, boolean cascade) throws LuceneIndexException
    {
        Set<NodeRef> refs = new LinkedHashSet<NodeRef>();

        try
        {
            if (delete)
            {
                int count = reader.delete(new Term("ID", nodeRef.getId()));
            }
            refs.add(nodeRef);
            if (cascade)
            {
                TermDocs td = reader.termDocs(new Term("ANCESTOR", nodeRef.getId()));
                while (td.next())
                {
                    int doc = td.doc();
                    Document document = reader.document(doc);
                    String id = document.get("ID");
                    NodeRef ref = new NodeRef(store, id);
                    refs.add(ref);
                    if (delete)
                    {
                        reader.delete(doc);
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new LuceneIndexException("Failed to delete container and below for " + nodeRef.getId(), e);
        }
        return refs;
    }

    private void indexImpl(Set<NodeRef> nodeRefs, boolean isNew) throws LuceneIndexException
    {
        for (NodeRef ref : nodeRefs)
        {
            indexImpl(ref, isNew);
        }
    }

    private void indexImpl(NodeRef nodeRef, boolean isNew) throws LuceneIndexException
    {
        IndexWriter writer = getDeltaWriter();

        // avoid attempting to index nodes that don't exist

        try
        {
            List<Document> docs = createDocuments(nodeRef, isNew);
            for (Document doc : docs)
            {
                try
                {
                    writer.addDocument(doc /*
                                             * TODO: Select the language based
                                             * analyser
                                             */);
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException("Failed to add document to index", e);
                }
            }
        }
        catch (InvalidNodeRefException e)
        {
            // The node does not exist
            return;
        }

    }

    static class Counter
    {
        int countInParent = 0;

        int count = -1;

        int getCountInParent()
        {
            return countInParent;
        }

        int getRepeat()
        {
            return (count / countInParent) + 1;
        }

        void incrementParentCount()
        {
            countInParent++;
        }

        void increment()
        {
            count++;
        }

    }

    private class Pair<F, S>
    {
        private F first;

        private S second;

        public Pair(F first, S second)
        {
            this.first = first;
            this.second = second;
        }

        public F getFirst()
        {
            return first;
        }

        public S getSecond()
        {
            return second;
        }
    }

    private List<Document> createDocuments(NodeRef nodeRef, boolean isNew)
    {
        Map<ChildAssociationRef, Counter> nodeCounts = getNodeCounts(nodeRef);
        List<Document> docs = new ArrayList<Document>();
        ChildAssociationRef qNameRef = null;
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

        Collection<Path> directPaths = nodeService.getPaths(nodeRef, false);
        Collection<Pair<Path, QName>> categoryPaths = getCategoryPaths(nodeRef, properties);
        Collection<Pair<Path, QName>> paths = new ArrayList<Pair<Path, QName>>(directPaths.size() + categoryPaths.size());
        for (Path path : directPaths)
        {
            paths.add(new Pair<Path, QName>(path, null));
        }
        paths.addAll(categoryPaths);

        Document xdoc = new Document();
        xdoc.add(new Field("ID", nodeRef.getId(), true, true, false));
        boolean isAtomic = true;
        for (QName propertyName : properties.keySet())
        {
            Serializable value = properties.get(propertyName);
            isAtomic = indexProperty(nodeRef, propertyName, value, xdoc, isAtomic);
        }

        boolean isRoot = nodeRef.equals(nodeService.getRootNode(nodeRef.getStoreRef()));

        StringBuilder parentBuffer = new StringBuilder();
        StringBuilder qNameBuffer = new StringBuilder(64);

        int containerCount = 0;
        for (Iterator<Pair<Path, QName>> it = paths.iterator(); it.hasNext(); /**/)
        {
            Pair<Path, QName> pair = it.next();
            // Lucene flags in order are: Stored, indexed, tokenised

            qNameRef = getLastRefOrNull(pair.getFirst());

            String pathString = pair.getFirst().toString();
            if ((pathString.length() > 0) && (pathString.charAt(0) == '/'))
            {
                pathString = pathString.substring(1);
            }

            String parentString = getParentString(pair.getFirst());

            if (isRoot)
            {
                // Root node
            }
            else if (pair.getFirst().size() == 1)
            {
                // Pseudo root node ignore
            }
            else
            // not a root node
            {
                Counter counter = nodeCounts.get(qNameRef);
                // If we have something in a container with root aspect we will
                // not find it

                if ((counter == null) || (counter.getRepeat() < counter.getCountInParent()))
                {
                    if ((qNameRef != null) && (qNameRef.getParentRef() != null) && (qNameRef.getQName() != null))
                    {
                        if (qNameBuffer.length() > 0)
                        {
                            qNameBuffer.append(";/");
                        }
                        qNameBuffer.append(qNameRef.getQName().toString());
                        xdoc.add(new Field("PARENT", qNameRef.getParentRef().getId(), true, true, false));
                        xdoc.add(new Field("TYPEQNAME", qNameRef.getTypeQName().toString(), true, false, false));
                        xdoc.add(new Field("LINKASPECT", (pair.getSecond() == null) ? "" : pair.getSecond().toString(), true, true, false));
                    }
                }

                if (counter != null)
                {
                    counter.increment();
                }

                // TODO: DC: Should this also include aspect child definitions?
                QName nodeTypeRef = nodeService.getType(nodeRef);
                TypeDefinition nodeTypeDef = getDictionaryService().getType(nodeTypeRef);
                // check for child associations
                if (nodeTypeDef.getChildAssociations().size() > 0)
                {
                    if (directPaths.contains(pair.getFirst()))
                    {
                        Document directoryEntry = new Document();
                        directoryEntry.add(new Field("ID", nodeRef.getId(), true, true, false));
                        directoryEntry.add(new Field("PATH", pathString, true, true, true));
                        directoryEntry.add(new Field("ANCESTOR", parentString, true, true, true));
                        directoryEntry.add(new Field("ISCONTAINER", "T", true, true, false));

                        if (isCategory(getDictionaryService().getType(nodeService.getType(nodeRef))))
                        {
                            directoryEntry.add(new Field("ISCATEGORY", "T", true, true, false));
                        }

                        docs.add(directoryEntry);
                    }
                }
            }
        }

        // Root Node
        if (isRoot)
        {
            // TODO: Does the root element have a QName?
            xdoc.add(new Field("ISCONTAINER", "T", true, true, false));
            xdoc.add(new Field("PATH", "", true, true, true));
            xdoc.add(new Field("QNAME", "", true, true, true));
            xdoc.add(new Field("ISROOT", "T", true, true, false));
            xdoc.add(new Field("ISNODE", "T", true, true, false));
            docs.add(xdoc);

        }
        else
        // not a root node
        {
            xdoc.add(new Field("QNAME", qNameBuffer.toString(), true, true, true));
            // xdoc.add(new Field("PARENT", parentBuffer.toString(), true, true,
            // true));

            ChildAssociationRef primary = nodeService.getPrimaryParent(nodeRef);
            xdoc.add(new Field("PRIMARYPARENT", primary.getParentRef().getId(), true, true, false));
            xdoc.add(new Field("TYPE", nodeService.getType(nodeRef).toString(), true, true, false));
            for (QName classRef : nodeService.getAspects(nodeRef))
            {
                xdoc.add(new Field("ASPECT", classRef.toString(), true, true, false));
            }

            xdoc.add(new Field("ISROOT", "F", true, true, false));
            xdoc.add(new Field("ISNODE", "T", true, true, false));
            if (isAtomic)
            {
                xdoc.add(new Field("FTSSTATUS", "Clean", true, true, false));
            }
            else
            {
                if (isNew)
                {
                    xdoc.add(new Field("FTSSTATUS", "New", true, true, false));
                }
                else
                {
                    xdoc.add(new Field("FTSSTATUS", "Dirty", true, true, false));
                }
            }
            xdoc.add(new Field("TX", deltaId, true, true, false));

            // {
            docs.add(xdoc);
            // }
        }

        return docs;
    }

    private String getParentString(Path path)
    {
        StringBuilder parentBuffer = new StringBuilder(128);
        ArrayList<NodeRef> parentsInDepthOrderStartingWithSelf = new ArrayList<NodeRef>(8);
        for (Iterator<Path.Element> elit = path.iterator(); elit.hasNext(); /**/)
        {
            Path.Element element = elit.next();
            if (!(element instanceof Path.ChildAssocElement))
            {
                throw new IndexerException("Confused path: " + path);
            }
            Path.ChildAssocElement cae = (Path.ChildAssocElement) element;
            parentsInDepthOrderStartingWithSelf.add(0, cae.getRef().getChildRef());

        }
        for (NodeRef ref : parentsInDepthOrderStartingWithSelf)
        {
            if (parentBuffer.length() != 0)
            {
                parentBuffer.append(" ");
            }
            parentBuffer.append(ref.getId());
        }
        parentsInDepthOrderStartingWithSelf.clear();
        return parentBuffer.toString();
    }

    private ChildAssociationRef getLastRefOrNull(Path path)
    {
        if (path.last() instanceof Path.ChildAssocElement)
        {
            Path.ChildAssocElement cae = (Path.ChildAssocElement) path.last();
            return cae.getRef();
        }
        else
        {
            return null;
        }
    }

    private boolean indexProperty(NodeRef nodeRef, QName propertyName, Serializable value, Document doc, boolean isAtomic)
    {
        boolean store = true;
        boolean index = true;
        boolean tokenise = true;
        boolean atomic = true;

        PropertyDefinition propertyDef = getDictionaryService().getProperty(propertyName);
        if (propertyDef != null)
        {
            index = propertyDef.isIndexed();
            store = propertyDef.isStoredInIndex();
            tokenise = propertyDef.isTokenisedInIndex();
            atomic = propertyDef.isIndexedAtomically();
        }
        isAtomic &= atomic;

        if (value != null)
        {
            // convert value to String
            for (String strValue : ValueConverter.getCollection(String.class, value))
            {
                if (strValue != null)
                {
                    // String strValue = ValueConverter.convert(String.class,
                    // value);
                    // TODO: Need to add with the correct language based
                    // analyser
                    if (index && atomic)
                    {
                        if (propertyName.equals(contentPropertyQName))
                        {
                            ContentReader reader = contentService.getReader(nodeRef);
                            if (reader != null)
                            {
                                ContentWriter writer = contentService.getTempWriter();
                                writer.setMimetype("text/plain");
                                if (contentService.isTransformable(reader, writer))
                                {
                                   try
                                   {
                                       contentService.transform(reader, writer);
                                       doc.add(Field.Text("TEXT", new InputStreamReader(writer.getReader().getContentInputStream())));
                                   }
                                   catch (NoTransformerException e)
                                   {
                                       // if it does not convert we did not write
                                       doc.add(Field.Text("TEXT", ""));
                                   }
                                }
                                else
                                {
                                    doc.add(Field.Text("TEXT", ""));
                                }
                            }
                        }
                        doc.add(new Field("@" + propertyName, strValue, store, index, tokenise));
                    }
                }
            }
        }
        return isAtomic;
    }

    private Map<ChildAssociationRef, Counter> getNodeCounts(NodeRef nodeRef)
    {
        Map<ChildAssociationRef, Counter> nodeCounts = new HashMap<ChildAssociationRef, Counter>(5);
        List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(nodeRef);
        // count the number of times the association is duplicated
        for (ChildAssociationRef assoc : parentAssocs)
        {
            Counter counter = nodeCounts.get(assoc);
            if (counter == null)
            {
                counter = new Counter();
                nodeCounts.put(assoc, counter);
            }
            counter.incrementParentCount();

        }
        return nodeCounts;
    }

    private Collection<Pair<Path, QName>> getCategoryPaths(NodeRef nodeRef, Map<QName, Serializable> properties)
    {
        ArrayList<Pair<Path, QName>> categoryPaths = new ArrayList<Pair<Path, QName>>();
        Set<QName> aspects = nodeService.getAspects(nodeRef);

        for (QName classRef : aspects)
        {
            AspectDefinition aspDef = getDictionaryService().getAspect(classRef);
            if (isCategorised(aspDef))
            {
                LinkedList<Pair<Path, QName>> aspectPaths = new LinkedList<Pair<Path, QName>>();
                for (PropertyDefinition propDef : aspDef.getProperties().values())
                {
                    if (propDef.getDataType().getName().equals(DataTypeDefinition.CATEGORY))
                    {
                        for (NodeRef catRef : ValueConverter.getCollection(NodeRef.class, properties.get(propDef.getName())))
                        {
                            if (catRef != null)
                            {
                                for (Path path : nodeService.getPaths(catRef, false))
                                {
                                    if ((path.size() > 1) && (path.get(1) instanceof Path.ChildAssocElement))
                                    {
                                        Path.ChildAssocElement cae = (Path.ChildAssocElement) path.get(1);
                                        boolean isFakeRoot = true;
                                        for (ChildAssociationRef car : nodeService.getParentAssocs(cae.getRef().getChildRef()))
                                        {
                                            if (cae.getRef().equals(car))
                                            {
                                                isFakeRoot = false;
                                                break;
                                            }
                                        }
                                        if (isFakeRoot)
                                        {
                                            if (path.toString().indexOf(aspDef.getName().toString()) != -1)
                                            {
                                                aspectPaths.add(new Pair<Path, QName>(path, aspDef.getName()));
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                categoryPaths.addAll(aspectPaths);
            }
        }
        // Add member final element
        for (Pair<Path, QName> pair : categoryPaths)
        {
            if (pair.getFirst().last() instanceof Path.ChildAssocElement)
            {
                Path.ChildAssocElement cae = (Path.ChildAssocElement) pair.getFirst().last();
                ChildAssociationRef assocRef = cae.getRef();
                pair.getFirst().append(new Path.ChildAssocElement(new ChildAssociationRef(assocRef.getTypeQName(), assocRef.getChildRef(), QName.createQName("member"), nodeRef)));
            }
        }

        return categoryPaths;
    }

    private boolean isCategorised(AspectDefinition aspDef)
    {
        AspectDefinition current = aspDef;
        while (current != null)
        {
            if (current.getName().equals(ContentModel.ASPECT_CLASSIFIABLE))
            {
                return true;
            }
            else
            {
                QName parentName = current.getParentName();
                if (parentName == null)
                {
                    break;
                }
                current = getDictionaryService().getAspect(parentName);
            }
        }
        return false;
    }

    private boolean isCategory(TypeDefinition typeDef)
    {
        if (typeDef == null)
        {
            return false;
        }
        TypeDefinition current = typeDef;
        while (current != null)
        {
            if (current.getName().equals(ContentModel.TYPE_CATEGORY))
            {
                return true;
            }
            else
            {
                QName parentName = current.getParentName();
                if (parentName == null)
                {
                    break;
                }
                current = getDictionaryService().getType(parentName);
            }
        }
        return false;
    }

    public void updateFullTextSearch(int size) throws LuceneIndexException
    {
        checkAbleToDoWork(true);
        try
        {
            String lastId = null;

            toFTSIndex = new ArrayList<Helper>(size);
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(new TermQuery(new Term("FTSSTATUS", "Dirty")), false, false);
            booleanQuery.add(new TermQuery(new Term("FTSSTATUS", "New")), false, false);

            int count = 0;
            Searcher searcher = null;
            LuceneResultSet results = null;
            try
            {
                searcher = getSearcher(null);
                Hits hits;
                try
                {
                    hits = searcher.search(booleanQuery);
                }
                catch (IOException e)
                {
                    throw new LuceneIndexException("Failed to excute query to find content which needs updating in the index", e);
                }
                results = new LuceneResultSet(store, hits, searcher, nodeService, null);

                for (ResultSetRow row : results)
                {
                    LuceneResultSetRow lrow = (LuceneResultSetRow) row;
                    Helper helper = new Helper(lrow.getNodeRef(), lrow.getDocument(), lrow.getIndex());
                    toFTSIndex.add(helper);
                    if (++count >= size)
                    {
                        break;
                    }
                }
                count = results.length();
            }
            finally
            {
                if (results != null)
                {
                    results.close(); // closes the searcher
                }
                else if (searcher != null)
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

            IndexWriter writer = null;
            try
            {
                writer = getDeltaWriter();
                for (Helper helper : toFTSIndex)
                {
                    Document document = helper.document;
                    NodeRef ref = helper.nodeRef;
                    Map<QName, Serializable> properties = nodeService.getProperties(ref);

                    for (QName propertyQName : properties.keySet())
                    {
                        boolean store = true;
                        boolean index = true;
                        boolean tokenise = true;
                        boolean atomic = true;

                        PropertyDefinition propertyDefinition = getDictionaryService().getProperty(propertyQName);
                        if (propertyDefinition != null)
                        {
                            index = propertyDefinition.isIndexed();
                            store = propertyDefinition.isStoredInIndex();
                            tokenise = propertyDefinition.isTokenisedInIndex();
                            atomic = propertyDefinition.isIndexedAtomically();
                        }

                        Serializable value = properties.get(propertyQName);
                        if (value != null)
                        {
                            // convert value to String
                            for (String strValue : ValueConverter.getCollection(String.class, value))
                            {

                                // TODO: Need converter here
                                // Conversion should be done in the anlyser as
                                // we
                                // may
                                // take
                                // advantage of tokenisation

                                // Need to add with the correct language based
                                // analyser
                                if (index && !atomic)
                                {
                                    document.removeFields(propertyQName.toString());
                                    if (propertyQName.equals(contentPropertyQName))
                                    {
                                        ContentReader reader = contentService.getReader(ref);
                                        if (reader != null)
                                        {
                                            ContentWriter cwriter = contentService.getTempWriter();
                                            cwriter.setMimetype("text/plain");
                                            try
                                            {
                                                contentService.transform(reader, cwriter);
                                                document.add(Field.Text("TEXT", new InputStreamReader(cwriter.getReader().getContentInputStream())));
                                            }
                                            catch (NoTransformerException e)
                                            {
                                                // if it does not convert we did
                                                // not
                                                // write and text
                                            }
                                        }
                                    }

                                    document.add(new Field("@" + propertyQName, strValue, store, index, tokenise));
                                }
                            }
                        }
                    }

                    document.removeField("FTSSTATUS");
                    document.add(new Field("FTSSTATUS", "Clean", true, true, false));

                    try
                    {
                        writer.addDocument(document /*
                                                     * TODO: Select the language
                                                     * based analyser
                                                     */);
                    }
                    catch (IOException e)
                    {
                        throw new LuceneIndexException("Failed to add docuement while updating fts index", e);
                    }

                    // Need to do all the current id in the TX - should all be
                    // together so skip until id changes
                    if (writer.docCount() > size)
                    {
                        String id = document.getField("ID").stringValue();
                        if (lastId == null)
                        {
                            lastId = id;
                        }
                        if (!lastId.equals(id))
                        {
                            break;
                        }
                    }
                }

                remainingCount = count - writer.docCount();
            }
            catch (LuceneIndexException e)
            {
                if (writer != null)
                {
                    closeDeltaWriter();
                }
            }

        }
        catch (LuceneIndexException e)
        {
            setRollbackOnly();
            throw new LuceneIndexException("Failed FTS update", e);
        }
    }

    public void registerCallBack(FTSIndexerAware callBack)
    {
        this.callBack = callBack;
    }

    private static class Helper
    {
        NodeRef nodeRef;

        Document document;

        int index;

        boolean update = false;

        Helper(NodeRef nodeRef, Document document, int index)
        {
            this.nodeRef = nodeRef;
            this.document = document;
            this.index = index;
        }
    }

    private static class Command
    {
        NodeRef nodeRef;

        Action action;

        Command(NodeRef nodeRef, Action action)
        {
            this.nodeRef = nodeRef;
            this.action = action;
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer();
            if (action == Action.INDEX)
            {
                buffer.append("Index ");
            }
            else if (action == Action.DELETE)
            {
                buffer.append("Delete ");
            }
            else if (action == Action.REINDEX)
            {
                buffer.append("Reindex ");
            }
            else
            {
                buffer.append("Unknown ... ");
            }
            buffer.append(nodeRef);
            return buffer.toString();
        }

    }

    private FullTextSearchIndexer luceneFullTextSearchIndexer;

    public void setLuceneFullTextSearchIndexer(FullTextSearchIndexer luceneFullTextSearchIndexer)
    {
        this.luceneFullTextSearchIndexer = luceneFullTextSearchIndexer;
    }
    
    public Set<NodeRef> getDeletions()
    {
        return Collections.unmodifiableSet(deletions);
    }
}
