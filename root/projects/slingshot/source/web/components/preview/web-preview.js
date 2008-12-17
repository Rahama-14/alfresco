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
 * WebPreview component. 
 *
 * @namespace Alfresco
 * @class Alfresco.WebPreview
 */
(function()
{

   var Dom = YAHOO.util.Dom;

   /**
    * WebPreview constructor.
    *
    * @param {string} htmlId The HTML id of the parent element
    * @return {Alfresco.WebPreview} The new WebPreview instance
    * @constructor
    * @private
    */
   Alfresco.WebPreview = function(containerId)
   {
      this.name = "Alfresco.WebPreview";
      this.id = containerId;      

      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      // Load YUI Components
      // Load uploader so we get access to swfobject
      Alfresco.util.YUILoaderHelper.require(["button", "container", "datatable", "datasource", "uploader"], this.onComponentsLoaded, this);

      this.widgets = {};

      return this;
   };

   Alfresco.WebPreview.prototype =
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
          * Noderef to the content to display
          *
          * @property nodeRef
          * @type string
          */
         nodeRef: "",

         /**
          * The file name representing root container
          *
          * @property name
          * @type string
          */
         name: "",

         /**
          * The icon displayed in the header of the component
          *
          * @property icon
          * @type string
          */
         icon: "",

         /**
          * The mimeType of the node to display, needed to decide what preview
          * that should be used.
          *
          * @property mimeType
          * @type string
          */
         mimeType: "",

         /**
          * A list of previews available for this component
          *
          * @property previews
          * @type Array
          */
         previews: []
      },

      /**
       * Object container for storing YUI widget instances.
       *
       * @property widgets
       * @type object
       */
       widgets: null,

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.Preview} returns 'this' for method chaining
       */
      setOptions: function WP_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },

      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.Preview} returns 'this' for method chaining
       */
      setMessages: function WP_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function WP_onComponentsLoaded()
      {

         // Save a reference to the HTMLElement displaying texts so we can alter the texts later
         this.widgets.swfPlayerDiv = Dom.get(this.id + "-swfPlayer-div");
         this.widgets.swfPlayerMessage = Dom.get(this.id + "-swfPlayerMessage-div");
         this.widgets.titleText = Dom.get(this.id + "-title-span");
         this.widgets.titleImg = Dom.get(this.id + "-title-img");

         // Set title and icon         
         this.widgets.titleText["innerHTML"] = this.options.name;
         this.widgets.titleImg.src = Alfresco.constants.URL_CONTEXT + this.options.icon.substring(1);


         // nodeRef is mandatory
         if (this.options.nodeRef === undefined)
         {
             throw new Error("A nodeRef must be provided");
         }

         if (Alfresco.util.hasRequiredFlashPlayer(9, 0, 45))
         {
            var previewCtx = this._resolvePreview();
            if (previewCtx)
            {                  
               // Use swfobject
               var so = new deconcept.SWFObject(Alfresco.constants.URL_CONTEXT + "/components/preview/WebPreviewer.swf",
                       "WebPreviewer", "100%", "670", "9.0.45");
               so.addVariable("fileName", this.options.name);
               so.addVariable("paging", previewCtx.paging);
               so.addVariable("url", previewCtx.url);
               so.addVariable("jsCallback", "Alfresco.util.ComponentManager.find({id:'" + this.id + "'})[0].onWebPreviewerEvent");
               so.addParam("allowScriptAccess", "sameDomain");
               so.addParam("allowFullScreen", "true");
               so.addParam("wmode", "transparent");                
               so.write(this.id + "-swfPlayer-div");     
            }
            else
            {
               // Cant find a preview
               var url = Alfresco.constants.PROXY_URI + "api/node/content/" + this.options.nodeRef.replace(":/", "") + "/" + encodeURIComponent(this.options.name) + "?a=true";
               var message = Alfresco.util.message("label.noPreview", this.name,
               {
                  "0": url
               });
               this.widgets.swfPlayerMessage["innerHTML"] = message;
            }
         }
         else
         {
            // No sufficient flash player installed
            var message = Alfresco.util.message("label.noFlash", this.name);
            this.widgets.swfPlayerMessage["innerHTML"] = message;
         }

      },


      /**
       * Helper method for deciding what preview to use, if any
       *
       * @method _resolvePreview
       * @return the name of the preview to use or nullif none is appropriate
       */
      _resolvePreview: function WP__resolvePreview(event)
      {
         // Try to prioritise usage of imgpreview for images and webpreview for other content
         var ps = this.options.previews;
         var webpreview = "webpreview", imgpreview = "imgpreview";
         var nodeRefAsLink = this.options.nodeRef.replace(":/", "");
         var argsNoCache = "?c=force&noCacheToken=" + new Date().getTime();
         if (this.options.mimeType.match(/^image\/\w+/))
         {
            var preview = Alfresco.util.arrayContains(ps, imgpreview) ? imgpreview : (Alfresco.util.arrayContains(ps, webpreview) ? webpreview : null);
            var url = Alfresco.constants.PROXY_URI + "api/node/" + nodeRefAsLink + "/content/thumbnails/" + preview + argsNoCache;
            return {url: url, paging: false};
         }
         else if (this.options.mimeType.match(/application\/x-shockwave-flash/))
         {
            url = Alfresco.constants.PROXY_URI + "api/node/content/" + nodeRefAsLink + argsNoCache + "&a=true";
            return {url: url, paging: false};
         }
         else
         {
            var preview = Alfresco.util.arrayContains(ps, webpreview) ? webpreview : (Alfresco.util.arrayContains(ps, imgpreview) ? imgpreview : null);
            if (preview != null)
            {
               var url = Alfresco.constants.PROXY_URI + "api/node/" + nodeRefAsLink + "/content/thumbnails/" + preview + argsNoCache;
               return {url: url, paging: true};
            }
            return null;
         }
      },

      /**
       * Called from the WebPreviewer when an event is dispatched.
       *
       * @method onWebPreviewerEvent
       * @param event {object} an WebPreview message
       */
      onWebPreviewerEvent: function WP_onWebPreviewerEvent(event)
      {
         // Inform the user about the failure
         var message = "Error";
         if (event.code)
         {
            message = Alfresco.util.message(event.code, this.name);
         } 
         Alfresco.util.PopupManager.displayMessage(
         {
            text: message
         });

         // Tell other components that the preview failed
         YAHOO.Bubbling.fire("webPreviewFailure",
         {
            error: event.code,
            nodeRef: this.showConfig.nodeRef,
            failureUrl: this.showConfig.failureUrl
         });

      }

   };

})();