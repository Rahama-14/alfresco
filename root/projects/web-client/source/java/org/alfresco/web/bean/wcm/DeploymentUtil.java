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
package org.alfresco.web.bean.wcm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.model.WCMAppModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ISO9075;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Helper methods for deployment
 * 
 * @author Gavin Cornwell
 */
public final class DeploymentUtil
{
   private static final Log logger = LogFactory.getLog(DeploymentUtil.class);
   
   /**
    * Returns all deployment attempts for the given store
    * 
    * @param store The store to get the deployment attempts for
    * @return List of NodeRef's representing the deployment attempts
    */
   public static List<NodeRef> findDeploymentAttempts(String store)
   {
      // return all deployment attempts
      return findDeploymentAttempts(store, null, null);
   }
   
   /**
    * Returns all deployment attempts for the given store
    * 
    * @param store The store to get the deployment attempts for
    * @param fromDate If present only attempts after the given date are returned
    * @param toDate If present only attempts before the given date are returned, if null
    *               toDate defaults to today's date
    * @return List of NodeRef's representing the deployment attempts
    */
   public static List<NodeRef> findDeploymentAttempts(String store, Date fromDate, Date toDate)
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      SearchService searchService = Repository.getServiceRegistry(fc).getSearchService();
      
      // query for all deploymentattempt nodes with the deploymentattemptstore
      // set to the given store id
      StringBuilder query = new StringBuilder("@");
      query.append(NamespaceService.WCMAPP_MODEL_PREFIX);
      query.append("\\:");
      query.append(WCMAppModel.PROP_DEPLOYATTEMPTSTORE.getLocalName());
      query.append(":\"");
      query.append(store);
      query.append("\"");
      
      // constrain the search by date if a fromDate is applied
      if (fromDate != null)
      {
         if (toDate == null)
         {
            toDate = new Date();
         }
         
         // see if the dates are the same (ignoring the time)
         boolean sameDate = false;
         Calendar fromCal = new GregorianCalendar();
         fromCal.setTime(fromDate);
         Calendar toCal = new GregorianCalendar();
         toCal.setTime(toDate);
         if ((fromCal.get(Calendar.YEAR) == toCal.get(Calendar.YEAR)) && 
             (fromCal.get(Calendar.MONTH) == toCal.get(Calendar.MONTH)) &&
             (fromCal.get(Calendar.DAY_OF_MONTH) == toCal.get(Calendar.DAY_OF_MONTH)))
         {
            sameDate = true;
         }
         
         // add date to query
         query.append(" AND @");
         query.append(NamespaceService.WCMAPP_MODEL_PREFIX);
         query.append("\\:");
         query.append(WCMAppModel.PROP_DEPLOYATTEMPTTIME.getLocalName());
         query.append(":");
         
         if (sameDate)
         {
            // convert date into format needed for range query
            String queryDate = formatLuceneQueryDate(fromDate, false);
            
            // query for exact date
            query.append("\"");
            query.append(queryDate);
            query.append("\"");
         }
         else
         {
            // convert to date into format needed for range query
            String queryFromDate = formatLuceneQueryDate(fromDate, true);
            String queryToDate = formatLuceneQueryDate(toDate, true);
            
            // create a date range query
            query.append("[");
            query.append(queryFromDate);
            query.append(" TO ");
            query.append(queryToDate);
            query.append("]");
         }
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Finding deploymentattempt nodes using query: " + query.toString());
      
      ResultSet results = null;
      List<NodeRef> attempts = new ArrayList<NodeRef>();
      try
      {
         // sort the results by deploymentattempttime
         SearchParameters sp = new SearchParameters();
         sp.addStore(Repository.getStoreRef());
         sp.setLanguage(SearchService.LANGUAGE_LUCENE);
         sp.setQuery(query.toString());
         sp.addSort("@" + WCMAppModel.PROP_DEPLOYATTEMPTTIME, false);
         
         // execute the query
         results = searchService.query(sp);
         
         if (logger.isDebugEnabled())
            logger.debug("Found " + results.length() + " deployment attempts");
         
         for (NodeRef attempt : results.getNodeRefs())
         {
            attempts.add(attempt);
         }
      }
      finally
      {
         if (results != null)
         {
            results.close();
         }
      }
      
      return attempts;
   }
   
   /**
    * Retrieves the NodeRef of the deploymentattempt node with the given id
    * 
    * @param attemptId The deployattemptid of the node to be found
    * @return The NodeRef of the deploymentattempt node or null if not found
    */
   public static NodeRef findDeploymentAttempt(String attemptId)
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      SearchService searchService = Repository.getServiceRegistry(fc).getSearchService();
      
      // construct the query
      StringBuilder query = new StringBuilder("@");
      query.append(NamespaceService.WCMAPP_MODEL_PREFIX);
      query.append("\\:");
      query.append(WCMAppModel.PROP_DEPLOYATTEMPTID.getLocalName());
      query.append(":\"");
      query.append(attemptId);
      query.append("\"");
      
      ResultSet results = null;
      NodeRef attempt = null;
      try
      {
         // execute the query
         results = searchService.query(Repository.getStoreRef(), 
               SearchService.LANGUAGE_LUCENE, query.toString());
         
         if (results.length() == 1)
         {
            attempt = results.getNodeRef(0);
         }
         else if (results.length() > 1)
         {
            throw new IllegalStateException(
               "More than one deployment attempt node was found, there should only be one!");
         }
      }
      finally
      {
         if (results != null)
         {
            results.close();
         }
      }
      
      return attempt;
   }
   
   /**
    * Returns the test server allocated to the given store.
    * 
    * @param store The store to get the test server for
    * @return The allocated server or null if there isn't one
    */
   public static NodeRef findAllocatedTestServer(String store)
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      SearchService searchService = Repository.getServiceRegistry(fc).getSearchService();
      
      // construct the query
      StringBuilder query = new StringBuilder("@");
      query.append(NamespaceService.WCMAPP_MODEL_PREFIX);
      query.append("\\:");
      query.append(WCMAppModel.PROP_DEPLOYSERVERALLOCATEDTO.getLocalName());
      query.append(":\"");
      query.append(store);
      query.append("\"");
      
      ResultSet results = null;
      NodeRef testServer = null;
      try
      {
         // execute the query
         results = searchService.query(Repository.getStoreRef(), 
               SearchService.LANGUAGE_LUCENE, query.toString());
         
         if (results.length() == 1)
         {
            testServer = results.getNodeRef(0);
         }
         else if (results.length() > 1)
         {
            // get the first one and warn that we found many!
            testServer = results.getNodeRef(0);
            
            if (logger.isWarnEnabled())
               logger.warn("More than one allocated test server for store '" +
                     store + "' was found, should only be one, first one found returned!");
         }
      }
      finally
      {
         if (results != null)
         {
            results.close();
         }
      }
      
      return testServer;
   }
   
   /**
    * Returns a list of NodeRefs representing the 'live' servers configured
    * for the given web project.
    *  
    * @param webProject Web project to get live servers for 
    * @return List of live servers
    */
   public static List<NodeRef> findLiveServers(NodeRef webProject)
   {
      return findServers(webProject, true, false);
   }
   
   /**
    * Returns a list of NodeRefs representing the 'test' servers configured
    * for the given web project.
    *  
    * @param webProject Web project to get test servers for 
    * @param availableOnly if true only returns those servers still available for deployment 
    * @return List of test servers
    */
   public static List<NodeRef> findTestServers(NodeRef webProject, boolean availableOnly)
   {
      return findServers(webProject, false, availableOnly);
   }
   
   private static List<NodeRef> findServers(NodeRef webProject, boolean live, boolean availableOnly)
   {
      FacesContext context = FacesContext.getCurrentInstance();
      NodeService nodeService = Repository.getServiceRegistry(context).getNodeService();
      SearchService searchService = Repository.getServiceRegistry(context).getSearchService();
      
      // build the query
      String webProjectName = (String)nodeService.getProperty(webProject, ContentModel.PROP_NAME);
      String safeProjectName = ISO9075.encode(webProjectName); 
      StringBuilder query = new StringBuilder("PATH:\"/");
      query.append(Application.getRootPath(context));
      query.append("/");
      query.append(Application.getWebsitesFolderName(context));
      query.append("/cm:");
      query.append(safeProjectName);
      query.append("/*\" AND @");
      query.append(NamespaceService.WCMAPP_MODEL_PREFIX);
      query.append("\\:");
      query.append(WCMAppModel.PROP_DEPLOYSERVERTYPE.getLocalName());
      query.append(":\"");
      if (live)
      {
         query.append(WCMAppModel.CONSTRAINT_LIVESERVER);
      }
      else
      {
         query.append(WCMAppModel.CONSTRAINT_TESTSERVER);
      }      
      query.append("\"");
      
      // if required filter the test servers
      if (live == false && availableOnly)
      {
         query.append(" AND ISNULL:\"");
         query.append(WCMAppModel.PROP_DEPLOYSERVERALLOCATEDTO.toString());
         query.append("\"");
      }
      
      if (logger.isDebugEnabled())
         logger.debug("Finding deployment servers using query: " + query.toString());
      
      // execute the query
      ResultSet results = null;
      List<NodeRef> servers = new ArrayList<NodeRef>();
      try
      {
         results = searchService.query(Repository.getStoreRef(), 
               SearchService.LANGUAGE_LUCENE, query.toString());
         
         if (logger.isDebugEnabled())
            logger.debug("Found " + results.length() + " deployment servers");
         
         for (NodeRef server : results.getNodeRefs())
         {
            servers.add(server);
         }
      }
      finally
      {
         if (results != null)
         {
            results.close();
         }
      }
      
      return servers;
   }
   
   private static String formatLuceneQueryDate(Date date, boolean range)
   {
      Calendar cal = new GregorianCalendar();
      cal.setTime(date);
      
      StringBuilder queryDate = new StringBuilder();
      queryDate.append(cal.get(Calendar.YEAR));
      if (range)
      {
         queryDate.append("\\");
      }
      queryDate.append("-");
      queryDate.append((cal.get(Calendar.MONTH)+1));
      if (range)
      {
         queryDate.append("\\");
      }
      queryDate.append("-");
      queryDate.append(cal.get(Calendar.DAY_OF_MONTH));
      queryDate.append("T00:00:00");
      
      return queryDate.toString();
   }
}
