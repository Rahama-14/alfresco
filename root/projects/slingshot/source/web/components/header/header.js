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
 * Global Header
 * 
 * @namespace Alfresco
 * @class Alfresco.Header
*/
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Element = YAHOO.util.Element,
      Event = YAHOO.util.Event;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   Alfresco.Header = function(htmlId)
   {
      this.name = "Alfresco.Header";
      this.id = htmlId;

      this.widgets = {};
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);
      
      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require([], this.onComponentsLoaded, this);
      
      // Notifications that the favourite sites have been updated
      YAHOO.Bubbling.on("favouriteSiteAdded", this.onFavouriteSiteAdded, this);
      YAHOO.Bubbling.on("favouriteSiteRemoved", this.onFavouriteSiteRemoved, this);
      YAHOO.Bubbling.on("siteDeleted", this.onSiteDeleted, this);

      this.preferencesService = new Alfresco.service.Preferences();
      return this;
   };

   Alfresco.Header.prototype =
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
          * Current siteId.
          * 
          * @property siteId
          * @type string
          * @default ""
          */
         siteId: "",

         /**
          * Current site title.
          * 
          * @property siteTitle
          * @type string
          * @default ""
          */
         siteTitle: "",

         /**
          * Current search type.
          * 
          * @property searchType
          * @type string
          * @default ""
          */
         searchType: "",
         
         /**
          * Favourite sites
          * 
          * @property favouriteSites
          * @type object
          * @default {}
          */
         favouriteSites: {}
      },
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.Header} returns 'this' for method chaining
       */
      setOptions: function Header_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.Header} returns 'this' for method chaining
       */
      setMessages: function Header_setMessages(obj)
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
      onComponentsLoaded: function Header_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);           
      },
      
      /**
       * Fired by YUI when parent element is available for scripting.
       * Initial History Manager event registration
       *
       * @method onReady
       */
      onReady: function Header_onReady()
      {
         Event.addListener(this.id + "-searchtext", "focus", this.focusSearchText, null, this);
         Event.addListener(this.id + "-searchtext", "blur", this.blurSearchText, null, this);
         Event.addListener(this.id + "-search-sbutton", "click", this.doSearch, null, this);
         
         this.defaultSearchText();
         
         // register the "enter" event on the search text field
         var zinput = Dom.get(this.id + "-searchtext");
         var me = this;
         new YAHOO.util.KeyListener(zinput, 
         {
            keys: 13
         }, 
         {
            fn: me.doSearch,
            scope: this,
            correctScope: true
         }, "keydown").enable();
                         
         var searchMenu = new YAHOO.widget.Menu(this.id + "-searchtogglemenu");
         searchMenu.render();
         searchMenu.owner = this;
         searchMenu.subscribe("show", searchMenu.focus);

         Event.addListener(this.id + "-search-tbutton", "click", this.openToggleSearchMenu, null, searchMenu);
         Dom.removeClass(this.id + "-searchtogglemenu", "hidden");

         var sitesMenu = new YAHOO.widget.Menu(this.id + "-sites-menu");
         sitesMenu.render();
         sitesMenu.subscribe("hide", this.onSitesMenuHide, this, true);
         this.widgets.sitesMenu = sitesMenu;

         var sitesButton = new YAHOO.widget.Button(this.id + "-sites",
         {
            type: "menu"
         });
         sitesButton.subscribe("click", this.onSitesMenuShow, this, true);
         
      },
      
      /**
       * Update image class when sarch box has focus.
       *
       * @method focusSearchText
       */
      focusSearchText: function Header_focusSearchText()
      {
         if (Dom.hasClass(this.id + "-searchtext", "gray"))
         {
            Dom.get(this.id + "-searchtext").value = "";
            Dom.removeClass(this.id + "-searchtext", "gray");
         }
         else
         {
            Dom.get(this.id + "-searchtext").select();
         }
      },
      
      /**
       * Set default search text when box loses focus and is empty.
       *
       * @method blurSearchText
       */
      blurSearchText: function Header_blurSearchText()
      {
         var searchVal = Dom.get(this.id + "-searchtext").value;
         if (searchVal.length == 0)
         {
            this.defaultSearchText();
         }
      }, 
      
      /**
       * Set default search text for search box.
       *
       * @method defaultSearchText
       */
      defaultSearchText: function Header_defaultSearchText()
      {
         Dom.get(this.id + "-searchtext").value = this._getToggleLabel(this.options.searchType);
         Dom.addClass(this.id + "-searchtext", "gray");
      },
      
      /**
       * Show the search options menu.
       *
       * @method openToggleSearchMenu
       */
      openToggleSearchMenu: function Header_openToggleSearchMenu()
      {
         this.show();
         var coord = Dom.getXY(this.owner.id + "-search-tbutton");
         coord[0] -= (Dom.get(this.owner.id + "-searchtogglemenu").offsetWidth - Dom.get(this.owner.id + "-search-tbutton").offsetWidth);
         coord[1] += Dom.get(this.owner.id + "-search-tbutton").offsetHeight;
         Dom.setXY(this.id, coord);          
      },
      
      /**
       * Change the search type.
       *
       * @method doToggleSearchType
       * @param newVal {string} New search type from user input
       */
      doToggleSearchType: function Header_doToggleSearchType(newVal)
      {
         this.options.searchType = newVal;
         this.defaultSearchText();
      },
      
      /**
       * Will trigger a search, via a page refresh to ensure the Back button works correctly
       *
       * @method doSearch
       */
      doSearch: function Header_doSearch()
      {
         var searchTerm = Dom.get(this.id + "-searchtext").value;
         if (searchTerm.length != 0)
         {
            var searchAll =  (this.options.searchType == "all");
            
            // redirect to the search page
            var url = Alfresco.constants.URL_CONTEXT + "page/";
            if (this.options.siteId.length != 0)
            {
               url += "site/" + this.options.siteId + "/";
            }
            url += "search?t=" + encodeURIComponent(searchTerm);
            if (this.options.siteId.length != 0)
            {
               url += "&a=" + searchAll;
            }
            window.location = url;
         }
      },
      
      /**
       * Returns the toggle label based on the passed-in search type
       *
       * @method _getToggleLabel
       * @param type {string} Search type
       * @return {string} i18n message corresponding to search type
       * @private
       */
      _getToggleLabel: function Header__getToggleLabel(type)
      {
         if (type == 'all')
         {
            return this._msg("header.search.searchall");
         }
         return this._msg("header.search.searchsite", this.options.siteTitle);
      },

      /**
       * Show the sites drop-down menu
       *
       * @method onSitesMenuShow
       * @param e {object} User generated event
       */
      onSitesMenuShow: function Header_onSitesMenuShow(e)
      {
         // todo: Replace this positioning code when we use YUI 2.6.0 with position: "dynamic" and context
         // Position the menu under the link-menu-button wrapper span
         var coord = Dom.getXY(this.id + "-sites-linkMenuButton");
         coord[1] += Dom.get(this.id + "-sites-linkMenuButton").offsetHeight;
         Dom.setXY(this.widgets.sitesMenu.id, coord);
         this.widgets.sitesMenu.show();
         this.widgets.sitesMenu.focus();

         // Add a selector the link-menu-button wrapper so we can put a border around bothe the link and the button
         Dom.addClass(this.id + "-sites-linkMenuButton", "link-menu-button-menu-active");
      },

      /**
       * Hide the sites drop-down menu
       *
       * @method onSitesMenuHide
       * @param e {object} User generated event
       */
      onSitesMenuHide: function Header_onSitesMenuHide(e)
      {
         Dom.removeClass(this.id + "-sites-linkMenuButton", "link-menu-button-menu-active");
      },

      /**
       * Show the create site dialog
       *
       * @method showCreateSite
       */
      showCreateSite: function Header_showCreateSite()
      {
         Alfresco.module.getCreateSiteInstance().show();
      },
      
      /**
       * Favourite Site has been added
       *
       * @method onFavouriteSiteAdded
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onFavouriteSiteAdded: function Header_onFavouriteSiteAdded(layer, args)
      {
         var obj = args[1];
         if (obj && obj.shortName !== null)
         {
            this.options.favouriteSites[obj.shortName] = obj.title;
            this._renderFavouriteSites();
         }
      },

      /**
       * Favourite Site has been removed
       *
       * @method onFavouriteSiteAdded
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onFavouriteSiteRemoved: function Header_onFavouriteSiteRemoved(layer, args)
      {
         var obj = args[1];
         if (obj && obj.shortName !== null)
         {
            if (obj.shortName in this.options.favouriteSites)
            {
               delete this.options.favouriteSites[obj.shortName];
               this._renderFavouriteSites();
            }
         }
      },

      /**
       * Site has been deleted - maybe remove from favourites menu
       *
       * @method onSiteDeleted
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onSiteDeleted: function Header_onSiteDeleted(layer, args)
      {
         var obj = args[1];
         if (obj && obj.site !== null)
         {
            if (obj.site.shortName in this.options.favouriteSites)
            {
               delete this.options.favouriteSites[obj.site.shortName];
               this._renderFavouriteSites();
            }
         }
      },

      /**
       * Renders favourite sites into menu
       *
       * @method renderFavouriteSites
       * @private
       */
      _renderFavouriteSites: function Header__renderFavouriteSites()
      {
         var sites = [], site, sitesMenu = this.widgets.sitesMenu, sitesGroup, i, ii;
         
         // Create a sorted list of our current favourites
         for (site in this.options.favouriteSites)
         {
            if (this.options.favouriteSites.hasOwnProperty(site))
            {
               sites.push(site);
            }
         }
         sites.sort();

         sitesGroup = sitesMenu.getItemGroups()[1];
         for (i = 0, ii = sitesGroup.length; i < ii; i++)
         {
            sitesMenu.removeItem(0, 1, true);
         }
         
         Dom.setStyle(this.id + "-favouritesContainer", "display", sites.length > 0 ? "block" : "none");
         Dom.setStyle(this.id + "-favouriteSites", "display", site.length > 0 ? "block" : "none");

         for (i = 0, ii = sites.length; i < ii; i++)
         {
            sitesMenu.addItem(
            {
               text: $html(this.options.favouriteSites[sites[i]]),
               url: Alfresco.util.uriTemplate("sitedashboardpage",
               {
                  site: sites[i]
               })
            }, 1);
         }
         
         // Show/hide "Add to favourites" menu item if we're in a site
         if (this.options.siteId !== "")
         {
            Dom.setStyle(this.id + "-addFavourite", "display", this.options.siteId in this.options.favouriteSites ? "none" : "block");
         }
         
         sitesMenu.render();
      },

      /**
       * Adds the current site as a favourite
       *
       * @method addAsFavourite
       */
      addAsFavourite: function Header_addAsFavourite()
      {
         var site =
         {
            shortName: this.options.siteId,
            title: this.options.siteTitle
         };

         var responseConfig =
         {
            failureCallback:
            {
               fn: function(event, obj)
               {
                  Alfresco.util.PopupManager.displayPrompt(
                  {
                     text: me._msg("message.siteFavourite.failure")
                  });
               },
               scope: this
            },
            successCallback:
            {
               fn: function(event, obj)
               {
                  YAHOO.Bubbling.fire("favouriteSiteAdded", obj.site);
                  // sitesMenu.removeItem(this.widgets.sitesMenu.activeItem.index, this.widgets.sitesMenu.activeItem.groupIndex, true);
               },
               scope: this,
               obj:
               {
                  site: site
               }
            }
         };

         this.preferencesService.set(
            Alfresco.service.Preferences.FAVOURITE_SITES + "." + site.shortName,
            true,
            responseConfig);
      },
      
      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function Header__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
      }      
   };
})();
