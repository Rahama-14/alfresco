/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
package org.alfresco.repo.copy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Handles compound behavioural callbacks for the same dictionary class (type or aspect).
 * <p>
 * When multiple policy handlers register callback for the same model class, an instance
 * of this class is used to resolve the calls.  The behaviour is sometimes able to resolve
 * conflicts and sometimes not.  Look at the individual callback methods to see how
 * conflicts are handled.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public class CompoundCopyBehaviourCallback extends AbstractCopyBehaviourCallback
{
    private static Log logger = LogFactory.getLog(CompoundCopyBehaviourCallback.class);
    
    private QName classQName;
    private List<CopyBehaviourCallback> callbacks;
    
    /**
     * 
     * @param classQName            the 
     */
    public CompoundCopyBehaviourCallback(QName classQName)
    {
        this.classQName = classQName;
        callbacks = new ArrayList<CopyBehaviourCallback>(2);
    }
    
    public void addBehaviour(CopyBehaviourCallback callback)
    {
        callbacks.add(callback);
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("\n")
          .append("CompoundCopyBehaviourCallback: \n")
          .append("      Model Class: ").append(classQName);
        boolean first = true;
        for (CopyBehaviourCallback callback : callbacks)
        {
            if (first)
            {
                first = false;
                sb.append("\n");
            }
            sb.append("      ").append(callback.getClass().getName());
        }
        return sb.toString();
    }
    
    /**
     * Individual callbacks effectively have a veto on the copy i.e. if one of the
     * callbacks returns <tt>false</tt> for {@link CopyBehaviourCallback#mustCopy(NodeRef)},
     * then the copy will NOT proceed.  However, a warning is generated indicating that
     * there is a conflict in the defined behaviour.
     * 
     * @return          Returns <tt>true</tt> if all registered callbacks return <tt>true</tt>
     *                  or <b><tt>false</tt> if any of the  registered callbacks return <tt>false</tt></b>.
     */
    public boolean getMustCopy(QName classQName, CopyDetails copyDetails)
    {
        CopyBehaviourCallback firstVeto = null;
        for (CopyBehaviourCallback callback : callbacks)
        {
            boolean mustCopyLocal = callback.getMustCopy(classQName, copyDetails);
            if (firstVeto == null && !mustCopyLocal)
            {
                firstVeto = callback;
            }
            if (mustCopyLocal && firstVeto != null)
            {
                // The callback says 'copy' but there is already a veto in place
                logger.warn(
                        "CopyBehaviourCallback '" + callback.getClass().getName() + "' " +
                        "is attempting to induce a copy when callback '" + firstVeto.getClass().getName() +
                        "' has already vetoed it.  Copying of '" + copyDetails.getSourceNodeRef() +
                        "' will not occur.");
            }
        }
        // Done
        if (firstVeto == null)
        {
            // Allowed by all
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "All copy behaviours voted for a copy of node \n" +
                        "   " + copyDetails + "\n" +
                        "   " + this);
            }
            return true;
        }
        else
        {
            // Vetoed
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Copy behaviour vetoed for node " + copyDetails.getSourceNodeRef() + "\n" +
                        "   First veto: " + firstVeto.getClass().getName() + "\n" +
                        "   " + copyDetails + "\n" +
                        "   " + this);
            }
            return false;
        }
    }

    /**
     * Uses the {@link ChildAssocCopyAction} ordering to drive priority i.e. a vote
     * to copy will override a vote NOT to copy.
     * 
     * @return          Returns the most lively choice of action 
     */
    public ChildAssocCopyAction getChildAssociationCopyAction(
            QName classQName,
            CopyDetails copyDetails,
            CopyChildAssociationDetails childAssocCopyDetails)
    {
        ChildAssocCopyAction bestAction = ChildAssocCopyAction.IGNORE;
        for (CopyBehaviourCallback callback : callbacks)
        {
            ChildAssocCopyAction action = callback.getChildAssociationCopyAction(
                    classQName,
                    copyDetails,
                    childAssocCopyDetails);
            if (action.compareTo(bestAction) > 0)
            {
                // We've trumped the last best one
                bestAction = action;
            }
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Child association copy behaviour: " + bestAction + "\n" +
                    "   " + childAssocCopyDetails + "\n" +
                    "   " + copyDetails + "\n" +
                    "   " + this);
        }
        return bestAction;
    }

    /**
     * Uses the {@link ChildAssocRecurseAction} ordering to drive recursive copy behaviour.
     * 
     * @return          Returns the most lively choice of action 
     */
    @Override
    public ChildAssocRecurseAction getChildAssociationRecurseAction(
            QName classQName,
            CopyDetails copyDetails,
            CopyChildAssociationDetails childAssocCopyDetails)
    {
        ChildAssocRecurseAction bestAction = ChildAssocRecurseAction.RESPECT_RECURSE_FLAG;
        for (CopyBehaviourCallback callback : callbacks)
        {
            ChildAssocRecurseAction action = callback.getChildAssociationRecurseAction(
                    classQName,
                    copyDetails,
                    childAssocCopyDetails);
            if (action.compareTo(bestAction) > 0)
            {
                // We've trumped the last best one
                bestAction = action;
            }
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Child association recursion behaviour: " + bestAction + "\n" +
                    "   " + childAssocCopyDetails + "\n" +
                    "   " + copyDetails + "\n" +
                    "   " + this);
        }
        return bestAction;
    }

    /**
     * The lowest common denominator applies here.  The properties are passed to each
     * callback in turn.  The resulting map is then passed to the next callback and so
     * on.  If any callback removes or alters properties, these will not be recoverable.
     * 
     * @return          Returns the least properties assigned for the copy by any individual
     *                  callback handler
     */
    public Map<QName, Serializable> getCopyProperties(
            QName classQName,
            CopyDetails copyDetails,
            Map<QName, Serializable> properties)
    {
        Map<QName, Serializable> copyProperties = new HashMap<QName, Serializable>(properties);
        for (CopyBehaviourCallback callback : callbacks)
        {
            copyProperties = callback.getCopyProperties(classQName, copyDetails, properties);
        }
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Copy properties: \n" +
                    "   " + copyDetails + "\n" +
                    "   " + this + "\n" +
                    "   Before: " + properties + "\n" +
                    "   After:  " + copyProperties);
        }
        return copyProperties;
    }
}
