/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.repo.transfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.transfer.NodeFinder;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.springframework.extensions.surf.util.ParameterCheck;

/**
 * @author brian
 * 
 */
public class ChildAssociatedNodeFinder implements NodeFinder
{
    private Set<QName> suppliedAssociationTypes = new HashSet<QName>();
    private boolean exclude = false;
    private boolean initialised = false;
    private List<QName> childAssociationTypes = new ArrayList<QName>();
    private ServiceRegistry serviceRegistry;

    public ChildAssociatedNodeFinder()
    {
    }

    public ChildAssociatedNodeFinder(Set<QName> associationTypeNames)
    {
        setAssociationTypes(associationTypeNames);
    }

    public ChildAssociatedNodeFinder(QName... associationTypeNames)
    {
        setAssociationTypes(associationTypeNames);
    }

    public ChildAssociatedNodeFinder(Set<QName> associationTypeNames, boolean exclude)
    {
        setAssociationTypes(associationTypeNames);
        this.exclude = exclude;
    }

    public ChildAssociatedNodeFinder(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    public ChildAssociatedNodeFinder(ServiceRegistry serviceRegistry, Set<QName> associationTypeNames)
    {
        this(serviceRegistry);
        setAssociationTypes(associationTypeNames);
    }

    public ChildAssociatedNodeFinder(ServiceRegistry serviceRegistry, QName... associationTypeNames)
    {
        this(serviceRegistry);
        setAssociationTypes(associationTypeNames);
    }

    public ChildAssociatedNodeFinder(ServiceRegistry serviceRegistry, Set<QName> associationTypeNames, boolean exclude)
    {
        setAssociationTypes(associationTypeNames);
        this.exclude = exclude;
    }

    public void setAssociationTypes(QName... associationTypes)
    {
        setAssociationTypes(Arrays.asList(associationTypes));
    }

    public void setAssociationTypes(Collection<QName> associationTypes)
    {
        this.suppliedAssociationTypes = new HashSet<QName>(associationTypes);
        initialised = false;
    }

    /**
     * @param exclude
     *            the exclude to set
     */
    public void setExclude(boolean exclude)
    {
        this.exclude = exclude;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.service.cmr.transfer.NodeFinder#findFrom(org.alfresco.service.cmr.repository.NodeRef,
     * org.alfresco.service.ServiceRegistry)
     */
    public Set<NodeRef> findFrom(NodeRef thisNode)
    {
        if (!initialised)
        {
            init();
        }
        if (exclude)
        {
            return processExcludedSet(thisNode);
        }
        else
        {
            return processIncludedSet(thisNode);
        }
    }

    /**
     * @param thisNode
     * @param serviceRegistry
     * @return
     */
    private Set<NodeRef> processExcludedSet(NodeRef thisNode)
    {
        Set<NodeRef> results = new HashSet<NodeRef>(89);
        NodeService nodeService = serviceRegistry.getNodeService();

        // Find all the child nodes (filtering as necessary).
        List<ChildAssociationRef> children = nodeService.getChildAssocs(thisNode);
        boolean filterChildren = !childAssociationTypes.isEmpty();
        for (ChildAssociationRef child : children)
        {
            if (!filterChildren || !childAssociationTypes.contains(child.getTypeQName()))
            {
                results.add(child.getChildRef());
            }
        }
        return results;
    }

    private Set<NodeRef> processIncludedSet(NodeRef startingNode)
    {
        NodeService nodeService = serviceRegistry.getNodeService();
        Set<NodeRef> foundNodes = new HashSet<NodeRef>(89);
        for (QName assocType : childAssociationTypes)
        {
            List<ChildAssociationRef> children = nodeService.getChildAssocs(startingNode, assocType,
                    RegexQNamePattern.MATCH_ALL);
            for (ChildAssociationRef child : children)
            {
                foundNodes.add(child.getChildRef());
            }
        }
        return foundNodes;
    }

    /**
     * @param serviceRegistry
     *            the serviceRegistry to set
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    public void init()
    {
        ParameterCheck.mandatory("serviceRegistry", serviceRegistry);
        // Quickly scan the supplied association types and remove any that either
        // do not exist or are not child association types.
        DictionaryService dictionaryService = serviceRegistry.getDictionaryService();
        childAssociationTypes.clear();
        for (QName associationType : suppliedAssociationTypes)
        {
            AssociationDefinition assocDef = dictionaryService.getAssociation(associationType);
            if (assocDef != null && assocDef.isChild())
            {
                childAssociationTypes.add(associationType);
            }
        }
        initialised = true;
    }

}
