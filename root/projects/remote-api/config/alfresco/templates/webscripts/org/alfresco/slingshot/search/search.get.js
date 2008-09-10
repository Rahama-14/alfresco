/**
 * Search Component: search
 *
 * Inputs:
 *   optional: site = the site to search into.
 *   optional: container = the component the search in
 *
 * Outputs:
 *  data.items/data.error - object containing list of search results
 */

const DEFAULT_MAX_RESULTS = 100;
const SITES_SPACE_QNAME_PATH = "/app:company_home/st:sites/";

/**
 * Returns site data as returned to the user.
 * { shortName: siteId, title: title }
 * 
 * Caches the sites to avoid double-querying the repository
 */
var siteDataCache = [];
function getSiteData(siteId)
{
   if (siteDataCache[siteId] != undefined)
   {
      return siteDataCache[siteId];
   }
   var site = siteService.getSite(siteId);
   var data = {
      shortName : siteId,
      title : "unknown"
   };
   if (site != null)
   {
      data.title = site.title;
   }
   siteDataCache[siteId] = data;
   return data;
}

/**
 * Cache to not display twice the same element (e.g. if two comments of the
 * same blog post match the search criteria
 */
var processedCache = {};
function addToProcessed(category, key)
{
   var cat = processedCache[category];
   if (cat == undefined)
   {
      processedCache[category] = [];
      cat = processedCache[category];
   }
   cat.push(key);
}
function checkProcessed(category, key)
{
   var cat = processedCache[category];
   if (cat != undefined)
   {
      for (var x in cat)
      {
         if (cat[x] == key)
         {
            return true;
         }
      }
   }
   return false;
}

/**
 * Returns the name path for a space
 */
function getSpaceNamePath(siteId, containerId, space)
{
   // first find the container to which we are relative to
   var site = siteService.getSite(siteId);
   var container = site.getContainer(containerId);
   var folders = [];
   while (! space.nodeRef.equals(container.nodeRef))
   {
      folders.push(space.name);
      space = space.parent;
   }
   var path = "";
   for (var x = folders.length - 1; x >= 0; x--)
   {
      path += "/" + folders[x];
   }
   return path;
}

/**
 * Returns an item of the document library component.
 */
function getDocumentItem(siteId, containerId, restOfPath, node)
{
   // PENDING: how to handle comments? the document should
   //          be returned instead
   
   // check whether we already processed this document
   if (checkProcessed(siteId + containerId, "" + node.nodeRef.toString()))
   {
      return null;
   }
   addToProcessed(siteId + containerId, "" + node.nodeRef.toString());
    
   // check whether this is a folder or a file
   var item = null;
   if (node.isContainer)
   {
      item = {};
      item.site = getSiteData(siteId);
      item.container = containerId;
      item.nodeRef = node.nodeRef.toString();
      item.type = "folder";
      item.tags = (node.tags != null) ? node.tags : [];
      item.name = node.name;
      item.displayName = node.name; // PENDING: node.name.replace(message("coci_service.working_copy_label"), ''); // ${item.name?replace(workingCopyLabel, "")?html}",  
      item.browseUrl = containerId + "#path=" + encodeURIComponent(getSpaceNamePath(siteId, containerId, node));
   }
   else if (node.isDocument)
   {
      item = {};
      item.site = getSiteData(siteId);
      item.container = containerId;
      item.nodeRef = node.nodeRef.toString();
      item.type = "file";
      item.tags = (node.tags != null) ? node.tags : [];
      item.name = node.name;
      item.displayName = node.name; // PENDING: node.name.replace(message("coci_service.working_copy_label"), ''); // ${item.name?replace(workingCopyLabel, "")?html}",
      item.browseUrl = "document-details?nodeRef=" + node.nodeRef.toString();
   }
   
   return item;
}

function getBlogPostItem(siteId, containerId, restOfPath, node)
{
   // investigate the rest of the path. the first item is the blog post, ignore everything that follows
   // are replies or folders 
   var site = siteService.getSite(siteId);
   var container = site.getContainer(containerId);
   
   // find the direct child of the container
   // note: this only works for post which are direct children of the blog container
   var child = node;
   var parent = child.parent;
   while ((parent != null) && (! parent.nodeRef.equals(container.nodeRef)))
   {
      child = parent;
      parent = parent.parent;
   }
   
   // check whether we found the container
   if (parent == null)
   {
      return null;
   }
   
   // check whether we already added this blog post
   if (checkProcessed(siteId + containerId, "" + child.nodeRef.toString()))
   {
      return null;
   }
   addToProcessed(siteId + containerId, "" + child.nodeRef.toString());
       
   // child is our blog post
   var item = {};
   item.site = getSiteData(siteId);
   item.container = containerId;
   item.nodeRef = child.nodeRef.toString();
   item.type = "blogpost";
   item.tags = (child.tags != null) ? child.tags : [];
   item.name = child.name;
   item.displayName = child.properties["cm:title"];
   item.browseUrl = "blog-postview?container=" + containerId + "&postId=" + child.name;
   
   return item;
}

function getForumPostItem(siteId, containerId, restOfPath, node)
{
   // try to find the first fm:topic node, that's what we return as search result
   var topicNode = node;
   while ((topicNode != null) && (topicNode.type != "{http://www.alfresco.org/model/forum/1.0}topic"))
   {
      topicNode = topicNode.parent;
   }
   if (topicNode == null)
   {
      return null;
   }
   
   // make sure we haven't already added the post
   if (checkProcessed(siteId + containerId, "" + topicNode.nodeRef.toString()))
   {
      return null;
   }
   addToProcessed(siteId + containerId, "" + topicNode.nodeRef.toString());
   
   // find the first post, which contains the post title
   // PENDING: error prone
   var postNode = topicNode.childAssocs["cm:contains"][0];
   
   // child is our forum post
   var item = {};
   item.site = getSiteData(siteId);
   item.container = containerId;
   item.nodeRef = topicNode.nodeRef.toString();
   item.type = "topicpost";
   item.tags = (topicNode.tags != null) ? topicNode.tags : [];
   item.name = topicNode.name;
   item.displayName = postNode.properties["cm:title"];
   item.browseUrl = "discussions-topicview?container=" + containerId + "&topicId=" + topicNode.name;
   
   return item;
}

function getCalendarItem(siteId, containerId, restOfPath, node)
{
   // only process nodes of the correct type
   if (node.type != "{com.infoaxon.alfresco.calendar}calendarEvent")
   {
      return null;
   }
   
   // make sure we haven't already added the post
   if (checkProcessed(siteId + containerId, "" + node.nodeRef.toString()))
   {
      return null;
   }
   addToProcessed(siteId + containerId, "" + node.nodeRef.toString());
   
   var item = {};
   item.site = getSiteData(siteId);
   item.container = containerId;
   item.nodeRef = node.nodeRef.toString();
   item.type = "calendarevent";
   item.tags = (node.tags != null) ? node.tags : [];
   item.name = node.name;
   item.displayName = node.properties["ia:whatEvent"];
   item.browseUrl = containerId; // this is "calendar"
   
   return item;
}

function getWikiItem(siteId, containerId, restOfPath, node)
{
   // only process documents
   if (!node.isDocument)
   {
      return null;
   }
   
   // make sure we haven't already added the page
   if (checkProcessed(siteId + containerId, "" + node.nodeRef.toString()))
   {
      return null;
   }
   addToProcessed(siteId + containerId, "" + node.nodeRef.toString());
   
   var item = {};
   item.site = getSiteData(siteId);
   item.container = containerId;
   item.nodeRef = node.nodeRef.toString();
   item.type = "wikipage";
   item.tags = (node.tags != null) ? node.tags : [];
   item.name = node.name;
   item.displayName = node.properties["cm:name"]; // cm:title at some point?
   item.browseUrl = "wiki-page?title=" + node.properties["cm:name"];
   
   return item;
}

/**
 * Delegates the extraction to the correct extraction function
 * depending site/container id.
 */
function getItem(siteId, containerId, restOfPath, node)
{
   if (containerId == "documentLibrary")
   {
      return getDocumentItem(siteId, containerId, restOfPath, node);
   }
   else if (containerId == "blog")
   {
      return getBlogPostItem(siteId, containerId, restOfPath, node);
   }
   else if (containerId == "discussions")
   {
      return getForumPostItem(siteId, containerId, restOfPath, node);
   }
   else if (containerId == "calendar")
   {
      return getCalendarItem(siteId, containerId, restOfPath, node);
   }
   else if (containerId == "wiki")
   {
      return getWikiItem(siteId, containerId, restOfPath, node);
   }
   else
   {
      // unknown container
      return null;
   }
}

/**
 * Returns an array with [0] = site and [1] = container or null if the node does not match
 */
function splitQNamePath(node)
{
   var path = node.qnamePath;
   
   if (path.match("^"+SITES_SPACE_QNAME_PATH) != SITES_SPACE_QNAME_PATH)
   {
      return null;
   }
   
   var tmp = path.substring(SITES_SPACE_QNAME_PATH.length);
   var pos = tmp.indexOf('/');
   if (pos < 1)
   {
      return null;
   }
   
   var siteId = tmp.substring(0, pos);
   siteId = siteId.substring(siteId.indexOf(":") + 1);
   tmp = tmp.substring(pos + 1);
   pos = tmp.indexOf('/');
   if (pos < 1)
   {
      return null;
   }
   
   var containerId = tmp.substring(0, pos);
   containerId = containerId.substring(containerId.indexOf(":") + 1);
   var restOfPath = tmp.substring(pos + 1);
   
   return [ siteId, containerId, restOfPath ];
}

/**
 * Processes the search results. Filters out unnecessary nodes
 * 
 * @return the final search results object
 */
function processResults(nodes, maxResults)
{    
   var results = new Array(nodes.length);
   
   var added = 0;
   for (var x=0; x < nodes.length && added < maxResults; x++)
   {
      // for each node we extract the site/container qname path and then
      // let the per-container helper function decide what to do
      var parts = splitQNamePath(nodes[x]);
      if (parts != null)
      {
         var item = getItem(parts[0], parts[1], parts[2], nodes[x]);
         if (item != null)
         {
            results.push(item);
            added++;
         }
      }
   }
   
   return ({
         "items": results
   });
}

/**
 * Return Search results with the given search terms
 * Terms are split on whitespace characters.
 * 
 * AND, OR and NOT are supported keyboard as their Lucene equivilent.
 */
function getSearchResults(term, maxResults, siteId, containerId)
{
   var path = SITES_SPACE_QNAME_PATH; // "/app:company_home/st:sites/";
   if (siteId != null && siteId.length > 0)
   {
      path += "cm:" + search.ISO9075Encode(siteId) + "/";
   }
   else
   {
      path += "*/";
   }
   if (containerId != null && containerId.length > 0)
   {
      path += "cm:" + search.ISO9075Encode(containerId) + "/";
   }
   else
   {
      path += "*/";
   }
	
   var luceneQuery = ""
   if (term != null && term.length != 0)
   {
      // TODO: Perform smarter term processing. For now we simply split on whitespace
      //       which ignores quoted phrases that may be present.
      var terms = term.split(/\s/);
      
      for (var x=0; x < terms.length; x++)
      {
         var t = terms[x];
         // remove quotes - TODO: add support for quoted terms later
         t = t.replace(/\"/g, "");
         if (t.length != 0)
         {
            switch (t.toLowerCase())
            {
               case "and":
                  if (x < terms.length - 1 && terms[x + 1].length != 0)
                  {
                     luceneQuery += "AND ";
                  }
                  break;
               
               case "or":
                  break;
               
               case "not":
                  if (x < terms.length - 1 && terms[x + 1].length != 0)
                  {
                     luceneQuery += "NOT ";
                  }
                  break;
               
               default:
                  luceneQuery += "(TEXT:\"" + t + "\"" +        // full text
                                 " @cm\\:name:\"" + t + "\"" +  // name property
                                 " PATH:\"/cm:taggable/cm:" + search.ISO9075Encode(t) + "/member\"" + // tags
                                 ") ";
            }
         }
      }
   }
   
   var nodes;
   
   // if we processed the search terms, then suffix the PATH query
   if (luceneQuery.length != 0)
   {
      luceneQuery = "+PATH:\"" + path + "/*\" +(" + luceneQuery + ")";
      luceneQuery += " -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\"";
      nodes = search.luceneSearch(luceneQuery);
   }
   else
   {
      // failed to process the search string - empty list returned
      nodes = new Array();
   }
   
   return processResults(nodes, maxResults);
}


function main()
{
   var siteId = (args["site"] != undefined) ? args["site"] : null;
   var containerId = (args["container"] != undefined) ? args["container"] : null;
   var term = (args["term"] != undefined) ? args["term"] : null;
   var maxResults = (args["maxResults"] != undefined) ? parseInt(args["maxResults"]) : DEFAULT_MAX_RESULTS;
   
   model.data = getSearchResults(term, maxResults, siteId, containerId);
}

main();