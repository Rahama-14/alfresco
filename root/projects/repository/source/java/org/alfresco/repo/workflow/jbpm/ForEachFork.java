/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.repo.workflow.jbpm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowException;
import org.dom4j.Element;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.instantiation.FieldInstantiator;
import org.springframework.beans.factory.BeanFactory;


/**
 * For each "item in collection", create a fork.
 */
public class ForEachFork extends JBPMSpringActionHandler
{
    private static final long serialVersionUID = 4643103713602441652L;

    private ServiceRegistry services;
    
    private Element foreach;
    private String var;


    /* (non-Javadoc)
     * @see org.alfresco.repo.workflow.jbpm.JBPMSpringActionHandler#initialiseHandler(org.springframework.beans.factory.BeanFactory)
     */
    @Override
    protected void initialiseHandler(BeanFactory factory)
    {
        services = (ServiceRegistry)factory.getBean(ServiceRegistry.SERVICE_REGISTRY);
    }
    
    /**
     * Create a new child token for each item in list.
     * 
     * @param executionContext
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void execute(final ExecutionContext executionContext)
        throws Exception
    {
        //
        // process action handler arguments
        //
        
        if (foreach == null)
        {
            throw new WorkflowException("forEach has not been provided");
        }

        // build "for each" collection
        List forEachColl = null;
        String forEachCollStr = foreach.getTextTrim();
        if (forEachCollStr != null)
        {
            if (forEachCollStr.startsWith("#{"))
            {
                String expression = forEachCollStr.substring(2, forEachCollStr.length() -1);
                Object eval = AlfrescoJavaScript.executeScript(executionContext, services, expression, null);
                if (eval == null)
                {
                    throw new WorkflowException("forEach expression '" + forEachCollStr + "' evaluates to null");
                }
                
                // expression evaluates to string
                if (eval instanceof String)
                {
                    String[] forEachStrs = ((String)eval).trim().split(",");
                    forEachColl = new ArrayList(forEachStrs.length);
                    for (String forEachStr : forEachStrs)
                    {
                        forEachColl.add(forEachStr);
                    }
                }
                
                // expression evaluates to Node array
                else if (eval instanceof Serializable[])
                {
                    Serializable[] nodes = (Serializable[])eval;
                    forEachColl = new ArrayList(nodes.length);
                    for (Serializable node : nodes)
                    {
                        if (node instanceof NodeRef)
                        {
                            forEachColl.add(new JBPMNode((NodeRef)node, services));
                        }
                    }
                }
                
                // expression evaluates to collection
                else if (eval instanceof Collection)
                {
                    forEachColl = (List)eval;
                }
                
            }
        }
        else
        {
            forEachColl = (List)FieldInstantiator.getValue(List.class, foreach);
        }
        
        if (var == null || var.length() == 0)
        {
            throw new WorkflowException("forEach variable name has not been provided");
        }
        
        //
        // create forked paths
        //
        
        Token rootToken = executionContext.getToken();
        Node node = executionContext.getNode();
        List<ForkedTransition> forkTransitions = new ArrayList<ForkedTransition>();

        // first, create a new token and execution context for each item in list
        for (int i = 0; i < node.getLeavingTransitions().size(); i++)
        {
            Transition transition = (Transition) node.getLeavingTransitions().get(i);

            for (int iVar = 0; iVar < forEachColl.size(); iVar++)
            {
                // create child token to represent new path
                String tokenName = getTokenName(rootToken, transition.getName(), iVar); 
                Token loopToken = new Token(rootToken, tokenName);
                loopToken.setTerminationImplicit(true);
                executionContext.getJbpmContext().getSession().save(loopToken);
            
                // assign variable within path
                final ExecutionContext newExecutionContext = new ExecutionContext(loopToken);
                newExecutionContext.getContextInstance().createVariable(var, forEachColl.get(iVar), loopToken);
                
                // record path & transition
                ForkedTransition forkTransition = new ForkedTransition();
                forkTransition.executionContext = newExecutionContext;
                forkTransition.transition = transition;
                forkTransitions.add(forkTransition);
            }
        }

        //
        // let each new token leave the node.
        //
        for (ForkedTransition forkTransition : forkTransitions)
        {
            node.leave(forkTransition.executionContext, forkTransition.transition);
        }
    }

    /**
     * Create a token name
     * 
     * @param parent
     * @param transitionName
     * @return
     */
    protected String getTokenName(Token parent, String transitionName, int loopIndex)
    {
        String tokenName = null;
        if (transitionName != null && transitionName.length() > 0)
        {
            if (!parent.hasChild(transitionName))
            {
                tokenName = transitionName;
            }
            else
            {
                int i = 2;
                tokenName = transitionName + Integer.toString(i);
                while (parent.hasChild(tokenName))
                {
                    i++;
                    tokenName = transitionName + Integer.toString(i);
                }
            }
        }
        else
        {
            // no transition name
            int size = (parent.getChildren() != null) ? parent.getChildren().size() + 1 : 1;
            tokenName = "FOREACHFORK" + Integer.toString(size);
        }
        return tokenName + "." + loopIndex;
    }
    

    /**
     * Fork Transition
     */
    private class ForkedTransition
    {
        private ExecutionContext executionContext;
        private Transition transition;
    }

}
