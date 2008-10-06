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
 * Calendar template.
 * 
 * @namespace Alfresco
 * @class Alfresco.CalendarComponent
 */
(function()
{
   /**
    * CalendarComponent constructor.
    * 
    * @return {Alfresco.Calendar} The new Calenda instance
    * @constructor
    */
   Alfresco.CalendarComponent = function Calendar_constructor()
   {
      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require([], this.onComponentsLoaded, this);
            
      return this;
   };
   
   Alfresco.CalendarComponent.prototype =
   {
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
      onComponentsLoaded: function Calendar_onComponentsLoaded()
      {
         YAHOO.util.Event.onDOMReady(this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting.
       * Template initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function Calendar_onReady()
      {
         return;
         var Dom = YAHOO.util.Dom;
         
         // Horizontal Resizer
         this.widgets.horizResize = new YAHOO.util.Resize("divCalendarFilters",
         {
            handles: ["r"],
            minWidth: this.MIN_FILTER_PANEL_WIDTH,
            maxWidth: this.MAX_FILTER_PANEL_WIDTH
         });
         this.widgets.horizResize.on("resize", function(eventTarget)
         {
            this.onCalendarResize(eventTarget.width);
         }, this, true);
         
         // Initial size
         if (YAHOO.env.ua.ie > 0)
         {
            this.widgets.horizResize.resize(null, this.widgets.horizResize.get("element").offsetHeight, this.DEFAULT_FILTER_PANEL_WIDTH, 0, 0, true);
         }
         else
         {
            this.widgets.horizResize.resize(null, this.widgets.horizResize.get("height"), this.DEFAULT_FILTER_PANEL_WIDTH, 0, 0, true);
         }
      },
   
      /**
       * Fired by via resize event listener.
       *
       * @method onCalendarResize
       */
      onCalendarResize: function Calendar_onCalendarResize(width)
      {
         var Dom = YAHOO.util.Dom;
         
         if (typeof width != 'undefined')
         {
            // Reset widget height to ensure correct rendering
            if (YAHOO.env.ua.ie == 0)
            {
               Dom.setStyle("divCalendarFilters", "height", "auto");
            }
            // 6px breathing space for resize gripper
            Dom.setStyle("divCalendarEvents", "margin-left", 8 + width + "px");
         }
      }
   };
})();

// Instantiate the Calendar template
new Alfresco.CalendarComponent();