<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/parse-args.lib.js">

/**
 * Document List Component: treenode
 */
model.treenode = getTreenode();

/* Create collection of folders in the given space */
function getTreenode(siteId, path)
{
   try
   {
      var items = [],
         parsedArgs = ParseArgs.getParsedArgs(),
         skipPermissionCheck = args["perms"] == "false",
         item, node, rmNode, capabilities, cap;
   
      // Use helper function to get the arguments
      if (parsedArgs === null)
      {
         return;
      }

      // Quick version if "skipPermissionCheck" flag set
      if (skipPermissionCheck)
      {
         for each(node in parsedArgs.parentNode.children)
         {
            if (itemIsAllowed(node))
            {
               items.push(
               {
                  node: node
               });
            }
         }
      }
      else
      {
         for each(node in parsedArgs.parentNode.children)
         {
            if (itemIsAllowed(node))
            {
               capabilities = {};
               rmNode = rmService.getRecordsManagementNode(node);
               for each (cap in rmNode.capabilities)
               {
                  capabilities[cap.name] = true;
               }

               items.push(
               {
                  node: node,
                  permissions:
                  {
                     create: capabilities["Create"]
                  }
               });
            }
         }
      }
   
      items.sort(sortByName);
   
      return (
      {
         "items": items
      });
   }
   catch(e)
   {
      status.setCode(status.STATUS_INTERNAL_SERVER_ERROR, e.toString());
      return;
   }
}


/* Sort the results by case-insensitive name */
function sortByName(a, b)
{
   return (b.node.name.toLowerCase() > a.node.name.toLowerCase() ? -1 : 1);
}

/* Filter allowed types, etc. */
function itemIsAllowed(item)
{
   if (!item.isSubType("cm:folder"))
   {
      return false;
   }
   
   if (item.typeShort == "rma:hold" || item.typeShort == "rma:transfer")
   {
      return false;
   }
   
   return true;
}