/**
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
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * Document actions component.
 * 
 * @namespace Alfresco
 * @class Alfresco.DocumentActions
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;
   
   /**
    * DocumentActions constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.DocumentActions} The new DocumentActions instance
    * @constructor
    */
   Alfresco.DocumentActions = function(htmlId)
   {
      Alfresco.DocumentActions.superclass.constructor.call(this, "Alfresco.DocumentActions", htmlId, ["button"]);
      
      /* Decoupled event listeners */
      YAHOO.Bubbling.on("documentDetailsAvailable", this.onDocumentDetailsAvailable, this);
      
      return this;
   };

   /**
    * Extend Alfresco.component.Base
    */
   YAHOO.extend(Alfresco.DocumentActions, Alfresco.component.Base);
   
   /**
    * Augment prototype with Actions module
    */
   YAHOO.lang.augmentProto(Alfresco.DocumentActions, Alfresco.doclib.Actions);

   /**
    * Augment prototype with main class implementation, ensuring overwrite is enabled
    */
   YAHOO.lang.augmentObject(Alfresco.DocumentActions.prototype,
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * Working mode: Site or Repository.
          * Affects how actions operate, e.g. actvities are not posted in Repository mode.
          * 
          * @property workingMode
          * @type number
          * @default Alfresco.doclib.MODE_SITE
          */
         workingMode: Alfresco.doclib.MODE_SITE,

         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",
         
         /**
          * ContainerId representing root container
          *
          * @property containerId
          * @type string
          * @default "documentLibrary"
          */
         containerId: "documentLibrary",

         /**
          * Valid inline edit mimetypes
          * Currently allowed are plain text, HTML and XML only
          *
          * @property inlineEditMimetypes
          * @type object
          */
         inlineEditMimetypes:
         {
            "text/plain": true,
            "text/html": true,
            "text/xml": true
         }
      },
      
      /**
       * The data for the document
       * 
       * @property assetData
       * @type object
       */
      assetData: null,
      
      /**
       * Metadata returned by doclist data webscript
       *
       * @property doclistMetadata
       * @type object
       * @default null
       */
      doclistMetadata: null,
      
      /**
       * Path of asset being viewed - used to scope some actions (e.g. copy to, move to)
       * 
       * @property currentPath
       * @type string
       */
      currentPath: null,

      /**
       * The urls to be used when creating links in the action cell
       *
       * @method getActionUrls
       * @return {object} Object literal containing URLs to be substituted in action placeholders
       */
      getActionUrls: function DocumentActions_getActionUrls()
      {
         var recordData = this.assetData,
            nodeRef = recordData.nodeRef,
            custom = recordData.custom || {};

         return (
         {
            downloadUrl: Alfresco.constants.PROXY_URI + recordData.contentUrl + "?a=true",
            viewUrl:  Alfresco.constants.PROXY_URI + recordData.contentUrl + "\" target=\"_blank",
            documentDetailsUrl: "document-details?nodeRef=" + nodeRef,
            editMetadataUrl: "edit-metadata?nodeRef=" + nodeRef,
            inlineEditUrl: "inline-edit?nodeRef=" + nodeRef,
            workingCopyUrl: "document-details?nodeRef=" + (custom.workingCopyNode || nodeRef),
            originalUrl: "document-details?nodeRef=" + (custom.workingCopyOriginal || nodeRef),
            managePermissionsUrl: "permissions?nodeRef=" + nodeRef + "&itemName=" + encodeURIComponent(recordData.displayName) + "&nodeType=" + recordData.type
         });
      },
       
      /**
       * Event handler called when the "documentDetailsAvailable" event is received
       *
       * @method: onDocumentDetailsAvailable
       */
      onDocumentDetailsAvailable: function DocumentActions_onDocumentDetailsAvailable(layer, args)
      {
         var me = this;
         
         // Asset data passed-in through event arguments
         this.assetData = args[1].documentDetails;
         this.doclistMetadata = args[1].metadata;
         this.currentPath = this.assetData.location.path;
         
         // Copy template into active area
         var assetData = this.assetData,
            actionsContainer = Dom.get(this.id + "-actionSet"),
            actionSet = assetData.actionSet,
            clone = Dom.get(this.id + "-actionSet-" + actionSet).cloneNode(true),
            downloadUrl = Alfresco.constants.PROXY_URI + assetData.contentUrl + "?a=true",
            displayName = assetData.displayName;

         // Token replacement
         clone.innerHTML = YAHOO.lang.substitute(window.unescape(clone.innerHTML), this.getActionUrls());

         // Replace existing actions and assign correct class for icon rendering
         actionsContainer.innerHTML = clone.innerHTML;
         Dom.addClass(actionsContainer, assetData.type);

         // Hide actions which have been disallowed through permissions
         if (assetData.permissions && assetData.permissions.userAccess)
         {
            var userAccess = assetData.permissions.userAccess,
               actionLabels = assetData.actionLabels || {},
               actions = YAHOO.util.Selector.query("div", actionsContainer),
               action, actionPermissions, i, ii, j, jj, actionAllowed, aTag, spanTag;

            // Inject special-case permissions for inline and online editing
            if (assetData.mimetype in this.options.inlineEditMimetypes)
            {
               userAccess["inline-edit"] = true;
            }
            
            for (i = 0, ii = actions.length; i < ii; i++)
            {
               action = actions[i];
               actionAllowed = true;
               aTag = action.firstChild;
               spanTag = aTag.firstChild;

               if (spanTag && actionLabels[action.className])
               {
                  spanTag.innerHTML = $html(actionLabels[action.className]);
               }

               if (aTag.rel !== "")
               {
                  actionPermissions = aTag.rel.split(",");
                  for (j = 0, jj = actionPermissions.length; j < jj; j++)
                  {
                     if (!userAccess[actionPermissions[j]])
                     {
                        actionAllowed = false;
                        break;
                     }
                  }
               }
               Dom.setStyle(action, "display", actionAllowed ? "block" : "none");
            }
         }
         Dom.setStyle(actionsContainer, "visibility", "visible");
         
         // Hook action events
         var fnActionHandler = function DocumentActions_fnActionHandler(layer, args)
         {
            var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
            if (owner !== null)
            {
               var action = owner.className;
               if (typeof me[action] == "function")
               {
                  args[1].stop = true;
                  me[action].call(me, me.assetData, owner);
               }
            }
            return true;
         };
         
         YAHOO.Bubbling.addDefaultAction("action-link", fnActionHandler);
         
         // DocLib Actions module
         this.modules.actions = new Alfresco.module.DoclibActions(this.options.workingMode);
         
         // Prompt auto-download (after Edit Offline action)?
         if (window.location.hash == "#editOffline")
         {
            window.location.hash = "";

            if (YAHOO.env.ua.ie > 6)
            {
               // MSIE7 blocks the download and gets the wrong URL in the "manual download bar"
               Alfresco.util.PopupManager.displayPrompt(
               {
                  title: this.msg("message.edit-offline.success", displayName),
                  text: this.msg("message.edit-offline.success.ie7"),
                  buttons: [
                  {
                     text: this.msg("button.download"),
                     handler: function DocumentActions_oAEO_success_download()
                     {
                        window.location = downloadUrl;
                        this.destroy();
                     },
                     isDefault: true
                  },
                  {
                     text: this.msg("button.close"),
                     handler: function DocumentActions_oAEO_success_close()
                     {
                        this.destroy();
                     }
                  }]
               });
            }
            else
            {
               Alfresco.util.PopupManager.displayMessage(
               {
                  text: this.msg("message.edit-offline.success", displayName)
               });
               // Kick off the download 3 seconds after the confirmation message
               YAHOO.lang.later(3000, this, function()
               {
                  window.location = downloadUrl;
               });
            }
         }
         
         if (window.location.hash == "#editCancelled")
         {
            window.location.hash = "";
            Alfresco.util.PopupManager.displayMessage(
            {
               text: this.msg("message.edit-cancel.success", displayName)
            });
         }
      },

      /**
       * Edit Offline.
       *
       * @override
       * @method onActionEditOffline
       * @param asset {object} Object literal representing file or folder to be actioned
       */
      onActionEditOffline: function DocumentActions_onActionEditOffline(asset)
      {
         var displayName = asset.displayName,
            nodeRef = new Alfresco.util.NodeRef(asset.nodeRef);

         this.modules.actions.genericAction(
         {
            success:
            {
               callback:
               {
                  fn: function DocumentActions_oAEO_success(data)
                  {
                     this.assetData.nodeRef = data.json.results[0].nodeRef;
                     window.location = this.getActionUrls().documentDetailsUrl + "#editOffline";
                  },
                  scope: this
               }
            },
            failure:
            {
               message: this.msg("message.edit-offline.failure", displayName)
            },
            webscript:
            {
               method: Alfresco.util.Ajax.POST,
               name: "checkout/node/{nodeRef}",
               params:
               {
                  nodeRef: nodeRef.uri
               }
            }
         });
      },

      /**
       * Cancel editing.
       *
       * @override
       * @method onActionCancelEditing
       * @param asset {object} Object literal representing file or folder to be actioned
       */
      onActionCancelEditing: function DocumentActions_onActionCancelEditing(asset)
      {
         var displayName = asset.displayName,
            nodeRef = new Alfresco.util.NodeRef(asset.nodeRef);

         this.modules.actions.genericAction(
         {
            success:
            {
               callback:
               {
                  fn: function DocumentActions_oACE_success(data)
                  {
                     this.assetData.nodeRef = data.json.results[0].nodeRef;
                     window.location = this.getActionUrls().documentDetailsUrl + "#editCancelled";
                  },
                  scope: this
               }
            },
            failure:
            {
               message: this.msg("message.edit-cancel.failure", displayName)
            },
            webscript:
            {
               method: Alfresco.util.Ajax.POST,
               name: "cancel-checkout/node/{nodeRef}",
               params:
               {
                  nodeRef: nodeRef.uri
               }
            }
         });
      },

      /**
       * Upload new version.
       *
       * @override
       * @method onActionUploadNewVersion
       * @param asset {object} Object literal representing the file to be actioned upon
       */
      onActionUploadNewVersion: function DocumentActions_onActionUploadNewVersion(asset)
      {
         var displayName = asset.displayName,
            nodeRef = new Alfresco.util.NodeRef(asset.nodeRef),
            version = asset.version;

         if (!this.fileUpload)
         {
            this.fileUpload = Alfresco.getFileUploadInstance();
         }

         // Show uploader for multiple files
         var description = this.msg("label.filter-description", displayName),
            extensions = "*";

         if (displayName && new RegExp(/[^\.]+\.[^\.]+/).exec(displayName))
         {
            // Only add a filtering extension if filename contains a name and a suffix
            extensions = "*" + displayName.substring(displayName.lastIndexOf("."));
         }
         
         if (asset.custom && asset.custom.workingCopyVersion)
         {
            version = asset.custom.workingCopyVersion;
         }
         
         var singleUpdateConfig =
         {
            updateNodeRef: nodeRef.toString(),
            updateFilename: displayName,
            updateVersion: version,
            suppressRefreshEvent: true,
            overwrite: true,
            filter: [
            {
               description: description,
               extensions: extensions
            }],
            mode: this.fileUpload.MODE_SINGLE_UPDATE,
            onFileUploadComplete:
            {
               fn: this.onNewVersionUploadCompleteCustom,
               scope: this
            }
         };
         if (this.options.workingMode == Alfresco.doclib.MODE_SITE)
         {
            singleUpdateConfig.siteId = this.options.siteId;
            singleUpdateConfig.containerId = this.options.containerId;
         }
         this.fileUpload.show(singleUpdateConfig);
      },

      /**
       * Called from the uploader component after a the new version has been uploaded.
       *
       * @method onNewVersionUploadCompleteCustom
       * @param complete {object} Object literal containing details of successful and failed uploads
       */
      onNewVersionUploadCompleteCustom: function DocumentActions_onNewVersionUploadCompleteCustom(complete)
      {
         // Call the normal callback to post the activity data
         this.onNewVersionUploadComplete.call(this, complete);
         this.assetData.nodeRef = complete.successful[0].nodeRef;
         window.location = this.getActionUrls().documentDetailsUrl;
      },
      
      /**
       * Delete Asset confirmed.
       *
       * @override
       * @method _onActionDeleteConfirm
       * @param asset {object} Object literal representing file or folder to be actioned
       * @private
       */
      _onActionDeleteConfirm: function DocumentActions__onActionDeleteConfirm(asset)
      {
         var path = asset.location.path,
            fileName = asset.fileName,
            displayName = asset.displayName,
            nodeRef = new Alfresco.util.NodeRef(asset.nodeRef),
            callbackUrl = this.options.workingMode == Alfresco.doclib.MODE_SITE ? "documentlibrary" : "repository",
            encodedPath = path.length > 1 ? "?path=" + window.escape(path) : "";
         
         this.modules.actions.genericAction(
         {
            success:
            {
               activity:
               {
                  siteId: this.options.siteId,
                  activityType: "file-deleted",
                  page: "documentlibrary",
                  activityData:
                  {
                     fileName: fileName,
                     path: path
                  }
               },
               callback:
               {
                  fn: function DocumentActions_oADC_success(data)
                  {
                     window.location = callbackUrl + encodedPath;
                  }
               }
            },
            failure:
            {
               message: this.msg("message.delete.failure", displayName)
            },
            webscript:
            {
               method: Alfresco.util.Ajax.DELETE,
               name: "file/node/{nodeRef}",
               params:
               {
                  nodeRef: nodeRef.uri
               }
            }
         });
      }      
   }, true);
})();