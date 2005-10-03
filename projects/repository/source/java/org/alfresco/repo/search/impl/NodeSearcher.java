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
package org.alfresco.repo.search.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.alfresco.repo.search.DocumentNavigator;
import org.alfresco.repo.search.NodeServiceXPath;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.XPathException;
import org.alfresco.service.cmr.search.QueryParameterDefinition;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.jaxen.JaxenException;

/**
 * Helper class that walks a node hierarchy.
 * <p>
 * Some searcher methods on
 * {@link org.alfresco.service.cmr.search.SearchService} can use this directly
 * as its only dependencies are
 * {@link org.alfresco.service.cmr.repository.NodeService},
 * {@link org.alfresco.service.cmr.dictionary.DictionaryService} and a
 * {@link org.alfresco.service.cmr.search.SearchService}
 * 
 * @author Derek Hulley
 */
public class NodeSearcher
{
    private NodeService nodeService;

    private DictionaryService dictionaryService;

    private SearchService searchService;

    public NodeSearcher(NodeService nodeService, DictionaryService dictionaryService, SearchService searchService)
    {
        this.nodeService = nodeService;
        this.dictionaryService = dictionaryService;
        this.searchService = searchService;
    }

    /**
     * @see NodeServiceXPath
     */
    public synchronized List<NodeRef> selectNodes(NodeRef contextNodeRef, String xpathIn,
            QueryParameterDefinition[] paramDefs, NamespacePrefixResolver namespacePrefixResolver,
            boolean followAllParentLinks, String language)
    {
        try
        {
            String xpath = xpathIn;
            boolean useJCRXPath = language.equalsIgnoreCase(SearchService.LANGUAGE_JCR_XPATH);

            List<AttributeOrder> order = null;

            // replace element
            if (useJCRXPath)
            {
                order = new ArrayList<AttributeOrder>();
                // We do not allow variable substitution with this pattern
                xpath = xpath.replaceAll("element\\(\\s*(\\*|\\w*:\\w*)\\s*,\\s*(\\*|\\w*:\\w*)\\s*\\)",
                        "$1[subtypeOf(\"$2\")]");
                String split[] = xpath.split("order\\s*by\\s*", 2);
                xpath = split[0];

                if (split.length > 1 && split[1].length() > 0)
                {
                    String clauses[] = split[1].split("\\s,\\s");

                    for (String clause : clauses)
                    {
                        if (clause.startsWith("@"))
                        {
                            String attribute = clause.replaceFirst("@(\\p{Alpha}[\\w:]*)(?:\\s+(.*))?", "$1");
                            String sort = clause.replaceFirst("@(\\p{Alpha}[\\w:]*)(?:\\s+(.*))?", "$2");

                            if (sort.length() == 0)
                            {
                                sort = "ascending";
                            }

                            QName attributeQName = QName.createQName(attribute, namespacePrefixResolver);
                            order.add(new AttributeOrder(attributeQName, sort.equalsIgnoreCase("ascending")));
                        }
                        else if (clause.startsWith("jcr:score"))
                        {
                            // ignore jcr:score ordering
                        }
                        else
                        {
                            throw new IllegalArgumentException("Malformed order by expression " + split[1]);
                        }
                    }

                }

            }

            DocumentNavigator documentNavigator = new DocumentNavigator(dictionaryService, nodeService, searchService,
                    namespacePrefixResolver, followAllParentLinks, useJCRXPath);
            NodeServiceXPath nsXPath = new NodeServiceXPath(xpath, documentNavigator, paramDefs);
            for (String prefix : namespacePrefixResolver.getPrefixes())
            {
                nsXPath.addNamespace(prefix, namespacePrefixResolver.getNamespaceURI(prefix));
            }
            List list = nsXPath.selectNodes(nodeService.getPrimaryParent(contextNodeRef));
            HashSet<NodeRef> unique = new HashSet<NodeRef>(list.size());
            for (Object o : list)
            {
                if (o instanceof ChildAssociationRef)
                {
                    unique.add(((ChildAssociationRef) o).getChildRef());
                }
                else if (o instanceof DocumentNavigator.Property)
                {
                    unique.add(((DocumentNavigator.Property) o).parent);
                }
                else
                {
                    throw new XPathException("Xpath expression must only select nodes");
                }
            }

            List<NodeRef> answer = new ArrayList<NodeRef>(unique.size());
            answer.addAll(unique);
            if (order != null)
            {
                orderNodes(answer, order);
                for(NodeRef node : answer)
                {
                    StringBuffer buffer = new StringBuffer();
                    for (AttributeOrder attOrd : order)
                    {
                        buffer.append(" ").append(nodeService.getProperty(node, attOrd.attribute));
                    }
                    System.out.println(buffer.toString());
                }
                System.out.println();
            }
            return answer;
        }
        catch (JaxenException e)
        {
            throw new XPathException("Error executing xpath: \n" + "   xpath: " + xpathIn, e);
        }
    }

    private void orderNodes(List<NodeRef> answer, List<AttributeOrder> order)
    {
        Collections.sort(answer, new NodeRefComparator(nodeService, order));
    }

    static class NodeRefComparator implements Comparator<NodeRef>
    {
        List<AttributeOrder> order;
        NodeService nodeService;
        
        NodeRefComparator(NodeService nodeService, List<AttributeOrder> order)
        {
            this.nodeService = nodeService;
            this.order = order;
        }
        
        public int compare(NodeRef n1, NodeRef n2)
        {
            for (AttributeOrder attributeOrder : order)
            {
                Serializable o1 = nodeService.getProperty(n1, attributeOrder.attribute);
                Serializable o2 = nodeService.getProperty(n2, attributeOrder.attribute);

                if (o1 == null)
                {
                    if (o2 == null)
                    {
                        continue;
                    }
                    else
                    {
                        return attributeOrder.ascending ? -1 : 1;
                    }
                }
                else
                {
                    if (o2 == null)
                    {
                        return attributeOrder.ascending ? 1 : -1;
                    }
                    else
                    {
                        if ((o1 instanceof Comparable) && (o2 instanceof Comparable))
                        {
                            return (attributeOrder.ascending ? 1 : -1) * ((Comparable)o1).compareTo((Comparable) o2);
                        }
                        else
                        {
                            continue;
                        }
                    }
                }

            }
            return 0;
        }
    }

    /**
     * @see NodeServiceXPath
     */
    public List<Serializable> selectProperties(NodeRef contextNodeRef, String xpath,
            QueryParameterDefinition[] paramDefs, NamespacePrefixResolver namespacePrefixResolver,
            boolean followAllParentLinks, String language)
    {
        try
        {
            boolean useJCRXPath = language.equalsIgnoreCase(SearchService.LANGUAGE_JCR_XPATH);

            DocumentNavigator documentNavigator = new DocumentNavigator(dictionaryService, nodeService, searchService,
                    namespacePrefixResolver, followAllParentLinks, useJCRXPath);
            NodeServiceXPath nsXPath = new NodeServiceXPath(xpath, documentNavigator, paramDefs);
            for (String prefix : namespacePrefixResolver.getPrefixes())
            {
                nsXPath.addNamespace(prefix, namespacePrefixResolver.getNamespaceURI(prefix));
            }
            List list = nsXPath.selectNodes(nodeService.getPrimaryParent(contextNodeRef));
            List<Serializable> answer = new ArrayList<Serializable>(list.size());
            for (Object o : list)
            {
                if (!(o instanceof DocumentNavigator.Property))
                {
                    throw new XPathException("Xpath expression must only select nodes");
                }
                answer.add(((DocumentNavigator.Property) o).value);
            }
            return answer;
        }
        catch (JaxenException e)
        {
            throw new XPathException("Error executing xpath", e);
        }
    }

    private static class AttributeOrder
    {
        QName attribute;

        boolean ascending;

        AttributeOrder(QName attribute, boolean ascending)
        {
            this.attribute = attribute;
            this.ascending = ascending;
        }
    }
}
