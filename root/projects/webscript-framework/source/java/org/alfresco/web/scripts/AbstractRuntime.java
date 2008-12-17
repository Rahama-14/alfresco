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
package org.alfresco.web.scripts;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Encapsulates the execution of a single Web Script.
 *
 * Sub-classes of WebScriptRuntime maintain the execution environment e.g. servlet
 * request & response.
 * 
 * A new instance of WebScriptRuntime is required for each invocation.
 * 
 * @author davidc
 */
public abstract class AbstractRuntime implements Runtime
{
    // Logger
    protected static final Log logger = LogFactory.getLog(AbstractRuntime.class);

    /** Component Dependencies */
    protected RuntimeContainer container;

    /**
     * Construct
     * 
     * @param container  web script context
     */
    public AbstractRuntime(RuntimeContainer container)
    {
        this.container = container;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Runtime#getContainer()
     */
    public Container getContainer()
    {
        return container;
    }
    
    /**
     * Execute the Web Script encapsulated by this Web Script Runtime
     */
    final public void executeScript()
    {
        long startRuntime = System.nanoTime();

        String method = getScriptMethod();
        String scriptUrl = null;
        Match match = null;

        try
        {
            // extract script url
            scriptUrl = getScriptUrl();
            if (scriptUrl == null || scriptUrl.length() == 0)
            {
                throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Script URL not specified");
            }

            if (logger.isDebugEnabled())
                logger.debug("(Runtime=" + getName() + ", Container=" + container.getName() + ") Processing script url ("  + method + ") " + scriptUrl);

            match = container.getRegistry().findWebScript(method, scriptUrl);
            if (match == null || match.getKind() == Match.Kind.URI)
            {
                if (match == null)
                {
                    String msg = "Script url " + scriptUrl + " does not map to a Web Script.";
                    if (logger.isDebugEnabled())
                        logger.debug(msg);
                    throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, msg);
                }
                else
                {
                    String msg = "Script url " + scriptUrl + " does not support the method " + method;
                    if (logger.isDebugEnabled())
                        logger.debug(msg);
                    throw new WebScriptException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
                }
            }

            // create web script request & response
            final WebScriptRequest scriptReq = createRequest(match);
            final WebScriptResponse scriptRes = createResponse();
            final Authenticator auth = createAuthenticator();
            
            if (logger.isDebugEnabled())
                logger.debug("Agent: " + scriptReq.getAgent());

            long startScript = System.nanoTime();
            final WebScript script = match.getWebScript();
            final Description description = script.getDescription();
            
            try
            {
                if (logger.isDebugEnabled())
                {
                    String reqFormat = scriptReq.getFormat();
                    String format = (reqFormat == null || reqFormat.length() == 0) ? "[undefined]" : reqFormat;
                    Description desc = scriptReq.getServiceMatch().getWebScript().getDescription();
                    logger.debug("Invoking Web Script " + description.getId() + " (format " + format + ", style: " + desc.getFormatStyle() + ", default: " + desc.getDefaultFormat() + ")");
                }

                executeScript(scriptReq, scriptRes, auth);
            }
            finally
            {
                if (logger.isDebugEnabled())
                {
                    long endScript = System.nanoTime();
                    logger.debug("Web Script " + description.getId() + " executed in " + (endScript - startScript)/1000000f + "ms");
                }
            }
        }
        catch(Throwable e)
        {
            if (logger.isInfoEnabled())
                logger.info("Caught exception & redirecting to status template: " + e.getMessage());
            if (logger.isDebugEnabled())
                logger.debug("Caught exception: " + e.toString());

            // setup context
            WebScriptRequest req = createRequest(null);
            WebScriptResponse res = createResponse();

            // extract status code, if specified
            int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            StatusTemplate statusTemplate = null;
            Map<String, Object> statusModel = null;
            if (e instanceof WebScriptException)
            {
                WebScriptException we = (WebScriptException)e;
                statusCode = we.getStatus();
                statusTemplate = we.getStatusTemplate();
                statusModel = we.getStatusModel();
            }

            // retrieve status template for response rendering
            if (statusTemplate == null)
            {
                // locate status template
                // NOTE: search order...
                //   1) root located <status>.ftl
                //   2) root located status.ftl
                statusTemplate = getStatusCodeTemplate(statusCode);
                if (!container.getTemplateProcessor().hasTemplate(statusTemplate.getPath()))
                {
                    statusTemplate = getStatusTemplate();
                    if (!container.getTemplateProcessor().hasTemplate(statusTemplate.getPath()))
                    {
                        throw new WebScriptException("Failed to find status template " + statusTemplate.getPath() + " (format: " + statusTemplate.getFormat() + ")");
                    }
                }                
            }

            // create basic model for all information known at this point, if one hasn't been pre-provided
            if (statusModel == null || statusModel.equals(Collections.EMPTY_MAP))
            {
                statusModel = new HashMap<String, Object>(8, 1.0f);
                statusModel.put("url", new URLModel(req));
                statusModel.put("server", container.getDescription());
                statusModel.put("date", new Date());
                if (match != null && match.getWebScript() != null)
                {
                    statusModel.put("webscript", match.getWebScript().getDescription());
                }
            }

            // add status to model
            Status status = new Status();
            status.setCode(statusCode);
            status.setMessage(e.getMessage());
            status.setException(e);
            statusModel.put("status", status);

            // render output
            String mimetype = container.getFormatRegistry().getMimeType(req.getAgent(), statusTemplate.getFormat());
            if (mimetype == null)
            {
                throw new WebScriptException("Web Script format '" + statusTemplate.getFormat() + "' is not registered");
            }
            
            if (logger.isDebugEnabled())
            {
                logger.debug("Force success status header in response: " + req.forceSuccessStatus());
                logger.debug("Sending status " + statusCode + " (Template: " + statusTemplate.getPath() + ")");
                logger.debug("Rendering response: content type=" + mimetype);
            }

            res.reset();
            Cache cache = new Cache();
            cache.setNeverCache(true);
            res.setCache(cache);
            res.setStatus(req.forceSuccessStatus() ? HttpServletResponse.SC_OK : statusCode);
            res.setContentType(Format.HTML.mimetype() + ";charset=UTF-8");
            try
            {
                container.getTemplateProcessor().process(statusTemplate.getPath(), statusModel, res.getWriter());
            }
            catch (IOException e1)
            {
                throw new WebScriptException("Internal error", e1);
            }
        }
        finally
        {
            long endRuntime = System.nanoTime();
            if (logger.isDebugEnabled())
                logger.debug("Processed script url ("  + method + ") " + scriptUrl + " in " + (endRuntime - startRuntime)/1000000f + "ms");
        }
    }

    /**
     * Execute script given the specified context
     * 
     * @param scriptReq
     * @param scriptRes
     * @param auth
     * 
     * @throws IOException
     */
    protected void executeScript(WebScriptRequest scriptReq, WebScriptResponse scriptRes, Authenticator auth)
        throws IOException
    {
        container.executeScript(scriptReq, scriptRes, auth);
    }

    /**
     * Get code specific Status Template path
     * 
     * @param statusCode
     * @return  path
     */
    protected StatusTemplate getStatusCodeTemplate(int statusCode)
    {
        return new StatusTemplate("/" + statusCode + ".ftl", WebScriptResponse.HTML_FORMAT);
    }
    
    /**
     * Get Status Template path
     * 
     * @return  path
     */
    protected StatusTemplate getStatusTemplate()
    {
        return new StatusTemplate("/status.ftl", WebScriptResponse.HTML_FORMAT);
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Runtime#getScriptParameters()
     */
    public Map<String, Object> getScriptParameters()
    {
        return Collections.emptyMap();
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Runtime#getTemplateParameters()
     */
    public Map<String, Object> getTemplateParameters()
    {
        return Collections.emptyMap();
    }
    
    /**
     * Get the Web Script Method  e.g. get, post
     * 
     * @return  web script method
     */
    protected abstract String getScriptMethod();

    /**
     * Get the Web Script Url
     * 
     * @return  web script url
     */
    protected abstract String getScriptUrl();
    
    /**
     * Create a Web Script Request
     * 
     * @param match  web script matching the script method and url
     * @return  web script request
     */
    protected abstract WebScriptRequest createRequest(Match match);
    
    /**
     * Create a Web Script Response
     * 
     * @return  web script response
     */
    protected abstract WebScriptResponse createResponse();
    
    /**
     * Create a Web Script Authenticator
     * 
     * @return  web script authenticator
     */
    protected abstract Authenticator createAuthenticator();

}
