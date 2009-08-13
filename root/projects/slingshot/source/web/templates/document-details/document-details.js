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
 * DocumentDetails template.
 * 
 * @namespace Alfresco
 * @class Alfresco.DocumentDetails
 */
(function()
{
   /**
    * DocumentDetails constructor.
    * 
    * @return {Alfresco.DocumentDetails} The new DocumentDetails instance
    * @constructor
    */
   Alfresco.DocumentDetails = function DocumentDetails_constructor()
   {
      Alfresco.DocumentDetails.superclass.constructor.call(this, null, "Alfresco.DocumentDetails", ["editor"]);

      /* Decoupled event listeners */
      YAHOO.Bubbling.on("metadataRefresh", this.onReady, this);
      YAHOO.Bubbling.on("filesPermissionsUpdated", this.onReady, this);
      YAHOO.Bubbling.on("filesMoved", this.onReady, this);
            
      return this;
   };
   
   YAHOO.extend(Alfresco.DocumentDetails, Alfresco.component.Base,
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
          * nodeRef of document being viewed
          * 
          * @property nodeRef
          * @type string
          */
         nodeRef: null,
         
         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: ""
      },

      /**
       * Fired by YUILoaderHelper when required component script files have been loaded into the browser.
       * NOTE: This component doesn't have an htmlId, so we can't use onContentReady.
       *
       * @override
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function DocumentDetails_onComponentsLoaded()
      {
         YAHOO.util.Event.onDOMReady(this.onReady, this, true);
      },

      /**
       * Fired by YUI when parent element is available for scripting.
       * Template initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function DocumentDetails_onReady()
      {
         var config =
         {
            method: "GET",
            url: Alfresco.constants.PROXY_URI + 'slingshot/doclib/doclist/documents/node/' + 
                 this.options.nodeRef.replace(":/", "") + '?filter=node',
            successCallback: 
            { 
               fn: this._getDataSuccess, 
               scope: this 
            },
            failureMessage: "Failed to load data for document details"
         };
         Alfresco.util.Ajax.request(config);
      },
      
      /**
       * Success handler called when the AJAX call to the doclist web script returns successfully
       *
       * @method _getDataSuccess
       * @param response {object} The response object
       * @private
       */
      _getDataSuccess: function DocumentDetails__getDataSuccess(response)
      {
         if (response.json !== undefined)
         {
            var documentDetails = response.json.items[0];
            
            // Fire event to inform any listening components that the data is ready
            YAHOO.Bubbling.fire("documentDetailsAvailable",
            {
               documentDetails: documentDetails,
               metadata: response.json.metadata
            });
            
            // Fire event to show comments for document
            YAHOO.Bubbling.fire("setCommentedNode",
            { 
               nodeRef: documentDetails.nodeRef,
               title: documentDetails.displayName,
               page: "document-details",
               pageParams:
               {
                  nodeRef: this.options.nodeRef
               }
            });
         }
      }
   });
})();
