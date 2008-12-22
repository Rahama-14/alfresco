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

package org.alfresco.module.vti.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* <p>VtiRequestDispatcher provides the front-end controller for dispatching among 
* set of VtiActions. It selects and invokes a realization of {@link VtiAction} to 
* perform the requested business logic.</p>
*
* @author Stas Sokolovsky
*   
*/
public class VtiRequestDispatcher extends HttpServlet
{
    
    public static final String VTI_ALFRESCO_CONTEXT = "ALFRESCO-DEPLOYMENT-CONTEXT";

    private static final long serialVersionUID = 2257788564135460595L;

    private static final String MULTIPLIER_PATTERN = "*";

    private static final String URI_PARAM_NAME = "Uri";

    private static final String REQUEST_METHOD_PARAM_NAME = "Request-method";

    private static final String REQUEST_ATTRIBUTE = "Request-attribute";

    private List<ActionMapping> exactMatchActions;

    private List<ActionMapping> prefixActions;

    private List<ActionMapping> postfixActions;

    private List<ActionMapping> customRules;

    private String context = "";

    private static Log logger = LogFactory.getLog(VtiRequestDispatcher.class);
    
    /**
     * <p>
     * VtiRequestDispatcher is initialized by list of {@link ActionMapping}.
     * </p>
     * 
     * @param actionList list the action mappings
     */
    public VtiRequestDispatcher(List<ActionMapping> actionList)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Initializing VtiRequestDispatcher");
        }
        exactMatchActions = new ArrayList<ActionMapping>();
        prefixActions = new ArrayList<ActionMapping>();
        postfixActions = new ArrayList<ActionMapping>();
        customRules = new ArrayList<ActionMapping>();
        if (actionList != null)
        {
            for (ActionMapping actionMapping : actionList)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Adding action for pattern '" + actionMapping.getUriPattern() + "'");
                }
                if (actionMapping.getCustomRules() != null)
                {
                    customRules.add(actionMapping);
                }
                else
                {
                    String pattern = actionMapping.getUriPattern();
                    if (pattern.endsWith(MULTIPLIER_PATTERN))
                    {
                        prefixActions.add(actionMapping);
                    }
                    else if (pattern.startsWith(MULTIPLIER_PATTERN))
                    {
                        postfixActions.add(actionMapping);
                    }
                    else
                    {
                        exactMatchActions.add(actionMapping);
                    }
                }
            }
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("VtiRequestDispatcher was successfully initalized");
        }
    }
    
    /**
     * <p>Process a HTTP request.</p>
     *
     * @param request processing servlet request 
     * @param response creating servlet response
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        request.setAttribute(VTI_ALFRESCO_CONTEXT, context);
        if (logger.isDebugEnabled())
        {
            logger.debug("Process request");
        }
        doActions(request, response);
    }

    /**
     * <p>Context setter.</p>
     *
     * @param context specific context for all requests  
     */
    public void setContext(String context)
    {
        this.context = context;
    }

    private void doActions(ServletRequest request, ServletResponse response) throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String uri = getUri(httpRequest);

        if (logger.isDebugEnabled())
        {
            logger.debug("Find appropriate action by specific rules");
        }
        VtiAction targetAction = null;

        for (ActionMapping actionMapping : customRules)
        {
            if (isRulesAccepted(httpRequest, actionMapping))
            {
                targetAction = actionMapping.getAction();
                break;
            }
        }
        if (logger.isDebugEnabled())
        {
            if (targetAction == null)
            {
                logger.debug("Specific rule not found");
                logger.debug("Find appropriate action by pattern for uri='" + uri + "'");
            }
        }
        if (targetAction == null)
        {
            for (ActionMapping actionMapping : exactMatchActions)
            {
                if (isMatch(uri, actionMapping.getUriPattern()))
                {
                    targetAction = actionMapping.getAction();
                    break;
                }
            }
        }
        if (targetAction == null)
        {
            int maxPatternLength = 0;
            for (ActionMapping actionMapping : postfixActions)
            {
                String pattern = actionMapping.getUriPattern();
                if (isMatch(uri, pattern))
                {
                    if (pattern.length() > maxPatternLength)
                    {
                        targetAction = actionMapping.getAction();
                        maxPatternLength = pattern.length();
                    }
                }
            }
        }
        if (targetAction == null)
        {
            int maxPatternLength = 0;
            for (ActionMapping actionMapping : prefixActions)
            {
                String pattern = actionMapping.getUriPattern();
                if (isMatch(uri, pattern))
                {
                    if (pattern.length() > maxPatternLength)
                    {
                        targetAction = actionMapping.getAction();
                        maxPatternLength = pattern.length();
                    }
                }
            }
        }
        if (targetAction != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Action found for request");
                logger.debug("Execute target action");
            }
            targetAction.execute(httpRequest, httpResponse);
        }
    }

    private boolean isRulesAccepted(HttpServletRequest request, ActionMapping actionMapping)
    {
        Set<Entry<String, Pattern>> entries = actionMapping.getCustomRules().entrySet();
        boolean result = true;
        for (Entry<String, Pattern> entry : entries)
        {
            String analyzedData = null;
            if (entry.getKey().equals(URI_PARAM_NAME))
            {
                analyzedData = request.getRequestURL() != null ? request.getRequestURL().toString() : null;
            }
            else if (entry.getKey().equals(REQUEST_METHOD_PARAM_NAME))
            {
                analyzedData = request.getMethod() != null ? request.getMethod().toString() : null;
            }
            else if (entry.getKey().startsWith(REQUEST_ATTRIBUTE))
            {
                analyzedData = (String) request.getAttribute(entry.getKey().substring(REQUEST_ATTRIBUTE.length() + 1));
            }
            else
            {
                analyzedData = request.getHeader(entry.getKey());
            }
            if (analyzedData == null)
                analyzedData = "";
            Matcher matcher = entry.getValue().matcher(analyzedData);
            if (!matcher.find())
            {
                result = false;
                break;
            }
        }
        return result;
    }

    private String getUri(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        if (context != null && uri.startsWith(context))
        {
            uri = uri.substring(context.length());
        }
        return uri;
    }

    private boolean isMatch(String uri, String pattern)
    {
        boolean result = false;
        if (uri != null && pattern != null && pattern.length() > 1)
        {
            if (pattern.endsWith(MULTIPLIER_PATTERN))
            {
                if (uri.contains(pattern.substring(0, pattern.length() - 1)))
                {
                    result = true;
                }
            }
            else if (pattern.startsWith(MULTIPLIER_PATTERN))
            {
                if (uri.endsWith(pattern.substring(1, pattern.length())))
                {
                    result = true;
                }
            }
            else
            {
                if (uri.contains(pattern))
                {
                    result = true;
                }
            }
        }
        return result;
    }
    
    /**
    * <p>ActionMapping provides configuration class that define mapping of url-pattern and
    * specific rules to {@link VtiAction}.</p>
    *
    */
    public static class ActionMapping
    {

        private String uriPattern;

        private Map<String, Pattern> customRules = null;

        private VtiAction action;

        public void setCustomRules(Map<String, String> accessRules)
        {
            Set<Entry<String, String>> entries = accessRules.entrySet();
            customRules = new HashMap<String, Pattern>();
            for (Entry<String, String> entry : entries)
            {
                String requestparam = entry.getKey();
                String regexp = entry.getValue();
                this.customRules.put(requestparam, Pattern.compile(regexp));
            }
        }

        public String getUriPattern()
        {
            return uriPattern;
        }

        public void setUriPattern(String uriPattern)
        {
            this.uriPattern = uriPattern;
        }

        public VtiAction getAction()
        {
            return action;
        }

        public void setAction(VtiAction action)
        {
            this.action = action;
        }

        public Map<String, Pattern> getCustomRules()
        {
            return customRules;
        }

    }

}
