<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/evaluator.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/filters.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/parse-args.lib.js">

/**
 * Main entry point: Return single document or folder given it's nodeRef
 *
 * @method getDoclist
 */
function getDoclist()
{
   // Use helper function to get the arguments
   var parsedArgs = ParseArgs.getParsedArgs();
   if (parsedArgs === null)
   {
      return;
   }

   var filter = args.filter,
      items = [];

   var favourites = Common.getFavourites(),
      node = parsedArgs.rootNode,
      parent =
      {
         node: node.parent,
         userAccess: Evaluator.run(node.parent, true).actionPermissions
      };

   var isThumbnailNameRegistered = thumbnailService.isThumbnailNameRegistered(THUMBNAIL_NAME),
      thumbnail = null,
      item = Evaluator.run(node);

   item.isFavourite = (favourites[node.nodeRef] === true);

   item.location =
   {
      site: parsedArgs.location.site,
      siteTitle: parsedArgs.location.siteTitle,
      container: parsedArgs.location.container,
      path: parsedArgs.location.path,
      file: node.name
   };
   
   // Special case for container and libraryRoot nodes
   if ((parsedArgs.location.containerNode && String(parsedArgs.location.containerNode.nodeRef) == String(node.nodeRef)) ||
      (parsedArgs.libraryRoot && String(parsedArgs.libraryRoot.nodeRef) == String(node.nodeRef)))
   {
      item.location.file = "";
   }
   else if (node.isContainer)
   {
      // Strip off the extra path that will have been added by default
      var paths = item.location.path.split("/");
      item.location.path = "/" + paths.slice(1, paths.length - 1).join("/");
   }
      
   // Is our thumbnail type registered?
   if (isThumbnailNameRegistered)
   {
      // Make sure we have a thumbnail.
      thumbnail = node.getThumbnail(THUMBNAIL_NAME);
      if (thumbnail === null)
      {
         // No thumbnail, so queue creation
         node.createThumbnail(THUMBNAIL_NAME, true);
      }
   }
      
   return (
   {
      parent: parent,
      onlineEditing: utils.moduleInstalled("org.alfresco.module.vti"),
      items: [item]
   });
}

/**
 * Document List Component: doclist
 */
model.doclist = getDoclist();