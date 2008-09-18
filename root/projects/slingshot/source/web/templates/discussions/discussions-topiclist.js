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
 * DiscussionsViewTopics template.
 * 
 * @namespace Alfresco
 * @class Alfresco.DiscussionsViewTopics
 */
(function()
{
   /**
    * DocumentLibrary constructor.
    * 
    * @return {Alfresco.DiscussionsViewTopics} The new DiscussionsViewTopics instance
    * @constructor
    */
   Alfresco.DiscussionsViewTopics = function DiscussionsViewTopics_constructor()
   {
      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require(["resize"], this.onComponentsLoaded, this);
            
      return this;
   };
   
   Alfresco.DiscussionsViewTopics.prototype =
   {
      /**
       * Minimum Filter Panel width.
       * 
       * @property MIN_FILTER_PANEL_WIDTH
       * @type int
       */
      MIN_FILTER_PANEL_WIDTH: 140,

      /**
       * Default Filter Panel width.
       * 
       * @property DEFAULT_FILTER_PANEL_WIDTH
       * @type int
       */
      DEFAULT_FILTER_PANEL_WIDTH: 180,

      /**
       * Maximum Filter Panel width.
       * 
       * @property MAX_FILTER_PANEL_WIDTH
       * @type int
       */
      MAX_FILTER_PANEL_WIDTH: 750,
      
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
       widgets: {},

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function DiscussionsViewTopics_onComponentsLoaded()
      {
         YAHOO.util.Event.onDOMReady(this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting.
       * Template initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function DiscussionsViewTopics_onReady()
      {
         // Horizontal Resizer
         this.widgets.horizResize = new YAHOO.util.Resize("divTopicListFilters",
         {
            handles: ["r"],
            minWidth: this.MIN_FILTER_PANEL_WIDTH,
            maxWidth: this.MAX_FILTER_PANEL_WIDTH
         });
         this.widgets.horizResize.on("resize", function(eventTarget)
         {
            this.onTopicListResize(eventTarget.width);
         }, this, true);
         
         // Initial size
         this.widgets.horizResize.resize(null, null, this.DEFAULT_FILTER_PANEL_WIDTH, 0, 0, true);
      },
   
      /**
       * Fired by via resize event listener.
       *
       * @method onDocLibraryResize
       */
      onTopicListResize: function DiscussionsViewTopics_onTopicListResize(width)
      {
         var Dom = YAHOO.util.Dom;
         
         if (typeof width != 'undefined')
         {
            // Reset widget height to ensure correct rendering
            Dom.setStyle(Dom.get("divTopicListFilters"), "height", "auto");
            // 8px breathing space for resize gripper
            Dom.setStyle(Dom.get("divTopicListTopics"), "margin-left", 8 + width + "px");
         }
      }
   };
   
})();

// Instantiate the Discussions View Topics template
new Alfresco.DiscussionsViewTopics();