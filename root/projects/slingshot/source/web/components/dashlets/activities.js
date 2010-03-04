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
    * Preferences
    */
   var PREFERENCES_ACTIVITIES = "org.alfresco.share.activities",
       PREF_FILTER = PREFERENCES_ACTIVITIES + ".filter",
       PREF_RANGE = PREFERENCES_ACTIVITIES + ".range";
   
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

      // Preferences service
      this.preferencesService = new Alfresco.service.Preferences();
      
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
         
         // Create dropdown filter widgets
         this.widgets.range = new YAHOO.widget.Button(this.id + "-range",
         {
            type: "split",
            menu: this.id + "-range-menu",
            lazyloadmenu: false
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
         
         this.widgets.user = new YAHOO.widget.Button(this.id + "-user",
         {
            type: "split",
            menu: this.id + "-user-menu",
            lazyloadmenu: false
         });
         
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
         
         // The activity list container
         this.activityList = Dom.get(this.id + "-activityList");
         
         // Load preferences to override default filter and range
         this.widgets.range.value = "today";
         this.widgets.user.value = "others";
         this.preferencesService.request(PREFERENCES_ACTIVITIES,
         {
            successCallback:
            {
               fn: function(p_oResponse)
               {
                  var rangePreference = Alfresco.util.findValueByDotNotation(p_oResponse.json, PREF_RANGE, null);
                  if (rangePreference !== null)
                  {
                     this.widgets.range.value = rangePreference;
                     // set the correct menu label
                     var menuItems = this.widgets.range.getMenu().getItems();
                     for (index in menuItems)
                     {
                        if (menuItems.hasOwnProperty(index))
                        {
                           if (menuItems[index].value === rangePreference)
                           {
                              this.widgets.range.set("label", menuItems[index].cfg.getProperty("text"));
                              break;
                           }
                        }
                     }
                  }
                  
                  var filterPreference = Alfresco.util.findValueByDotNotation(p_oResponse.json, PREF_FILTER, null);
                  if (filterPreference !== null)
                  {
                     this.widgets.user.value = filterPreference;
                     // set the correct menu label
                     var menuItems = this.widgets.user.getMenu().getItems();
                     for (index in menuItems)
                     {
                        if (menuItems.hasOwnProperty(index))
                        {
                           if (menuItems[index].value === filterPreference)
                           {
                              this.widgets.user.set("label", menuItems[index].cfg.getProperty("text"));
                              break;
                           }
                        }
                     }
                  }
                  // Populate the activity list
                  this.populateActivityList(this.widgets.range.value, this.widgets.user.value);
               },
               scope: this
            },
            failureCallback:
            {
               fn: function()
               {
                  // Populate the activity list
                  this.populateActivityList(this.widgets.range.value, this.widgets.user.value);
               },
               scope: this
            }
         });
      },
      
      /**
       * Populate the activity list via Ajax request
       * @method populateActivityList
       */
      populateActivityList: function Activities_populateActivityList(dateFilter, userFilter)
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
         this.populateActivityList(this.widgets.range.value, this.widgets.user.value);
      },
      
      /**
       * Date drop-down changed event handler
       * @method onDateFilterChanged
       * @param p_oMenuItem {object} Selected menu item
       */
      onDateFilterChanged: function Activities_onDateFilterChanged(p_oMenuItem)
      {
         this.widgets.range.value = p_oMenuItem.value;
         this.populateActivityList(this.widgets.range.value, this.widgets.user.value);
         this.preferencesService.set(PREF_RANGE, this.widgets.range.value);
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
         this.preferencesService.set(PREF_FILTER, this.widgets.user.value);
      },
      
      /**
       * Exclusion button clicked event handler
       * @method onExclusionFilterClicked
       * @param p_oEvent {object} Dom event
       */
      onExclusionFilterClicked: function Activities_onExclusionFilterClicked(p_oEvent)
      {
         this.populateActivityList(this.widgets.range.value, this.widgets.user.value);
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
