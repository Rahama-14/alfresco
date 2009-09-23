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
 * Document Library "File Transfer Report" module for Records Management.
 * 
 * @namespace Alfresco.module
 * @class Alfresco.module.RecordsFileTransferReport
 */
(function()
{
   /**
   * YUI Library aliases
   */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event,
      Element = YAHOO.util.Element;

   /**
    * Alfresco Slingshot aliases
    */
    var $html = Alfresco.util.encodeHTML,
       $combine = Alfresco.util.combinePaths;

   Alfresco.module.RecordsFileTransferReport = function(htmlId)
   {
      Alfresco.module.DoclibMoveTo.superclass.constructor.call(this, htmlId);
      
      // Re-register with our own name
      this.name = "Alfresco.module.RecordsFileTransferReport";
      Alfresco.util.ComponentManager.reregister(this);

      // Initialise prototype properties
      this.pathsToExpand = [];

      return this;
   };
   
   YAHOO.extend(Alfresco.module.RecordsFileTransferReport, Alfresco.module.DoclibMoveTo,
   {

      /**
       * Object container for initialization options
       */
      options:
      {
         /**
          * Current fileplan's nodeRef.
          *
          * @property fileplanNodeRef The fileplan's nodeRef
          * @type string
          */
         fileplanNodeRef: null,

         /**
          * Object representing the transfer object with nodeRef and displayName
          *
          * @property transfer The nodeRef to the transfer that shall be filed
          * @type object
          */
         transfer: null
      },

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @override
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.module.RecordsFileTransferReport} returns 'this' for method chaining
       */
      setOptions: function RMCMFT_setOptions(obj)
      {
         return Alfresco.module.RecordsFileTransferReport.superclass.setOptions.call(this, YAHOO.lang.merge(
         {
            templateUrl: Alfresco.constants.URL_SERVICECONTEXT + "modules/documentlibrary/dod5015/file-transfer-report",
            files: obj.transfer // To make the DoclibMoveTo component happy
         }, obj));
      },

      /**
       *
       *
       * @method onOK
       * @override
       */
      onOK: function RMCMFT_onOK()
      {
         // create the webscript url
         var transferNodeRefParts = this.options.transfer.nodeRef.split("/"),
            transferId = transferNodeRefParts[transferNodeRefParts.length - 1],
            url = Alfresco.constants.PROXY_URI + "api/node/" + this.options.fileplanNodeRef.replace(":/", "") + "/transfers/" + transferId + "/report";

         // Post file transfer report request to server
         Alfresco.util.Ajax.jsonPost(
         {
            url: url,
            dataObj:
            {
               destination: this.selectedNode.data.nodeRef
            },
            successCallback:
            {
               fn: function (response)
               {
                  // Hide the dialog
                  this.widgets.dialog.hide();

                  // Get reports record name
                  var reportName = response.json.recordName;

                  // Display success message
                  Alfresco.util.PopupManager.displayMessage(
                  {
                     text: this.msg("message.success", reportName)
                  });

                  // Make sure other components display the new file if present
                  YAHOO.Bubbling.fire("filterChanged",
                  {
                     filterId: "path",
                     filterData: this.selectedNode.data.path,
                     highlightFile: reportName
                  });                  
               },
               scope: this
            },
            failureCallback:
            {
               fn:function (response)
               {
                  // Display error
                  var text = this.msg("message.failure");
                  if(response.json && response.json.message)
                  {
                     text = response.json.message;
                  }
                  Alfresco.util.PopupManager.displayPrompt(
                  {
                     title: Alfresco.util.message("message.failure"),
                     text: text
                  });

                  // Enable dialog buttons again
                  this.widgets.okButton.set("disabled", false);
                  this.widgets.cancelButton.set("disabled", false);
               },
               scope: this
            }
         });

         // Disable dialog buttons
         this.widgets.okButton.set("disabled", true);
         this.widgets.cancelButton.set("disabled", true);
      },

      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Internal show dialog function
       * @method _showDialog
       * @override
       */
      _showDialog: function RMCMFT__showDialog()
      {
         this.widgets.okButton.set("label", this.msg("button.file"));
         return Alfresco.module.RecordsFileTransferReport.superclass._showDialog.apply(this, arguments);
      },

      /**
       * Build URI parameter string for treenode JSON data webscript
       *
       * @method _buildTreeNodeUrl
       * @param path {string} Path to query
       */
       _buildTreeNodeUrl: function RMCMFT__buildTreeNodeUrl(path)
       {
          var uriTemplate = Alfresco.constants.PROXY_URI + "slingshot/doclib/dod5015/treenode/site/{site}/{container}{path}";

          var url = YAHOO.lang.substitute(uriTemplate,
          {
             site: encodeURIComponent(this.options.siteId),
             container: encodeURIComponent(this.options.containerId),
             path: encodeURI(path)
          });

          return url;
       }
   });
})();

/* Dummy instance to load optional YUI components early */
new Alfresco.module.RecordsFileTransferReport("null");