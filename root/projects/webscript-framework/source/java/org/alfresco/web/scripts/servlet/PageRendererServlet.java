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
 * http://www.alfresco.com/legal/licensing
 */
package org.alfresco.web.scripts.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.config.Config;
import org.alfresco.config.ConfigService;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.Content;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.scripts.AbstractRuntime;
import org.alfresco.web.scripts.Authenticator;
import org.alfresco.web.scripts.Cache;
import org.alfresco.web.scripts.Match;
import org.alfresco.web.scripts.PresentationScriptProcessor;
import org.alfresco.web.scripts.Registry;
import org.alfresco.web.scripts.Runtime;
import org.alfresco.web.scripts.ScriptContent;
import org.alfresco.web.scripts.Store;
import org.alfresco.web.scripts.WebScriptRequest;
import org.alfresco.web.scripts.WebScriptRequestURLImpl;
import org.alfresco.web.scripts.WebScriptResponse;
import org.alfresco.web.scripts.WebScriptResponseImpl;
import org.alfresco.web.scripts.Description.RequiredAuthentication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import freemarker.cache.TemplateLoader;

/**
 * Servlet for rendering pages based a behavior driven FreeMarker Template and scoped
 * WebScript UI components.
 * 
 * GET: /<context>/<servlet>/[resource]...
 *  resource - app url resource
 *  url args - passed to all webscript component urls for the page
 * 
 * Read from page renderer web-app config:
 *  serverurl - url to Alfresco host repo server (i.e. http://servername:8080/alfresco)
 * 
 * Read from object model for spk:site - pass to webscript urls as "well known tokens"
 *  theme - default theme for the site (can be override in user prefs?) 
 * 
 * @author Kevin Roast
 */
public class PageRendererServlet extends WebScriptServlet
{
   private static Log logger = LogFactory.getLog(PageRendererServlet.class);
   
   private static final String MIMETYPE_HTML = "text/html;charset=utf-8";
   private static final String PARAM_COMPONENT_ID  = "_alfId";
   private static final String PARAM_COMPONENT_URL = "_alfUrl";
   
   private PageRendererTemplateProcessor templateProcessor;
   private PresentationScriptProcessor scriptProcessor;
   private PageComponentTemplateLoader pageComponentTemplateLoader;
   private Registry webscriptsRegistry;
   private Store pageStore;
   private Store templateStore;
   private Store templateConfigStore;
   private Store componentStore;
   
   private Map<String, CacheValue<PageComponent>> componentCache =
      Collections.synchronizedMap(new HashMap<String, CacheValue<PageComponent>>());
   
   
   @Override
   public void init() throws ServletException
   {
      super.init();
      
      // init required beans - template processor and template loaders
      ApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
      
      webscriptsRegistry = (Registry)context.getBean("webscripts.registry");
      pageStore = (Store)context.getBean("pagerenderer.pagestore");
      pageStore.init();
      componentStore = (Store)context.getBean("pagerenderer.componentstore");
      componentStore.init();
      templateStore = (Store)context.getBean("pagerenderer.templatestore");
      templateStore.init();
      templateConfigStore = (Store)context.getBean("pagerenderer.templateconfigstore");
      templateConfigStore.init();
      templateProcessor = (PageRendererTemplateProcessor)context.getBean("pagerenderer.templateprocessor");
      scriptProcessor = (PresentationScriptProcessor)context.getBean("webscripts.web.scriptprocessor");
      
      // custom loader for resolved UI Component reference indirections
      pageComponentTemplateLoader = new PageComponentTemplateLoader();
      templateProcessor.addTemplateLoader(pageComponentTemplateLoader);
      
      // add template loader for the template store
      templateProcessor.addTemplateLoader(templateStore.getTemplateLoader());
      
      // init the config for the template processor - loaders etc. get resolved
      templateProcessor.initConfig();
      
      // we use a specific config service instance
      configService = (ConfigService)context.getBean("pagerenderer.config");
   }

   @Override
   protected void service(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException
   {
      String uri = req.getRequestURI();
      
      long startTime = 0;
      if (logger.isDebugEnabled())
      {
         String qs = req.getQueryString();
         logger.debug("Processing Page Renderer URL: ("  + req.getMethod() + ") " + uri + 
               ((qs != null && qs.length() != 0) ? ("?" + qs) : ""));
         startTime = System.nanoTime();
      }
      
      uri = uri.substring(req.getContextPath().length());   // skip server context path
      StringTokenizer t = new StringTokenizer(uri, "/");
      t.nextToken();    // skip servlet name
      if (!t.hasMoreTokens())
      {
         throw new IllegalArgumentException("Invalid URL to PageRendererServlet: " + uri);
      }
      
      // get the remaining elements of the url ready for AppUrl lookup 
      StringBuilder buf = new StringBuilder(64);
      while (t.hasMoreTokens())
      {
         buf.append(t.nextToken());
         if (t.hasMoreTokens())
         {
            buf.append('/');
         }
      }
      String resource = buf.toString();
      
      // get URL arguments as a map ready for AppUrl lookup
      Map<String, String> args = new HashMap<String, String>(req.getParameterMap().size(), 1.0f);
      Enumeration names = req.getParameterNames();
      while (names.hasMoreElements())
      {
         String name = (String)names.nextElement();
         args.put(name, req.getParameter(name));
      }
      
      // resolve app url object to process this resource
      if (logger.isDebugEnabled())
         logger.debug("Matching resource URL: " + resource + " args: " + args.toString());
      ApplicationUrl appUrl = matchAppUrl(resource, args);
      if (appUrl == null)
      {
         logger.warn("No Application URL mapping found for resource: " + resource);
         res.setStatus(HttpServletResponse.SC_NOT_FOUND);
         return;
      }
      
      // TODO: what caching here...?
      setNoCacheHeaders(res);
      
      try
      {
         // retrieve the page instance via the application url - will throw a runtime exception if
         // the page cannot be found or fails to retrieve from the local store or repository
         PageInstance page = appUrl.getPageInstance();
         if (logger.isDebugEnabled())
            logger.debug("PageInstance: " + page.toString());
         
         // set response content type and charset
         res.setContentType(MIMETYPE_HTML);
         
         // TODO: authenticate - or redirect to login page etc...
         if (authenticate(getServletContext(), page.getAuthentication()))
         {
            // Setup the PageRenderer context for the webscript runtime template loader to use when
            // rebuilding urls for components - there is a single instance of the template loader
            // and it must be kept thread safe for multiple asynchronous page renderer requests.
            PageRendererContext context = new PageRendererContext();
            context.RequestURI = uri;
            context.RequestPath = req.getContextPath();
            context.PageInstance = page;
            context.Tokens = args;
            
            // handle a clicked UI component link - look for id+url
            // TODO: keep further state of page? i.e. multiple webscripts can be hosted and clicked
            String compId = req.getParameter(PARAM_COMPONENT_ID);
            if (compId != null)
            {
               String compUrl = req.getParameter(PARAM_COMPONENT_URL);
               if (logger.isDebugEnabled())
                  logger.debug("Clicked component found: " + compId + " URL: " + compUrl);
               context.ComponentId = compId;
               context.ComponentUrl = compUrl;
            }
            
            this.pageComponentTemplateLoader.setContext(context);
            
            // Process the page template using our custom loader - the loader will find and buffer
            // individual included webscript output into the Writer out for the servlet page.
            if (logger.isDebugEnabled())
               logger.debug("Page template resolved as: " + page.getTemplate());
            
            try
            {
               // execute the template to render the page - based on the current page definition
               processTemplatePage(page, req, res);
            }
            finally
            {
               // clean up
               this.pageComponentTemplateLoader.setContext(null);
               
               res.getWriter().flush();
               res.getWriter().close();
            }
         }
         if (logger.isDebugEnabled())
         {
            long endTime = System.nanoTime();
            logger.debug("Page render completed in: " + (endTime - startTime)/1000000f + "ms");
         }
      }
      catch (Throwable err)
      {
         throw new AlfrescoRuntimeException("Error occurred during page rendering.\nResource: " +
               resource + "\nError: " + err.getMessage(), err);
      }
   }
   
   /**
    * Match the specified resource url against an app url object, which in turn is responsible
    * for locating the correct Page. 
    *  
    * @param resource   
    * @param args       
    */
   private ApplicationUrl matchAppUrl(String resource, Map<String, String> args)
   {
      // TODO: match against app urls loaded from app registry?
      // TODO: page dispatcher framework plugs in here?
      ApplicationUrl testUrl = new ApplicationUrl(this.pageStore);
      testUrl.match(resource);
      return testUrl;
   }
   
   /**
    * Execute the template to render the main page - based on the specified page instance.
    * 
    * @throws IOException
    */
   private void processTemplatePage(PageInstance page, HttpServletRequest req, HttpServletResponse res)
      throws IOException
   {
      // load the config for the specific template instance - then resolve the template and behaviour
      String templateConfig = page.getTemplate() + ".xml";
      if (this.templateConfigStore.hasDocument(templateConfig))
      {
         // load and parse the template instance config document
         TemplateInstanceConfig templateInstance = new TemplateInstanceConfig(
               this.templateConfigStore, templateConfig); 
         
         // the optional 'format' template support i.e ".format.ftl"
         String format = req.getParameter("format");
         
         // build template name by convention:
         // templatename[.format].ftl
         String template =
            templateInstance.getTemplateType() + 
            ((format != null && format.length() != 0) ? ("." + format + ".ftl") : ".ftl");
         
         // We need to preprocess the template to execute all @region directives
         // - component dependancies are resolved only when they have all executed.
         // Output to a dummy writer as we don't process the result - the custom
         // directive is aware of the active/passive rendering mode and will either lookup
         // the component or execute the component based on the mode.
         
         long startTime = 0;
         if (logger.isDebugEnabled())
         {
            logger.debug("Executing 1st template pass, looking up components...");
            startTime = System.nanoTime();
         }
         
         Map<String, Object> resultModel = new HashMap<String, Object>(8, 1.0f);
         Map<String, Object> templateModel = getModel(page, req, false);
         
         // add the template config values directly to the template root model - this is useful for
         // templates that do not require additional processing in JavaScript, they just need the values
         templateModel.putAll(templateInstance.getPropetries());
         
         // execute any attached javascript behaviour for this template
         // the behaviour plus the config is responsible for specialising the template
         String scriptPath = templateInstance.getTemplateType() + ".js";
         ScriptContent script = templateStore.getScriptLoader().getScript(scriptPath);
         if (script != null)
         {
            Map<String, Object> scriptModel = new HashMap<String, Object>(8, 1.0f);
            // add the template config properties to the script model
            scriptModel.putAll(templateInstance.getPropetries());
            // results from the script should be placed into the root 'model' object
            scriptModel.put("model", resultModel);
            
            scriptProcessor.executeScript(script, scriptModel);
            
            // merge script results model into the template model
            for (Map.Entry<String, Object> entry : resultModel.entrySet())
            {
               // retrieve script model value and unwrap each java object from script object
               Object value = entry.getValue();
               Object templateValue = scriptProcessor.unwrapValue(value);
               templateModel.put(entry.getKey(), templateValue);
            }
         }
         
         // First pass is very fast as template pages themselves have very little implicit content and
         // any associated behaviour logic is executed only once, with the result stored for the 2nd pass.
         // The critical performance path is in executing the WebScript components - which is only
         // performed during the second pass of the template - once component references are all resolved.
         templateProcessor.process(template, templateModel,
            new Writer ()
            {
               public void write(char[] cbuf, int off, int len) throws IOException
               {
               }
            
               public void flush() throws IOException
               {
               }
            
               public void close() throws IOException
               {
               }
            });
         
         if (logger.isDebugEnabled())
         {
            long endTime = System.nanoTime();
            logger.debug("...1st pass processed in: " + (endTime - startTime)/1000000f + "ms");
            logger.debug("Executing 2nd template pass, rendering...");
            startTime = System.nanoTime();
         }
         
         // construct template model for 2nd pass
         templateModel = getModel(page, req, true);
         templateModel.putAll(templateInstance.getPropetries());
         if (script != null)
         {
            // script already executed - so just merge script return model into the template model
            for (Map.Entry<String, Object> entry : resultModel.entrySet())
            {
               // retrieve script model value
               Object value = entry.getValue();
               Object templateValue = scriptProcessor.unwrapValue(value);
               templateModel.put(entry.getKey(), templateValue);
            }
         }
         templateProcessor.process(template, templateModel, res.getWriter());
         
         if (logger.isDebugEnabled())
         {
            long endTime = System.nanoTime();
            logger.debug("...2nd pass processed in: " + (endTime - startTime)/1000000f + "ms");
         }
      }
      else
      {
         throw new AlfrescoRuntimeException("Unable to find template config: " + templateConfig);
      }
   }
   
   /**
    * @return model to use for UI Component template page execution
    */
   private Map<String, Object> getModel(PageInstance page, HttpServletRequest req, boolean active)
   {
      Map<String, Object> model = new HashMap<String, Object>(8);
      
      URLHelper urlHelper = new URLHelper(req);
      model.put("url", urlHelper);
      model.put("description", page.getDescription());
      model.put("title", page.getTitle());
      model.put("theme", page.getTheme());
      model.put("head", page.getHeaderRenderer(webscriptsRegistry, templateProcessor, urlHelper));
      
      // add the custom 'region' directive implementation - one instance per model as we pass in template/page 
      model.put("region", new RegionDirective(componentStore, componentCache, page, active));
      
      return model;
   }
   
   /**
    * @return the configuration object for the PageRenderer
    */
   private Config getConfig()
   {
      return this.configService.getConfig("PageRenderer");
   }
   
   /**
    * Authenticate against the repository using the specified Authentication.
    * @return success/failure
    */
   private static boolean authenticate(ServletContext sc, RequiredAuthentication auth)
   {
      // TODO: authenticate via call to Alfresco server - using web-app config?
      return true;
   }
   
   /**
    * Apply the headers required to disallow caching of the response in the browser
    */
   private static void setNoCacheHeaders(HttpServletResponse res)
   {
      res.setHeader("Cache-Control", "no-cache");
      res.setHeader("Pragma", "no-cache");
   }
   
   /**
    * Helper to replace tokens in a string with values from a map of token->value.
    * Token names in the string are delimited by '{' and '}' - the entire token name
    * plus the delimiters are replaced by the value found in the supplied replacement map.
    * If no replacement value is found for the token name, it is replaced by the empty string.
    * 
    * @param s       String to work on - cannot be null
    * @param tokens  Map of token name -> token value for replacements
    * 
    * @return the replaced string or the original if no tokens found or a failure occurs
    */
   private static String replaceContextTokens(String s, Map<String, String> tokens)
   {
      String result = s;
      int preIndex = 0;
      int delimIndex = s.indexOf('{');
      if (delimIndex != -1)
      {
         StringBuilder buf = new StringBuilder(s.length() + 16);
         do
         {
            // copy up to token delimiter start
            buf.append(s.substring(preIndex, delimIndex));
            
            // extract token and replace
            if (s.length() < delimIndex + 2)
            {
               if (logger.isWarnEnabled())
                  logger.warn("Failed to replace context tokens - malformed input: " + s);
               return s;
            }
            int endDelimIndex = s.indexOf('}', delimIndex + 2);
            if (endDelimIndex == -1)
            {
               if (logger.isWarnEnabled())
                  logger.warn("Failed to replace context tokens - malformed input: " + s);
               return s;
            }
            String token = s.substring(delimIndex + 1, endDelimIndex);
            String replacement = tokens.get(token);
            buf.append(replacement != null ? replacement : "");
            
            // locate next delimiter and mark end of previous delimiter
            preIndex = endDelimIndex + 1; 
            delimIndex = s.indexOf('{', preIndex);
            if (delimIndex == -1 && s.length() > preIndex)
            {
               // append suffix of original string after the last delimiter found
               buf.append(s.substring(preIndex));
            }
         } while (delimIndex != -1);
         
         result = buf.toString();
      }
      return result;
   }
   
   
   /**
    * WebScript runtime for a Page Component included within the PageRenderer servlet context.
    */
   private class PageComponentWebScriptRuntime extends AbstractRuntime
   {
      private PageComponent component;
      private PageRendererContext context;
      private String webScript;
      private String scriptUrl;
      private String encoding;
      private ByteArrayOutputStream baOut = null;
      
      /**
       * Constructor
       * 
       * @param component     The Page Component this runtime should execute
       * @param context       The context for the PageRenderer execution thread
       * @param webScript     The component WebScript url
       * @param executeUrl    The full URL to execute including context path
       * @param encoding      Output encoding
       */
      PageComponentWebScriptRuntime(
            PageComponent component, PageRendererContext context,
            String webScript, String executeUrl, String encoding)
      {
         super(PageRendererServlet.this.container);
         this.component = component;
         this.context = context;
         this.webScript = webScript;
         this.scriptUrl = executeUrl;
         this.encoding = encoding;
         if (logger.isDebugEnabled())
            logger.debug("Constructing runtime for url: " + executeUrl);
      }

      /* (non-Javadoc)
       * @see org.alfresco.web.scripts.Runtime#getName()
       */
      public String getName()
      {
          return "Page Renderer";
      }

      @Override
      protected String getScriptUrl()
      {
         return webScript;
      }

      @Override
      protected WebScriptRequest createRequest(Match match)
      {
         // add/replace the "well known" context tokens in component properties
         Map<String, String> properties = new HashMap<String, String>(8, 1.0f);
         // Component ID is always available to the component
         properties.put("id", component.getId());
         for (String arg : component.getProperties().keySet())
         {
            properties.put(arg, replaceContextTokens(component.getProperties().get(arg), context.Tokens));
         }
         
         // build the request to render this component
         return new WebScriptPageComponentRequest(this, scriptUrl, match, properties);
      }

      /**
       * Create the WebScriptResponse for a UI component.
       * 
       * Create a response object that we control to write to a temporary output buffer that
       * we later use that as the source for the UI component webscript include.
       */
      @Override
      protected WebScriptResponse createResponse()
      {
         try
         {
            baOut = new ByteArrayOutputStream(4096);
            OutputStreamWriter wrOut =
               (encoding == null ? new OutputStreamWriter(baOut) : new OutputStreamWriter(baOut, encoding));
            return new WebScriptPageComponentResponse(this, context, component.getId(), wrOut, baOut);
         }
         catch (UnsupportedEncodingException err)
         {
            throw new AlfrescoRuntimeException("Unsupported encoding.", err);
         }
      }

      @Override
      protected String getScriptMethod()
      {
         return "GET";
      }
      
      /**
       * @return Reader to the UI webscript response. The response has already been buffered
       *         so just return a Reader directly to it.
       */
      public Reader getResponseReader()
      {
         try
         {
            if (baOut == null)
            {
               return null;
            }
            else
            {
               return new InputStreamReader(
                     encoding == null ? new ByteArrayInputStream(baOut.toByteArray()) :
                        new ByteArrayInputStream(baOut.toByteArray()), encoding);
            }
         }
         catch (UnsupportedEncodingException err)
         {
            throw new AlfrescoRuntimeException("Unsupported encoding.", err);
         }
      }

      @Override
      protected Authenticator createAuthenticator()
      {
         return null;
      }
   }
   
   
   /**
    * Simple implementation of a WebScript URL Request for a webscript component on the page.
    * Mostly based on the existing WebScriptRequestURLImpl - just adds support for additional
    * page level context parameters available to the component as args.
    */
   private class WebScriptPageComponentRequest extends WebScriptRequestURLImpl
   {
      private Map<String, String> parameters;
      
      WebScriptPageComponentRequest(
            Runtime runtime, String scriptUrl, Match match, Map<String, String> parameters)
      {
         super(runtime, scriptUrl, match);
         this.parameters = parameters;
      }

      /* (non-Javadoc)
       * @see org.alfresco.web.scripts.WebScriptRequest#getParameterNames()
       */
      public String[] getParameterNames()
      {
         return this.parameters.keySet().toArray(new String[this.parameters.size()]);
      }

      /* (non-Javadoc)
       * @see org.alfresco.web.scripts.WebScriptRequest#getParameter(java.lang.String)
       */
      public String getParameter(String name)
      {
         return this.parameters.get(name);
      }

      /* (non-Javadoc)
       * @see org.alfresco.web.scripts.WebScriptRequest#getParameterValues(java.lang.String)
       */
      public String[] getParameterValues(String name)
      {
         return this.parameters.values().toArray(new String[this.parameters.size()]);
      }
      
      public String getAgent()
      {
         return null;
      }

      public String getServerPath()
      {
         return null;
      }

      public String[] getHeaderNames()
      {
         return new String[] {};
      }
        
      public String getHeader(String name)
      {
         return null;
      }
        
      public String[] getHeaderValues(String name)
      {
         return null;
      }

      public Content getContent()
      {
         return null;
      }
   }
   
   
   /**
    * Implementation of a WebScript Response object for PageRenderer servlet.
    * Mostly based on the existing WebScriptResponseImpl - just adds support for
    * encoding URLs to manage user click requests to any component on the page.
    */
   private class WebScriptPageComponentResponse extends WebScriptResponseImpl
   {
      private Writer outWriter;
      private OutputStream outStream;
      private PageRendererContext context;
      private String componentId;
      
      public WebScriptPageComponentResponse(
            Runtime runtime, PageRendererContext context,
            String componentId, Writer outWriter, OutputStream outStream)
      {
         super(runtime);
         this.context = context;
         this.componentId = componentId;
         this.outWriter = outWriter;
         this.outStream = outStream;
      }
      
      public String encodeScriptUrl(String url)
      {
         // encode to allow presentation tier webscripts to call themselves non this page
         // needs the servlet URL plus args to identify the webscript and it's new url
         return context.RequestPath + context.RequestURI + "?" + PARAM_COMPONENT_URL + "=" +
                URLEncoder.encode(url) + "&" + PARAM_COMPONENT_ID + "=" + componentId;
      }

      public String getEncodeScriptUrlFunction(String name)
      {
         // TODO: may be required?
         return null;
      }

      public OutputStream getOutputStream() throws IOException
      {
         return this.outStream;
      }

      public Writer getWriter() throws IOException
      {
         return this.outWriter;
      }

      public void reset()
      {
         // not supported
      }

      public void setCache(Cache cache)
      {
         // not supported
      }

      public void setHeader(String name, String value)
      {
          // not supported
      }

      public void addHeader(String name, String value)
      {
          // not supported
      }

      public void setContentType(String contentType)
      {
         // not supported
      }

      public void setStatus(int status)
      {
         // not supported
      }
   }
   
   
   /**
    * Template loader that resolves and executes UI WebScript components by looking up layout keys
    * in the template against the component definition service URLs for the page.
    */
   private class PageComponentTemplateLoader implements TemplateLoader
   {
      private ThreadLocal<PageRendererContext> context = new ThreadLocal<PageRendererContext>();
      private long last = 0L;
      private boolean ignoreMissingComponents = true;
      
      PageComponentTemplateLoader()
      {
         Config config = configService.getConfig("PageRenderer");
         if (config != null)
         {
            String value = config.getConfigElementValue("ignoreMissingComponents");
            if (value != null)
            {
               this.ignoreMissingComponents = Boolean.parseBoolean(value);
            }
         }
      }
      
      public void closeTemplateSource(Object templateSource) throws IOException
      {
         // nothing to do - we close all sources during getReader()
      }

      /**
       * Resolve the template source for the specified template name.
       * This custom handles UI component references with the [id] syntax
       */
      public Object findTemplateSource(String name) throws IOException
      {
         // The webscript is looked up based on the key in the #include directive - it must
         // be of the form "[key]" so that it can be recognised by the loader
         if (name.startsWith("[") && name.endsWith("]"))
         {
            String key = name.substring(1, name.length() - 1);
            
            if (logger.isDebugEnabled())
               logger.debug("Found WebScript UI component key: " + key);
            
            return key;
         }
         else
         {
            return null;
         }
      }

      /**
       * @return Last modified date of this template - as the result of this component is not
       *         an actual template but dynamic content, we have no useful last modified date.
       */
      public long getLastModified(Object templateSource)
      {
         return -1L;
      }

      /**
       * Return the reader for the specified template source.
       * This custom loader uses this hook to execute a webscript based UI component within
       * it's own specific WebScript runtme using the PageRenderer as the outer context.
       * The output of the UI component is buffered and a reader to it is returned to the
       * template engine. The stream is then output as the result of the <@region> directive.
       */
      public Reader getReader(Object templateSource, String encoding) throws IOException
      {
         String key = templateSource.toString();
         
         // lookup against resolved page component map
         PageRendererContext context = this.context.get();
         PageComponent component = context.PageInstance.getComponent(key);
         if (component == null)
         {
            if (this.ignoreMissingComponents)
            {
                return new StringReader("");
            }
            else
            {
               return new StringReader("ERROR: Failed to find component identified by key '" + key +
                     "' found in template: " + context.PageInstance.getTemplate());
            }
         }
         
         // NOTE: UI component URIs in page instance config files should not include /service prefix
         // Replace context/well known tokens in the component url
         String componentUrl = replaceContextTokens(component.getUrl(), context.Tokens);
         String webscript = componentUrl;
         if (webscript.lastIndexOf('?') != -1)
         {
            webscript = webscript.substring(0, webscript.lastIndexOf('?'));
         }
         
         // Execute the webscript and return a Reader to the textual content
         String executeUrl;
         if (component.getId().equals(context.ComponentId) == false)
         {
            executeUrl = context.RequestPath + componentUrl;
         }
         else
         {
            // else we found a clicked component url that was passed in on the servlet request
            executeUrl = context.ComponentUrl;
         }
         
         // Generate a runtime to execute the webscript ui component
         PageComponentWebScriptRuntime runtime = new PageComponentWebScriptRuntime(
               component, context, webscript, executeUrl, encoding);
         runtime.executeScript();
         
         // Return a reader from the runtime that executed the webscript - this effectively
         // returns the result as a "template" source to freemarker. In our case the template
         // result is the output from a webscript based UI component - it is not processed further
         // by the FreeMarker engine as the @region directive mandates this during template lookup.
         return runtime.getResponseReader();
      }
      
      /**
       * Setter to apply the current context for this template execution. A ThreadLocal wrapper is used
       * to allow multiple servlet threads to run using the same TemplateLoader (there can only be one)
       * but with a different context for each execution thread.
       */
      public void setContext(PageRendererContext context)
      {
         this.context.set(context);
      }
   }
   
   
   /**
    * Simple structure class representing the current thread request context for a page.
    * Holds thread local values to be used by the single instance of the custom Template Loader.
    */
   private static class PageRendererContext
   {
      PageInstance PageInstance;
      String RequestURI;
      String RequestPath;
      String ComponentId;
      String ComponentUrl;
      Map<String, String> Tokens;
   }
   
   
   /**
    * Helper to return context path for generating urls
    */
   public static class URLHelper
   {
      String context;
      String url;
      String args;

      public URLHelper(HttpServletRequest req)
      {
         this.context = req.getContextPath();
         this.url = req.getRequestURI();
         this.args = (req.getQueryString() != null ? req.getQueryString() : "");
      }

      public String getContext()
      {
         return context;
      }
      
      public String getFull()
      {
         return url;
      }
      
      public String getArgs()
      {
         return this.args;
      }
   }
   
   
   /**
    * Simple last modified timestamp based cache value wrapper
    */
   static class CacheValue<T>
   {
      public T Value;
      public long LastModified;
      
      CacheValue(T value, long lastModified)
      {
         this.Value = value;
         this.LastModified = lastModified;
      }
   }
}
