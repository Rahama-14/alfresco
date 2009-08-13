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
 * Document links component.
 * 
 * @namespace Alfresco
 * @class Alfresco.DocumentLinks
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
   var $html = Alfresco.util.encodeHTML;
   
   /**
    * DocumentLinks constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.DocumentLinks} The new DocumentLinks instance
    * @constructor
    */
   Alfresco.DocumentLinks = function(htmlId)
   {
      this.name = "Alfresco.DocumentLinks";
      this.id = htmlId;
      
      // initialise prototype properties
      this.widgets = {};
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button"], this.onComponentsLoaded, this);
   
      /* Decoupled event listeners */
      YAHOO.Bubbling.on("documentDetailsAvailable", this.onDocumentDetailsAvailable, this);
      
      return this;
   }
   
   Alfresco.DocumentLinks.prototype =
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
          * External authentication being used for login
          * 
          * @property externalAuth
          * @type boolean
          */
         externalAuth: false
      },
      
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: {},
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.Search} returns 'this' for method chaining
       */
      setOptions: function DocumentLinks_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.Search} returns 'this' for method chaining
       */
      setMessages: function DocumentLinks_setMessages(obj)
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
      onComponentsLoaded: function DocumentLinks_onComponentsLoaded()
      {
         // don't need to do anything we will be informed via an event when data is ready
      },
            
      /**
       * Event handler called when the "documentDetailsAvailable" event is received
       *
       * @method: onDocumentDetailsAvailable
       */
      onDocumentDetailsAvailable: function DocumentLinks_onDocumentDetailsAvailable(layer, args)
      {
         var docData = args[1].documentDetails;
         
         // construct the base content URL
         var contentUrl = (this.options.externalAuth ? Alfresco.constants.PROXY_URI : Alfresco.constants.PROXY_FEED_URI) + docData.contentUrl;
         
         // populate the text field with the download url
         var downloadUrl = contentUrl + "?a=true";
         Dom.get(this.id + "-download-url").value = downloadUrl;
         
         var clipboard = (window.clipboardData && clipboardData.setData);
         
         // create YUI button for download copy button
         if (clipboard)
         {
            this.widgets.downloadCopyButton = Alfresco.util.createYUIButton(this, "download-button", null,
            {
               onclick:
               {
                  fn: this._handleCopyClick,
                  obj: "-download-url",
                  scope: this
               }
            });
         }
         
         // populate the text field with the view url
         Dom.get(this.id + "-view-url").value = contentUrl;
         
         // create YUI button for view copy button
         if (clipboard)
         {
            this.widgets.viewCopyButton = Alfresco.util.createYUIButton(this, "view-button", null,
            {
               onclick:
               {
                  fn: this._handleCopyClick,
                  obj: "-view-url",
                  scope: this
               }
            });
         }
         
         // populate the text field with the page url
         Dom.get(this.id + "-page-url").value = window.location.href;
         
         // create YUI button for page copy button
         if (clipboard)
         {
            this.widgets.pageCopyButton = Alfresco.util.createYUIButton(this, "page-button", null,
            {
               onclick:
               {
                  fn: this._handleCopyClick,
                  obj: "-page-url",
                  scope: this
               }
            });
         }
 
         // add focus event handlers to fields        
         Event.addListener(Dom.get(this.id + "-download-url"), "focus", this._handleFocus, "-download-url", this);
         Event.addListener(Dom.get(this.id + "-view-url"), "focus", this._handleFocus, "-view-url", this);
         Event.addListener(Dom.get(this.id + "-page-url"), "focus", this._handleFocus, "-page-url", this);
      },
      
      /**
       * Event handler to copy URLs to the system clipboard
       * 
       * @method _handleCopyClick
       * @param event The event
       * @param urlId The id of the element holding the URL to copy
       */
      _handleCopyClick: function DocumentLinks__handleCopyClick(event, urlId)
      {
         clipboardData.setData("Text", Dom.get(this.id + urlId).value);
      },
      
      /**
       * Event handler used to select text in the field when focus is received
       *
       * @method _handleFocus
       * @param event The event
       * @field The suffix of the id of the field to select
       */
      _handleFocus: function DocumentLinks__handleFocus(event, field)
      {
         YAHOO.util.Event.stopEvent(event);
         
         var fieldObj = Dom.get(this.id + field);
         if (fieldObj && fieldObj.select)
         {
            fieldObj.select();
         }
      }
   };
})();
