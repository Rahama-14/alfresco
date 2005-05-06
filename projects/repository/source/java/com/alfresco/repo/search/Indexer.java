/*
 * Created on Mar 24, 2005
 * 
 * TODO Comment this class
 * 
 * 
 */
package org.alfresco.repo.search;

import org.alfresco.repo.ref.ChildAssocRef;
import org.alfresco.repo.ref.NodeRef;

/**
 * This interface abstracts how indexing is used from within the node service
 * implementation.
 * 
 * It has to optionally offer transactional integration For example, the lucene
 * indexer
 * 
 * @author andyh
 */

public interface Indexer
{
    /**
     * Create an index entry when a new node is created. A node is always
     * created with a name in a given parent and so a relationship ref is
     * required.
     * 
     * @param relationshipRef
     */
    public void createNode(ChildAssocRef relationshipRef);

    /**
     * Update an index entry due to property changes on a node. There are no
     * strucural impications from such a change.
     * 
     * @param nodeRef
     */
    public void updateNode(NodeRef nodeRef);

    /**
     * Delete a node entry from an index. This implies structural change. The
     * node will be deleted from the index. This will also remove any remaining
     * refernces to the node from the index. The index has no idea of the
     * primary link.
     * 
     * @param relationshipRef
     */
    public void deleteNode(ChildAssocRef relationshipRef);

    /**
     * Create a refernce link between a parent and child. Implies only
     * (potential) structural changes
     * 
     * @param relationshipRef
     */
    public void createChildRelationship(ChildAssocRef relationshipRef);

    /**
     * Alter the relationship between parent and child nodes in the index.
     * 
     * This can be used for:
     * <OL>
     * <LI> rename,
     * <LI> move,
     * <LI> move and rename,
     * <LI> replace
     * </OL>
     * 
     * This could be implemented as a delete and add but some implementations
     * may be able to optimise this operation.
     * 
     * @param relationshipBeforeRef
     * @param relationshipAfterRef
     */
    public void updateChildRelationship(ChildAssocRef relationshipBeforeRef, ChildAssocRef relationshipAfterRef);

    /**
     * Delete a relationship between a parent and child.
     * 
     * This will remove a structural route through the index. The index has no
     * idea of reference and primary relationships and will happily remove the
     * primary relationship before refernces which could remain.
     * 
     * Use delete to ensure all strctural references are removed or call this
     * sure you are doing an unlink (remove a hard link in the unix file system
     * world).
     * 
     * @param relationshipRef
     */
    public void deleteChildRelationship(ChildAssocRef relationshipRef);
    
    

}
