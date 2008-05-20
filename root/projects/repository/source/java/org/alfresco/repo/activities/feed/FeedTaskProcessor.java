/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
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
package org.alfresco.repo.activities.feed;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.activities.feed.control.FeedControlDAO;
import org.alfresco.repo.activities.post.ActivityPostDAO;
import org.alfresco.repo.template.ISO8601DateFormatMethod;
import org.alfresco.util.Base64;
import org.alfresco.util.JSONtoFmModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Responsible for processing the individual task
 */
public abstract class FeedTaskProcessor
{
    private static final Log logger = LogFactory.getLog(FeedTaskProcessor.class);
    
    private static final String defaultFormat = "text";
    private static final String[] formats = {"atomentry", "rss", "json", "html", "xml", defaultFormat};
    
    private static final String URL_SERVICE_SITES     = "/api/sites";
    private static final String URL_MEMBERSHIPS       = "/memberships";
    
    private static final String URL_SERVICE_TEMPLATES = "/api/activities/templates";
    private static final String URL_SERVICE_TEMPLATE  = "/api/activities/template";
    
  
    public void process(int jobTaskNode, long minSeq, long maxSeq, RepoCtx ctx) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(">>> Process: jobTaskNode '" + jobTaskNode + "' from seq '" + minSeq + "' to seq '" + maxSeq + "' on this node from grid job.");
        }
        
        ActivityPostDAO selector = new ActivityPostDAO();
        selector.setJobTaskNode(jobTaskNode);
        selector.setMinId(minSeq);
        selector.setMaxId(maxSeq);
        selector.setStatus(ActivityPostDAO.STATUS.POSTED.toString());
        
        String ticket = ctx.getTicket();
        
        List<ActivityPostDAO> activityPosts = null;
        int totalGenerated = 0;
        
        try
        {
            activityPosts = selectPosts(selector);
            
            if (logger.isDebugEnabled()) { logger.debug(">>> Process: " + activityPosts.size() + " activity posts"); }
            
            Configuration cfg = getFreemarkerConfiguration(ctx);
            
            Map<String, List<String>> activityTemplates = new HashMap<String, List<String>>(10);
                
            // for each activity post ...
            for (ActivityPostDAO activityPost : activityPosts)
            {
                String postingUserId = activityPost.getUserId();
                String activityType = activityPost.getActivityType();
                
                // eg. org.alfresco.folder.added -> added
                String baseActivityType = getBaseActivityType(activityType);
                
                List<String> fmTemplates = activityTemplates.get(baseActivityType);
                
                if (fmTemplates == null)
                {
                    // eg. org.alfresco.folder.added -> /org/alfresco/folder/added (note: the leading slash)
                    String templateSubPath = getTemplateSubPath(activityType);
                    
                    fmTemplates = new ArrayList<String>(0);
                    while (true)
                    {
                        int idx = templateSubPath.lastIndexOf("/");
                        if (idx != -1)
                        {
                            templateSubPath = templateSubPath.substring(0, idx);
                            Map<String, List<String>> templates = null;
                            try
                            {
                                // Repository callback to get list of FreeMarker templates for given activity type
                                templates = getActivityTypeTemplates(ctx.getRepoEndPoint(), ticket, templateSubPath+"/");
                            }
                            catch (FileNotFoundException fnfe)
                            {
                                // ignore - path does not exist
                            }
                            if (templates != null)
                            {
                                if (templates.get(baseActivityType) != null)
                                {
                                    // add templates, if format not already included
                                    addMissingFormats(activityType, fmTemplates, templates.get(baseActivityType));
                                }
                                
                                // special fallback case
                                if (templates.get("generic") != null)
                                {
                                    // add templates, if format not already included
                                    addMissingFormats(activityType, fmTemplates, templates.get("generic"));
                                }
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                    
                    activityTemplates.put(baseActivityType, fmTemplates);
                }
                                   
                if (fmTemplates.size() == 0)
                {
                    logger.error(">>> Skipping activity post " + activityPost.getId() + " since no specific/generic templates for activityType: " + activityType );
                    updatePostStatus(activityPost.getId(), ActivityPostDAO.STATUS.ERROR);
                    continue;
                }
                
                Map<String, Object> model = null;
                try
                {
                    model = JSONtoFmModel.convertJSONObjectToMap(activityPost.getActivityData());
                }
                catch(JSONException je)
                {
                    logger.error(">>> Skipping activity post " + activityPost.getId() + " due to invalid activity data: " + je);
                    updatePostStatus(activityPost.getId(), ActivityPostDAO.STATUS.ERROR);
                    continue;
                }
                
                model.put("activityType", activityPost.getActivityType());
                model.put("siteNetwork", activityPost.getSiteNetwork());
                model.put("userId", activityPost.getUserId());
                model.put("id", activityPost.getId());
                model.put("date", activityPost.getPostDate()); // post date rather than time that feed is generated
                model.put("xmldate", new ISO8601DateFormatMethod());
                model.put("repoEndPoint", ctx.getRepoEndPoint());
                
                Set<String> connectedUsers = null;
                if ((activityPost.getSiteNetwork() == null) || (activityPost.getSiteNetwork().length() == 0))
                {
                    connectedUsers = new HashSet<String>(1);
                }
                else
                {
                    try
                    {
                        // Repository callback to get site members
                        connectedUsers = getSiteMembers(ctx, activityPost.getSiteNetwork());
                    }
                    catch(Exception e)
                    {
                        logger.error(">>> Skipping activity post " + activityPost.getId() + " since failed to get site members: " + e);
                        updatePostStatus(activityPost.getId(), ActivityPostDAO.STATUS.ERROR);
                        continue;
                    }
                }
                
                connectedUsers.add(""); // add empty posting userid - to represent site feed !
                
                try 
                { 
                    startTransaction();
                    
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(">>> Process: " + connectedUsers.size() + " candidate connections for activity post " + activityPost.getId());
                    }
                    
                    int excludedConnections = 0;
                    
                    for (String connectedUser : connectedUsers)
                    {
                        List<FeedControlDAO> feedControls = null;
                        if (! connectedUser.equals(""))
                        {
                            feedControls = getFeedControls(connectedUser);
                        }
                        
                        // filter based on opt-out feed controls (if any)
                     	if (! acceptActivity(activityPost, feedControls))
                     	{
                     	    excludedConnections++;
                     	}
                     	else
                    	{ 
                            for (String fmTemplate : fmTemplates)
                            {
                                // determine format - based on template naming convention   
                                String formatFound = null;
                                for (String format : formats)
                                {
                                    if (fmTemplate.contains("."+format+"."))
                                    {
                                        formatFound = format;
                                        break;
                                    }
                                }    
                                
                                if (formatFound == null)
                                {
                                    formatFound = defaultFormat;
                                    logger.warn("Unknown format for: " + fmTemplate + " default to '"+formatFound+"'");
                                }
                                
        	                    ActivityFeedDAO feed = new ActivityFeedDAO();
        	                    
        	                    // Generate activity feed summary 
        	                    feed.setFeedUserId(connectedUser);
        	                    feed.setPostUserId(postingUserId);
        	                    feed.setActivityType(activityType);
        	                    
        	                    if (formatFound.equals("json"))
        	                    {
        	                        // allows generic JSON template to simply pass straight through
        	                        model.put("activityData", activityPost.getActivityData());
        	                    }
        	                    
        	                    String activitySummary = processFreemarker(fmTemplate, cfg, model);   
        	                    if (! activitySummary.equals(""))
        	                    {
            	                    feed.setActivitySummary(activitySummary);
            	                    feed.setActivitySummaryFormat(formatFound);
            	                    feed.setSiteNetwork(activityPost.getSiteNetwork());
            	                    feed.setAppTool(activityPost.getAppTool());
            	                    feed.setPostDate(activityPost.getPostDate());
            	                    feed.setPostId(activityPost.getId());
            	                    feed.setFeedDate(new Date());
 
            	                    // Insert activity feed
            	                    insertFeedEntry(feed); // ignore returned feedId
            	                    
            	                    totalGenerated++;
        	                    }
        	                    else
        	                    {
        	                        if (logger.isDebugEnabled())
                                    {
                                        logger.debug("Empty template result for activityType '" + activityType + "' using format '" + formatFound + "' hence skip feed entry (activity post " + activityPost.getId() + ")");
                                    }
        	                    }
                            }
                    	}
                    }
                    
                    updatePostStatus(activityPost.getId(), ActivityPostDAO.STATUS.PROCESSED);

                    commitTransaction();
                    
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(">>> Processed: " + (connectedUsers.size() - excludedConnections) + " connections for activity post " + activityPost.getId() + " (excluded " + excludedConnections + ")");
                    }
                } 
                finally 
                { 
                    endTransaction();
                } 
            }
        }
        catch(SQLException se)
        {
            logger.error(se);
            throw se;
        }
        finally
        {
            logger.info(">>> Generated " + totalGenerated + " activity feed entries for " + (activityPosts == null ? 0 : activityPosts.size()) + " activity posts");
        }
    }
    
    public abstract void startTransaction() throws SQLException;
    
    public abstract void commitTransaction() throws SQLException;
    
    public abstract void endTransaction() throws SQLException;
    
    public abstract List<ActivityPostDAO> selectPosts(ActivityPostDAO selector) throws SQLException;
    
    public abstract List<FeedControlDAO> selectUserFeedControls(String userId) throws SQLException;
    
    public abstract long insertFeedEntry(ActivityFeedDAO feed) throws SQLException;
    
    public abstract int updatePostStatus(long id, ActivityPostDAO.STATUS status) throws SQLException;
    
    
    protected String callWebScript(String urlString, String ticket) throws MalformedURLException, URISyntaxException, IOException
    {
    	URL url = new URL(urlString);
    	
    	if (logger.isDebugEnabled())
    	{
    	    logger.debug(">>> Request URI: " + url.toURI());
    	}
        
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");

        if (ticket != null)
        {
        	// add Base64 encoded authorization header
            // refer to: http://wiki.alfresco.com/wiki/Web_Scripts_Framework#HTTP_Basic_Authentication
        	conn.addRequestProperty("Authorization", "Basic " + Base64.encodeBytes(ticket.getBytes()));
        }
        
        String result = null;
        InputStream is = null;
        BufferedReader br = null;
        
        try
        {
	        is = conn.getInputStream();
	        br = new BufferedReader(new InputStreamReader(is));
	
	        String line = null;
	        StringBuffer sb = new StringBuffer();
	        while(((line = br.readLine()) !=null))  {
	        	sb.append(line);
	        }
	        
	        result = sb.toString();
	        
	        if (logger.isDebugEnabled())
	        {
	            int responseCode = conn.getResponseCode();
	            logger.debug(">>> Response code: " + responseCode);
	        }
        }
        finally
        {
	        if (br != null) { br.close(); };
        	if (is != null) { is.close(); };
        }

        return result;
    }

    protected Set<String> getSiteMembers(RepoCtx ctx, String siteId) throws Exception
    {   
        Set<String> members = new HashSet<String>();
        if ((siteId != null) && (siteId.length() != 0))
        {
            StringBuffer sbUrl = new StringBuffer();
            sbUrl.append(ctx.getRepoEndPoint()).
                  append(URL_SERVICE_SITES).append("/").append(siteId).append(URL_MEMBERSHIPS);
        
            String jsonArrayResult = callWebScript(sbUrl.toString(), ctx.getTicket());
            if ((jsonArrayResult != null) && (jsonArrayResult.length() != 0))
            {
                JSONArray ja = new JSONArray(jsonArrayResult);
                for (int i = 0; i < ja.length(); i++)
                {
                    JSONObject member = (JSONObject)ja.get(i);
                    JSONObject person = (JSONObject)member.getJSONObject("person");
                    
                    String userName = person.getString("userName");
                    if (! ctx.isUserNamesAreCaseSensitive())
                    {
                        userName = userName.toLowerCase();
                    }
                    members.add(person.getString("userName"));
                }
            }
        }
        
        return members;
    }
    
    protected Map<String, List<String>> getActivityTypeTemplates(String repoEndPoint, String ticket, String subPath) throws Exception
    {
        StringBuffer sbUrl = new StringBuffer();
        sbUrl.append(repoEndPoint).append(URL_SERVICE_TEMPLATES).append("?p=").append(subPath).append("*").append("&format=json");
        
        String jsonArrayResult = null;
        try
        {
            jsonArrayResult = callWebScript(sbUrl.toString(), ticket);
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
        
        List<String> allTemplateNames = new ArrayList<String>(10);
        
        if ((jsonArrayResult != null) && (jsonArrayResult.length() != 0))
        {
            JSONArray ja = new JSONArray(jsonArrayResult);
            for (int i = 0; i < ja.length(); i++)
            {
                String name = ja.getString(i);
                if (! name.contains(" (Working Copy)."))
                {
                    allTemplateNames.add(name);
                }
            }
        }
        
        Map<String, List<String>> activityTemplates = new HashMap<String, List<String>>(10);
        
        for (String template : allTemplateNames)
        {
            // assume template path = <path>/<base-activityType>.<format>.ftl
            // and base-activityType can contain "."
            
            String baseActivityType = template;
            int idx1 = baseActivityType.lastIndexOf("/");
            if (idx1 != -1)
            {
                baseActivityType = baseActivityType.substring(idx1+1);
            }
            
            int idx2 = baseActivityType.lastIndexOf(".");
            if (idx2 != -1)
            {
                int idx3 = baseActivityType.substring(0, idx2).lastIndexOf(".");
                if (idx3 != -1)
                {
                    baseActivityType = baseActivityType.substring(0, idx3);
                    
                    List<String> activityTypeTemplateList = activityTemplates.get(baseActivityType);
                    if (activityTypeTemplateList == null)
                    {
                        activityTypeTemplateList = new ArrayList<String>(1);
                        activityTemplates.put(baseActivityType, activityTypeTemplateList);
                    }
                    activityTypeTemplateList.add(template);
                }
            }
        }
        
        return activityTemplates;
    }

    protected Configuration getFreemarkerConfiguration(RepoCtx ctx)
    {	
    	Configuration cfg = new Configuration();
        cfg.setObjectWrapper(new DefaultObjectWrapper());

        // custom template loader
	    cfg.setTemplateLoader(new TemplateWebScriptLoader(ctx.getRepoEndPoint(), ctx.getTicket()));

	    // TODO review i18n
	    cfg.setLocalizedLookup(false);
	    
        return cfg;   
    }
    
    protected String processFreemarker(String fmTemplate, Configuration cfg, Map<String, Object> model) throws IOException, TemplateException, Exception
    {
        Template myTemplate = cfg.getTemplate(fmTemplate);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Writer out = new OutputStreamWriter(bos);
        myTemplate.process(model, out);
        out.flush();
        
        return new String(bos.toByteArray());
    }
    
    protected List<FeedControlDAO> getFeedControls(String connectedUser) throws SQLException
    {
        // TODO cache for this run
        return selectUserFeedControls(connectedUser);
    }
    
    protected boolean acceptActivity(ActivityPostDAO activityPost, List<FeedControlDAO> feedControls)
    {
        if (feedControls == null)
        {
            return true;
        }
        
        for (FeedControlDAO feedControl : feedControls)
        {
            if (((feedControl.getSiteNetwork() == null) || (feedControl.getSiteNetwork().length() == 0)) && (feedControl.getAppTool() != null))
            {
                if (feedControl.getAppTool().equals(activityPost.getAppTool()))
                {
                    // exclude this appTool (across sites)
                    return false;
                }
            }
            else if (((feedControl.getAppTool() == null) || (feedControl.getAppTool().length() == 0)) && (feedControl.getSiteNetwork() != null))
            {
                if (feedControl.getSiteNetwork().equals(activityPost.getSiteNetwork()))
                {
                    // exclude this site (across appTools)
                    return false;
                }
            }
            else if (((feedControl.getSiteNetwork() != null) && (feedControl.getSiteNetwork().length() > 0)) &&
                     ((feedControl.getAppTool() != null) && (feedControl.getAppTool().length() > 0)))
            {
                if ((feedControl.getSiteNetwork().equals(activityPost.getSiteNetwork())) &&
                    (feedControl.getAppTool().equals(activityPost.getAppTool())))
                {
                    // exclude this appTool for this site
                    return false;
                }
            }
        }

        return true;
    }
    
    protected void addMissingFormats(String activityType, List<String> fmTemplates, List<String> templatesToAdd)
    {
        for (String templateToAdd : templatesToAdd)
        {
            int idx1 = templateToAdd.lastIndexOf(".");
            if (idx1 != -1)
            {
                int idx2 = templateToAdd.substring(0, idx1).lastIndexOf(".");
                if (idx2 != -1)
                {
                    String templateFormat = templateToAdd.substring(idx2+1, idx1);
                    
                    boolean found = false;
                    for (String fmTemplate : fmTemplates)
                    {
                        if (fmTemplate.contains("."+templateFormat+"."))
                        {
                            found = true;
                        }
                    }
                    
                    if (! found)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug(">>> Add template '" + templateToAdd + "' for type '" + activityType + "'");
                        }
                        fmTemplates.add(templateToAdd);
                    }
                }
            }
        }
    }
    
    protected String getTemplateSubPath(String activityType)
    {
        return (! activityType.startsWith("/") ? "/" : "") + activityType.replace(".", "/");
    }
    
    protected String getBaseActivityType(String activityType)
    {
        String[] parts = activityType.split("\\.");
        
        return (parts.length != 0 ? parts[parts.length-1] : "");
    }
    
    protected class TemplateWebScriptLoader extends URLTemplateLoader
    {
    	private String repoEndPoint;
    	private String ticketId;
    	
    	public TemplateWebScriptLoader(String repoEndPoint, String ticketId)
    	{
    		this.repoEndPoint = repoEndPoint;
    		this.ticketId = ticketId;
    	}
    	
    	public URL getURL(String templatePath)
    	{
    		try
    		{
    			StringBuffer sb = new StringBuffer();
    			sb.append(this.repoEndPoint).
    			   append(URL_SERVICE_TEMPLATE).append("?p=").append(templatePath).
    			   append("&format=text").
    		       append("&alf_ticket=").append(ticketId);
    			
    			if (logger.isDebugEnabled())
                {
                    logger.debug(">>> getURL: " + sb.toString());
                }
    			
    			return new URL(sb.toString());
    		} 
    		catch (Exception e)
    		{
    			throw new RuntimeException(e);
    		}
    	}
    }
}
