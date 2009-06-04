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
 * Dashboard Activities common component.
 * 
 * @namespace Alfresco
 * @class Alfresco.Activities
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
    * Dashboard Activities constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.Activities} The new component instance
    * @constructor
    */
   Alfresco.Activities = function Activities_constructor(htmlId)
   {
      this.name = "Alfresco.Activities";
      this.id = htmlId;
      
      this.widgets = {};

      // Register this component
      Alfresco.util.ComponentManager.register(this);

      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require(["button", "container"], this.onComponentsLoaded, this);

      return this;
   };

   Alfresco.Activities.prototype =
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
          * Dashlet mode
          * 
          * @property mode
          * @type string
          * @default "site"
          */
         mode: "site",

         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",

         /**
          * Currently active filter.
          * 
          * @property activeFilter
          * @type string
          * @default "today"
          */
         activeFilter: "today"
      },
      
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: null,

      /**
       * Activity list DOM container.
       * 
       * @property activityList
       * @type object
       */
      activityList: null,

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.Activities} returns 'this' for method chaining
       */
      setOptions: function Activities_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.Activities} returns 'this' for method chaining
       */
      setMessages: function Activities_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },
      
      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function Activities_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },

      /**
       * Fired by YUI when parent element is available for scripting
       * @method onReady
       */
      onReady: function Activities_onReady()
      {
         var me = this;

         // Dropdown filter
         this.widgets.range = new YAHOO.widget.Button(this.id + "-range",
         {
            type: "split",
            menu: this.id + "-range-menu"
         });
         this.widgets.range.on("click", this.onDateFilterClicked, this, true);
         this.widgets.range.getMenu().subscribe("click", function (p_sType, p_aArgs)
         {
            var menuItem = p_aArgs[1];
            if (menuItem)
            {
               me.widgets.range.set("label", menuItem.cfg.getProperty("text"));
               me.onDateFilterChanged.call(me, p_aArgs[1]);
            }
         });
         this.widgets.range.value = "today";
         // Dropdown filter
         this.widgets.user = new YAHOO.widget.Button(this.id + "-user",
         {
            type: "split",
            menu: this.id + "-user-menu"
         });
         //exclusion filter
         this.widgets.user.on("click", this.onExclusionFilterClicked, this, true);
         this.widgets.user.getMenu().subscribe("click", function (p_sType, p_aArgs)
         {
            var menuItem = p_aArgs[1];
            if (menuItem)
            {
               me.widgets.user.set("label", menuItem.cfg.getProperty("text"));
               me.onExclusionFilterChanged.call(me, p_aArgs[1]);

            }
         });
         this.widgets.user.value = "others";

         // The activity list container
         this.activityList = Dom.get(this.id + "-activityList");
         
         // Populate the activity list
         this.populateActivityList(this.widgets.range.value,this.widgets.user.value);
      },
      
      /**
       * Populate the activity list via Ajax request
       * @method populateActivityList
       */
      populateActivityList: function Activities_populateActivityList(dateFilter,userFilter)
      {
         // Load the activity list
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.URL_SERVICECONTEXT + "components/dashlets/activities/list",
            dataObj:
            {
               site: this.options.siteId,
               mode: this.options.mode,
               dateFilter: dateFilter,
               userFilter: userFilter
            },
            successCallback:
            {
               fn: this.onListLoaded,
               scope: this,
               obj: dateFilter
            },
            failureCallback:
            {
               fn: this.onListLoadFailed,
               scope: this
            },
            scope: this,
            noReloadOnAuthFailure: true
         });
      },
      
      /**
       * List loaded successfully
       * @method onListLoaded
       * @param p_response {object} Response object from request
       */
      onListLoaded: function Activities_onListLoaded(p_response, p_obj)
      {
         this.options.activeFilter = p_obj;
         this.activityList.innerHTML = p_response.serverResponse.responseText;
         this.updateFeedLink(this.widgets.range.value,this.widgets.user.value);
      },

      /**
       * List load failed
       * @method onListLoadFailed
       */
      onListLoadFailed: function Activities_onListLoadFailed()
      {
         this.activityList.innerHTML = '<div class="detail-list-item first-item last-item">' + this._msg("label.load-failed") + '</div>';
      },
      
      
      /**
       * Updates the href attribute on the feed link
       * @method updateFeedLink
       */
      updateFeedLink: function Activities_updateFeedLink(dateFilter,userFilter)
      {
         var link = Dom.get(this.id + "-feedLink");
         if (link)
         {
            var url = Alfresco.constants.URL_FEEDSERVICECONTEXT + "components/dashlets/activities/list?";
            var dataObj =
            {
               format: "atomfeed",
               mode: this.options.mode,
               site: this.options.siteId,
               dateFilter: dateFilter,
               userFilter: userFilter
            };
            url += Alfresco.util.Ajax.jsonToParamString(dataObj, true);
            link.setAttribute("href", url);
         }
      },
      

      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */


      /**
       * Date button clicked event handler
       * @method onDateFilterClicked
       * @param p_oEvent {object} Dom event
       */
      onDateFilterClicked: function Activities_onDateFilterClicked(p_oEvent)
      {
         this.populateActivityList(this.widgets.range.value,this.widgets.user.value);
      },
      
      /**
       * Date drop-down changed event handler
       * @method onDateFilterChanged
       * @param p_oMenuItem {object} Selected menu item
       */
      onDateFilterChanged: function Activities_onDateFilterChanged(p_oMenuItem)
      {
         this.widgets.range.value = p_oMenuItem.value;
         // this.setActiveFilter(dateFilter,Alfresco.Activities.FILTER_BYDATE);
         this.populateActivityList(this.widgets.range.value,this.widgets.user.value);
      },
      /**
       * Exclusion drop-down changed event handler
       * @method onExclusionFilterChanged
       * @param p_oMenuItem {object} Selected menu item
       */
      onExclusionFilterChanged: function Activities_onExclusionFilterChanged(p_oMenuItem)
      {
         this.widgets.user.value = p_oMenuItem.value;
         this.populateActivityList(this.widgets.range.value, this.widgets.user.value);
      },      
      /**
       * Exclusion button clicked event handler
       * @method onExclusionFilterClicked
       * @param p_oEvent {object} Dom event
       */
      onExclusionFilterClicked: function Activities_onExclusionFilterClicked(p_oEvent)
      {
         this.populateActivityList(this.widgets.range.value,this.widgets.user.value);

      },

      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function Activities__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.Activities", Array.prototype.slice.call(arguments).slice(1));
      }
   };
   Alfresco.Activities.FILTER_BYDATE = 'byDate';
   Alfresco.Activities.FILTER_BYUSER = 'byUser';
})();
