package org.alfresco.service.cmr.repository;

import java.io.Serializable;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

/**
 * This class represents a child relationship between two nodes. This
 * relationship is named.
 * <p>
 * So it requires the parent node ref, the child node ref and the name of the
 * child within the particular parent.
 * <p>
 * This combination is not a unique identifier for the relationship with regard
 * to structure. In use this does not matter as we have no concept of order,
 * particularly in the index.
 * 
 * @author andyh
 * 
 */
public class ChildAssociationRef implements EntityRef, Serializable
{
    private static final long serialVersionUID = 4051322336257127729L;

    private QName assocTypeQName;
    private NodeRef parentRef;
    private QName childQName;
    private NodeRef childRef;
    private boolean isPrimary;
    private int nthSibling;
    

    /**
     * Construct a representation of a parent --- name ----> child relationship.
     * 
     * @param assocTypeQName
     *            the type of the association
     * @param parentRef
     *            the parent reference - may be null
     * @param childQName
     *            the qualified name of the association - may be null
     * @param childRef
     *            the child node reference. This must not be null.
     * @param isPrimary
     *            true if this represents the primary parent-child relationship
     * @param nthSibling
     *            the nth association with the same properties. Usually -1 to be
     *            ignored.
     */
    public ChildAssociationRef(
            QName assocTypeQName,
            NodeRef parentRef,
            QName childQName,
            NodeRef childRef,
            boolean isPrimary,
            int nthSibling)
    {
        this.assocTypeQName = assocTypeQName;
        this.parentRef = parentRef;
        this.childQName = childQName;
        this.childRef = childRef;
        this.isPrimary = isPrimary;
        this.nthSibling = nthSibling;

        // check
        if (childRef == null)
        {
            throw new IllegalArgumentException("Child reference may not be null");
        }
    }

    /**
     * Constructs a <b>non-primary</b>, -1th sibling parent-child association
     * reference.
     * 
     * @see ChildAssociationRef#ChildAssocRef(QName, NodeRef, QName, NodeRef, boolean, int)
     */
    public ChildAssociationRef(QName assocTypeQName, NodeRef parentRef, QName childQName, NodeRef childRef)
    {
        this(assocTypeQName, parentRef, childQName, childRef, false, -1);
    }
    
    /**
     * Get the qualified name of the association type
     * 
     * @return Returns the qualified name of the parent-child association type
     *      as defined in the data dictionary.  It may be null if this is the
     *      imaginary association to the root node.
     */
    public QName getTypeQName()
    {
        return assocTypeQName;
    }

    /**
     * Get the qualified name of the parent-child association
     * 
     * @return Returns the qualified name of the parent-child association. It
     *         may be null if this is the imaginary association to a root node.
     */
    public QName getQName()
    {
        return childQName;
    }

    /**
     * @return Returns the child node reference - never null
     */
    public NodeRef getChildRef()
    {
        return childRef;
    }

    /**
     * @return Returns the parent node reference, which may be null if this
     *         represents the imaginary reference to the root node
     */
    public NodeRef getParentRef()
    {
        return parentRef;
    }

    /**
     * @return Returns true if this represents a primary association
     */
    public boolean isPrimary()
    {
        return isPrimary;
    }

    /**
     * @return Returns the nth sibling required
     */
    public int getNthSibling()
    {
        return nthSibling;
    }

    /**
     * Compares:
     * <ul>
     * <li>{@link #assocTypeQName}</li>
     * <li>{@link #parentRef}</li>
     * <li>{@link #childRef}</li>
     * <li>{@link #childQName}</li>
     * </ul>
     */
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ChildAssociationRef))
        {
            return false;
        }
        ChildAssociationRef other = (ChildAssociationRef) o;

        return (EqualsHelper.nullSafeEquals(this.assocTypeQName, other.assocTypeQName)
                && EqualsHelper.nullSafeEquals(this.parentRef, other.parentRef)
                && EqualsHelper.nullSafeEquals(this.childQName, other.childQName)
                && EqualsHelper.nullSafeEquals(this.childRef, other.childRef));
    }

    public int hashCode()
    {
        int hashCode = ((getTypeQName() == null) ? 0 : getTypeQName().hashCode());
        hashCode = 37 * hashCode + ((getParentRef() == null) ? 0 : getParentRef().hashCode());
        hashCode = 37 * hashCode + ((getQName() == null) ? 0 : getQName().hashCode());
        hashCode = 37 * hashCode + getChildRef().hashCode();
        return hashCode;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[").append(getTypeQName()).append("]");
        sb.append(getParentRef());
        sb.append(" --- ").append(getQName()).append(" ---> ");
        sb.append(getChildRef());
        return sb.toString();
    }
}
