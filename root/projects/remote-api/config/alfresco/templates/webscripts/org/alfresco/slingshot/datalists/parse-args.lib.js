/**
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

var Common =
{
   /**
    * Cache for person objects
    */
   PeopleCache: {},

   /**
    * Gets / caches a person object
    *
    * @method getPerson
    * @param username {string} User name
    */
   getPerson: function Common_getPerson(username)
   {
      if (username == null || username == "")
      {
         return null;
      }

      if (typeof Common.PeopleCache[username] == "undefined")
      {
         var person = people.getPerson(username);
         if (person == null && (username == "System" || username.match("^System@") == "System@"))
         {
            person =
            {
               properties:
               {
                  userName: "System",
                  firstName: "System",
                  lastName: "User"
               },
               assocs: {}
            };
         }
         Common.PeopleCache[username] =
         {
            userName: person.properties.userName,
            firstName: person.properties.firstName,
            lastName: person.properties.lastName,
            displayName: (person.properties.firstName + " " + person.properties.lastName).replace(/^\s+|\s+$/g, "")
         };
         if (person.assocs["cm:avatar"] != null)
         {
            Common.PeopleCache[username].avatar = person.assocs["cm:avatar"][0];
         }
      }
      return Common.PeopleCache[username];
   }
};

var ParseArgs =
{
   /**
    * Get and parse arguments
    *
    * @method getParsedArgs
    * @param containerType {string} Optional: Node Type of container to create if it doesn't exist, defaults to "cm:folder"
    * @return {array|null} Array containing the validated input parameters
    */
   getParsedArgs: function ParseArgs_getParsedArgs(containerType)
   {
      var rootNode = null,
         nodeRef = null,
         listNode = null;

      if (url.templateArgs.store_type !== null)
      {
         /**
          * nodeRef input: store_type, store_id and id
          */
         var storeType = url.templateArgs.store_type,
            storeId = url.templateArgs.store_id,
            id = url.templateArgs.id;

         nodeRef = storeType + "://" + storeId + "/" + id;
         rootNode = ParseArgs.resolveVirtualNodeRef(nodeRef);
         if (rootNode == null)
         {
            rootNode = search.findNode(nodeRef);
            if (rootNode === null)
            {
               status.setCode(status.STATUS_NOT_FOUND, "Not a valid nodeRef: '" + nodeRef + "'");
               return null;
            }
         }
         
         listNode = rootNode;
      }
      else
      {
         /**
          * Site and container input
          */
         var siteId = url.templateArgs.site,
            containerId = url.templateArgs.container,
            listId = url.templateArgs.list,
            siteNode = siteService.getSite(siteId);

         if (siteNode === null)
         {
            status.setCode(status.STATUS_NOT_FOUND, "Site not found: '" + siteId + "'");
            return null;
         }

         rootNode = siteNode.getContainer(containerId);
         if (rootNode === null)
         {
            rootNode = siteNode.createContainer(containerId, containerType || "cm:folder");
            if (rootNode === null)
            {
               status.setCode(status.STATUS_NOT_FOUND, "Data Lists container '" + containerId + "' not found in '" + siteId + "'. (No permission?)");
               return null;
            }
            
            rootNode.properties["cm:description"] = "Data Lists";
            rootNode.save();
         }
         listNode = rootNode;
         
         if (listId !== null)
         {
            listNode = rootNode.childByNamePath(listId);
            if (listNode === null)
            {
               status.setCode(status.STATUS_NOT_FOUND, "List not found: '" + listId + "'");
               return null;
            }
         }
      }
      
      // Filter
      var filter = null;
      if (args.filter)
      {
         filter =
         {
            filterId: args.filter
         };
      }
      else if (typeof json !== "undefined" && json.has("filter"))
      {
         filter = jsonUtils.toObject(json.get("filter"));
         if (filter == null)
         {
            filter =
            {
               filterId: ""
            }
         }
      }

      var objRet =
      {
         rootNode: rootNode,
         listNode: listNode,
         nodeRef: String(listNode.nodeRef),
         filter: filter
      };

      return objRet;
   },

   /**
    * Resolve "virtual" nodeRefs into nodes
    *
    * @method resolveVirtualNodeRef
    * @param virtualNodeRef {string} nodeRef
    * @return {ScriptNode|null} Node corresponding to supplied virtual nodeRef. Returns null if supplied nodeRef isn't a "virtual" type
    */
   resolveVirtualNodeRef: function ParseArgs_resolveVirtualNodeRef(nodeRef)
   {
      var node = null;
      if (nodeRef == "alfresco://company/home")
      {
         node = companyhome;
      }
      else if (nodeRef == "alfresco://user/home")
      {
         node = userhome;
      }
      else if (nodeRef == "alfresco://sites/home")
      {
         node = companyhome.childrenByXPath("st:sites")[0];
      }
      return node;
   }
};
