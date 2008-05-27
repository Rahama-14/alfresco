/**
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
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * DocumentList TreeView component.
 * 
 * @namespace Alfresco
 * @class Alfresco.DocListTree
 */
(function()
{
   /**
    * DocumentList TreeView constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.DocListTree} The new DocListTree instance
    * @constructor
    */
   Alfresco.DocListTree = function DLT_constructor(htmlId)
   {
      this.name = "Alfresco.DocListTree";
      this.id = htmlId;
      
      // Register this component
      Alfresco.util.ComponentManager.register(this);
      
      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require(["treeview"], this.onComponentsLoaded, this);
      
      // Decoupled event listeners
      YAHOO.Bubbling.on("onDoclistPathChanged", this.onDoclistPathChanged, this);

      return this;
   }
   
   Alfresco.DocListTree.prototype =
   {
      /**
       * Object container for initialization options
       */
      options:
      {
         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: ""
      },
      
      /**
       * Flag set after TreeView instantiated.
       * 
       * @property isReady
       * @type boolean
       */
      isReady: false,

      /**
       * Initial path on page load.
       * 
       * @property initialPath
       * @type string
       */
      initialPath: null,

      /**
       * The YUI TreeView widget.
       * 
       * @property treeview
       * @type object
       */
      treeview: null,

      /**
       * Paths we have to expand as a result of a deep navigation event.
       * 
       * @property pathsToExpand
       * @type array
       */
      pathsToExpand: [],

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       */
      setOptions: function DLT_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
      },
      
      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function DLT_onComponentsLoaded()
      {
         // Register the onReady callback when the component parent element has been loaded
         YAHOO.util.Event.onContentReady(this.id, this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting
       * @method onReady
       */
      onReady: function DLT_onReady()
      {
         // Reference to self - used in inline functions
         var me = this;
         
         /**
          * Dynamically loads TreeView nodes.
          * This MUST be inline in order to have access to the Alfresco.DocListTree class.
          * @method loadNodeData
          * @param node {object} Parent node
          * @param fnLoadComplete {function} Expanding node's callback function
          */
         this.fnLoadNodeData = function DLT_loadNodeData(node, fnLoadComplete)
         {
            // Get the path this node refers to
            var nodePath = node.data.path;

            // Prepare URI for XHR data request
            var uri = Alfresco.constants.PROXY_URI + "slingshot/doclib/treenode?site=" + encodeURIComponent(me.options.siteId) + "&path=" + encodeURIComponent(nodePath);

            // Prepare the XHR callback object
            var callback =
            {
               success: function DLT_lND_success(oResponse)
               {
                  var results = eval("(" + oResponse.responseText + ")");

                  if (results.treenode.items)
                  {
                     for (var i = 0, j = results.treenode.items.length; i < j; i++)
                     {
                        var item = results.treenode.items[i];
                        var tempNode = new YAHOO.widget.TextNode(
                        {
                           label: item.name,
                           path: nodePath + "/" + item.name,
                           nodeRef: item.nodeRef,
                           description: item.description
                        }, node, false);

                        if (!item.hasChildren)
                        {
                           tempNode.isLeaf = true;
                        }
                     }
                  }

                  /**
                  * Execute the node's loadComplete callback method which comes in via the argument
                  * in the response object
                  */
                  oResponse.argument.fnLoadComplete();
               },

               // If the XHR call is not successful, fire the TreeView callback anyway
               failure: function DLT_lND_failure(oResponse)
               {
                  oResponse.argument.fnLoadComplete();
               },

               // XHR response argument information
               argument:
               {
                  "node": node,
                  "fnLoadComplete": fnLoadComplete
               },

               // Timeout -- abort the transaction after 7 seconds
               timeout: 7000
            };

            // Make the XHR call using Connection Manager's asyncRequest method
            YAHOO.util.Connect.asyncRequest('GET', uri, callback);
         };

         // Build the TreeView widget
         this._buildTree();
         
         this.isReady = true;
         if (this.initialPath !== null)
         {
            // We missed the onDoclistPathChanged event, so fake it here
            this.onDoclistPathChanged("onDoclistPathChanged",
            [
               null,
               {
                  path: this.initialPath
               }
            ]);
         }
      },

      /**
       * Fired when the path has changed
       * @method onDoclistPathChanged
       * @param layer {string} the event source
       * @param args {object} arguments object
       */
      onDoclistPathChanged: function DLT_onDoclistPathChanged(layer, args)
      {
         var obj = args[1];
         if ((obj !== null) && (obj.path !== null))
         {
            // Defer if event received before we're ready
            if (!this.isReady)
            {
               this.initialPath = obj.path;
               return;
            }
            
            // Search the tree to see if this path's node is expanded
            var node = this.treeview.getNodeByProperty("path", obj.path);
            if (node !== null)
            {
               // Node found
               node.expand();
               while (node.parent !== null)
               {
                  node = node.parent;
                  node.expand();
               }
               return;
            }
            
            /**
             * The path's node hasn't been loaded into the tree. Create a stack
             * of parent paths that we need to expand one-by-one in order to
             * eventually display the current path's node
             */
            var paths = obj.path.split("/");
            var expandPath = "";
            for (var i = 0; i < paths.length; i++)
            {
               if (paths[i] != "")
               {
                  // Push the path onto the list of paths to be expanded
                  expandPath += "/" + paths[i];
                  this.pathsToExpand.push(expandPath);
               }
            }
            
            node = this.treeview.getNodeByProperty("path", "");
            if (node !== null)
            {
               node.expand();
            }
         }
      },
      
      /**
       * Fired by YUI TreeView when a node has finished expanding
       * @method onExpandComplete
       * @param oNode {YAHOO.widget.Node} the node recently expanded
       */
      onExpandComplete: function DLT_onExpandComplete(oNode)
      {
         if (this.pathsToExpand.length > 0)
         {
            var node = this.treeview.getNodeByProperty("path", this.pathsToExpand.shift());
            if (node !== null)
            {
               node.expand();
            }
         }
      },

      /**
       * Fired by YUI TreeView when a node label is clicked
       * @method onNodeClicked
       * @param node {YAHOO.widget.Node} the node clicked
       * @return allowExpand {boolean} allow or disallow node expansion
       */
      onNodeClicked: function DLT_onNodeClicked(node)
      {
         YAHOO.Bubbling.fire('onDoclistPathChanged',
         {
            path: node.data.path
         });
         
         // Prevent the tree node from expanding (TODO: user preference?)
         return false;
      },
      
      /**
       * Creates the TreeView control and renders it to the parent element.
       * @method _buildTree
       * @private
       */
      _buildTree: function DLT__buildTree()
      {
         // Create a new tree
         var tree = new YAHOO.widget.TreeView(this.id + "-treeview");
         this.treeview = tree;

         // Turn dynamic loading on for entire tree
         tree.setDynamicLoad(this.fnLoadNodeData);

         // Get root node for tree
         var root = tree.getRoot();

         // Add default top-level node
         var tempNode = new YAHOO.widget.TextNode(
         {
            label: "Documents",
            path: "",
            nodeRef: ""
         }, root, false);

         // Register tree-level listeners
         tree.subscribe("labelClick", this.onNodeClicked, this, true);
         tree.subscribe("expandComplete", this.onExpandComplete, this, true);

         // Render tree with this one top-level node
         tree.draw();
      }

   };
})();
