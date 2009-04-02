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
package org.alfresco.repo.search.impl.querymodel.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.cmis.mapping.CMISMapping;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.FunctionalConstraint;
import org.alfresco.repo.search.impl.querymodel.Join;
import org.alfresco.repo.search.impl.querymodel.JoinType;
import org.alfresco.repo.search.impl.querymodel.PropertyArgument;
import org.alfresco.repo.search.impl.querymodel.Selector;
import org.alfresco.repo.search.impl.querymodel.Source;
import org.alfresco.repo.search.impl.querymodel.impl.functions.Equals;
import org.alfresco.service.namespace.QName;

/**
 * @author andyh
 */
public class BaseJoin implements Join
{
    private Constraint joinConstraint;

    private JoinType joinType;

    private Source left;

    private Source right;

    public BaseJoin(Source left, Source right, JoinType joinType, Constraint joinConstraint)
    {
        this.left = left;
        this.right = right;
        this.joinType = joinType;
        this.joinConstraint = joinConstraint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.Join#getJoinCondition()
     */
    public Constraint getJoinCondition()
    {
        return joinConstraint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.Join#getJoinType()
     */
    public JoinType getJoinType()
    {
        return joinType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.Join#getLeft()
     */
    public Source getLeft()
    {
        return left;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.Join#getRight()
     */
    public Source getRight()
    {
        return right;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("BaseJoin[");
        builder.append("Left=" + getLeft()).append(", ");
        builder.append("Right=" + getRight()).append(", ");
        builder.append("JoinType=" + getJoinType()).append(", ");
        builder.append("Condition=" + getJoinCondition());
        builder.append("]");
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.Source#getSelectorNames()
     */
    public Map<String, Selector> getSelectors()
    {
        HashMap<String, Selector> answer = new HashMap<String, Selector>();
        Map<String, Selector> leftSelectors = left.getSelectors();
        for (String selectorName : leftSelectors.keySet())
        {
            Selector selector = leftSelectors.get(selectorName);
            if (answer.put(selectorName, selector) != null)
            {
                throw new DuplicateSelectorNameException("There is a duplicate selector name for " + selectorName);
            }
        }
        Map<String, Selector> rightSelectors = right.getSelectors();
        for (String selectorName : rightSelectors.keySet())
        {
            Selector selector = rightSelectors.get(selectorName);
            if (answer.put(selectorName, selector) != null)
            {
                throw new DuplicateSelectorNameException("There is a duplicate selector name for " + selectorName);
            }
        }
        return answer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.repo.search.impl.querymodel.Source#getSelector(java.lang.String)
     */
    public Selector getSelector(String name)
    {
        Map<String, Selector> answer = getSelectors();
        return answer.get(name);
    }

    public List<Set<String>> getSelectorGroups()
    {
        List<Set<String>> answer = new ArrayList<Set<String>>();

        List<Set<String>> left = getLeft().getSelectorGroups();
        List<Set<String>> right = getRight().getSelectorGroups();

        FunctionalConstraint joinCondition = (FunctionalConstraint) getJoinCondition();
        if (!joinCondition.getFunction().getName().equals(Equals.NAME))
        {
            throw new UnsupportedOperationException("Only equi-joins are supported");
        }

        Argument lhs = joinCondition.getFunctionArguments().get(Equals.ARG_LHS);
        Argument rhs = joinCondition.getFunctionArguments().get(Equals.ARG_RHS);

        String lhsSelector = null;
        String rhsSelector = null;

        if (lhs instanceof PropertyArgument)
        {
            PropertyArgument propertyArgument = (PropertyArgument) lhs;
            QName qname = propertyArgument.getPropertyName();
            if (isObjectId(qname))
            {
                lhsSelector = propertyArgument.getSelector();
            }
        }

        if (rhs instanceof PropertyArgument)
        {
            PropertyArgument propertyArgument = (PropertyArgument) rhs;
            QName qname = propertyArgument.getPropertyName();
            if (isObjectId(qname))
            {
                rhsSelector = propertyArgument.getSelector();
            }
        }

        if ((getJoinType() == JoinType.INNER) && (lhsSelector != null) && (rhsSelector != null))
        {

            TOADD: for (Set<String> toAddTo : left)
            {
                if (toAddTo.contains(lhsSelector))
                {
                    TOMOVE: for (Set<String> toMove : right)
                    {
                        if (toMove.contains(rhsSelector))
                        {
                            toAddTo.addAll(toMove);
                            toMove.clear();
                            break TOMOVE;
                        }
                    }
                    break TOADD;
                }
                if (toAddTo.contains(rhsSelector))
                {
                    TOMOVE: for (Set<String> toMove : right)
                    {
                        if (toMove.contains(lhsSelector))
                        {
                            toAddTo.addAll(toMove);
                            toMove.clear();
                            break TOMOVE;
                        }
                    }
                    break TOADD;
                }
            }
        }

        // remove any empty sets

        for (Set<String> group : left)
        {
            if (group.size() > 0)
            {
                answer.add(group);
            }
        }
        for (Set<String> group : right)
        {
            if (group.size() > 0)
            {
                answer.add(group);
            }
        }

        return answer;
    }

    private boolean isObjectId(QName qname)
    {
        return ContentModel.PROP_NODE_DBID.equals(qname) || CMISMapping.PROP_OBJECT_ID_QNAME.equals(qname);
    }
}
