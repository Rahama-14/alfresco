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
 * Site Finder component.
 * 
 * @namespace Alfresco
 * @class Alfresco.SiteFinder
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
    * SiteFinder constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.SiteFinder} The new SiteFinder instance
    * @constructor
    */
   Alfresco.SiteFinder = function(htmlId)
   {
      this.name = "Alfresco.SiteFinder";
      this.id = htmlId;
      
      // initialise prototype properties
      this.widgets = {};
      this.buttons = [];
      this.modules = {};
      this.searchTerm = "";
      this.memberOfSites = {};
      this.membershipsRetrieved = false;
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "json"], this.onComponentsLoaded, this);

      YAHOO.Bubbling.on("siteDeleted", this.onSiteDeleted, this);

      return this;
   }
   
   Alfresco.SiteFinder.prototype =
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
          * Maximum number of results displayed.
          * 
          * @property maxResults
          * @type int
          * @default 100
          */
         maxResults: 100,
         
         /**
          * Flag to indicate whether private sites should be displayed
          * 
          * @property showPrivateSites
          * @type boolean
          * @default false
          */
         showPrivateSites: false,
         
         /**
          * The userid of the current user
          * 
          * @property currentUser
          * @type string
          */
         currentUser: ""
      },

      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: {},
      
      /**
       * List of Join/Leave buttons
       * 
       * @property buttons
       * @type array
       */
      buttons: [],

      /**
       * Object container for storing module instances.
       * 
       * @property modules
       * @type object
       */
      modules: {},

      /**
       * Search term used for the site search.
       * 
       * @property searchTerm
       * @type string
       */
      searchTerm: null,
      
      /**
       * List of sites the current user is a member of
       * 
       * @property memberOfSites
       * @type object
       */
      memberOfSites: {},
      
      /**
       * Flag to determine whether membership details have been
       * retrieved yet, until they have join/leave buttons can
       * not be shown
       * 
       * @property membershipsRetrieved
       * @type boolean
       */
      membershipsRetrieved: null,
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.Search} returns 'this' for method chaining
       */
      setOptions: function SiteFinder_setOptions(obj)
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
      setMessages: function SiteFinder_setMessages(obj)
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
      onComponentsLoaded: function SiteFinder_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },
      
      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function SiteFinder_onReady()
      {  
         var me = this;
         
         // build a list of sites the current user is a member of
         var config =
         {
            method: "GET",
            url: Alfresco.constants.PROXY_URI + "api/people/" + encodeURIComponent(this.options.currentUser) + "/sites",
            successCallback: 
            { 
               fn: this._processMembership, 
               scope: this 
            },
            failureMessage: me._msg("site-finder.no-membership-detail")
         };
         Alfresco.util.Ajax.request(config);
         
         // DataSource definition
         var uriSearchResults = Alfresco.constants.PROXY_URI + "api/sites?";
         this.widgets.dataSource = new YAHOO.util.DataSource(uriSearchResults);
         this.widgets.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
         this.widgets.dataSource.connXhrMode = "queueRequests";
         this.widgets.dataSource.responseSchema =
         {
             resultsList: "items",
             fields: ["url", "sitePreset", "shortName", "title", "description", "node", "tagScope", "isPublic", "button"]
         };
         this.widgets.dataSource.doBeforeParseData = function SiteFinder_doBeforeParseData(oRequest , oFullResponse)
         {
            var updatedResponse = oFullResponse;
               
            if (oFullResponse)
            {
               var items = [];
               
               // determine list of sites to show
               if (me.searchTerm.length == 0 && me.showPrivateSites)
               {
                  // if no search term and private sites are to be shown
                  // just pass response through
                  items = oFullResponse;
               }
               else
               {
                  for (var x = 0; x < oFullResponse.length; x++)
                  {
                     var siteData = oFullResponse[x];
                     var shortName = siteData.shortName;
                     var title = siteData.title;
                     var isPublic = siteData.isPublic;
                     
                     // Filter out private sites if necessary
                     if (me.options.showPrivateSites ||
                         (!me.options.showPrivateSites && isPublic))
                     {
                        // Determine if site matches search term
                        if (shortName.toLowerCase().indexOf(me.searchTerm.toLowerCase()) != -1 ||
                            title.toLowerCase().indexOf(me.searchTerm.toLowerCase()) != -1)
                        {
                           // add site to list
                           items.push(siteData);
                        }
                     }
                  }
               }

               // Sort the sites by their title
               items.sort(function (site1, site2)
               {
                  return (site1.title > site2.title) ? 1 : (site1.title < site2.title) ? -1 : 0;
               });

               // we need to wrap the array inside a JSON object so the DataTable is happy
               updatedResponse = {
                  "items": items
               };
            }
            
            return updatedResponse;
         }
         
         // setup of the datatable.
         this._setupDataTable();
         
         // setup the button
         this.widgets.searchButton = Alfresco.util.createYUIButton(this, "button", this.doSearch);
         
         // register the "enter" event on the search text field
         var searchIinput = Dom.get(this.id + "-term");
         new YAHOO.util.KeyListener(searchIinput, { keys:13 }, 
         {
            fn: function() 
            {
               me.doSearch()
            },
            scope:this,
            correctScope:true
         }, 
         "keydown" 
         ).enable();
         
         // Finally show the component body here to prevent UI artifacts on YUI button decoration
         Dom.setStyle(this.id + "-body", "visibility", "visible");
      },

      _processMembership: function SiteFinder__processMembership(response)
      {
         if (response.json.error === undefined)
         {
            var sites = response.json;
            for (var x = 0; x < sites.length; x++)
            {
               var site = sites[x];
               
               this.memberOfSites[site.shortName] = true;
            }
            
            // indicate that membership details have been received
            this.membershipsRetrieved = true;
         }
      },
      
      _setupDataTable: function SiteFinder_setupDataTable()
      {
         /**
          * DataTable Cell Renderers
          *
          * Each cell has a custom renderer defined as a custom function. See YUI documentation for details.
          * These MUST be inline in order to have access to the Alfresco.SiteFinder class (via the "me" variable).
          */
         var me = this;
          
         /**
          * Thumbnail custom datacell formatter
          *
          * @method renderCellThumbnail
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         renderCellThumbnail = function SiteFinder_renderCellThumbnail(elCell, oRecord, oColumn, oData)
         {
            var shortName = oRecord.getData("shortName");
            var url = Alfresco.constants.URL_PAGECONTEXT + "site/" + shortName + "/dashboard";
            var siteName = $html(oRecord.getData("title"));

            // Render the icon
            elCell.innerHTML = '<a href="' + url + '"><img src="' + 
               Alfresco.constants.URL_CONTEXT + '/components/site-finder/images/site-64.png' + 
               '" alt="' + siteName + '" title="' + siteName + '" /></a>';
         };

         /**
          * Description/detail custom datacell formatter
          *
          * @method renderCellDescription
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         renderCellDescription = function SiteFinder_renderCellDescription(elCell, oRecord, oColumn, oData)
         {
            var shortName = oRecord.getData("shortName");
            var url = Alfresco.constants.URL_PAGECONTEXT + "site/" + shortName + "/dashboard";         
            var title = oRecord.getData("title");
            var desc = oRecord.getData("description");
            var isPublic = oRecord.getData("isPublic");
            
            // title/link to site page
            var details = '<h3 class="sitename"><a href="' + url + '">' + $html(title) + '</a></h3>';
            // description
            details += '<div class="sitedescription">' + $html(desc) + '</div>';
            
            elCell.innerHTML = details;
         };
         
         /**
          * Actions custom datacell formatter
          *
          * @method renderCellActions
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         renderCellActions = function InvitationList_renderCellActions(elCell, oRecord, oColumn, oData)
         {
            if (me.membershipsRetrieved)
            {
               var isPublic = oRecord.getData("isPublic");
               if (isPublic)
               {
                  var shortName = oRecord.getData("shortName");
                  var title = $html(oRecord.getData("title"));
                  var action = '<span id="' + me.id + '-button-' + shortName + '"></span>';
                  if (shortName in me.memberOfSites)
                  {
                     action = '<span id="' + me.id + '-deleteButton-' + shortName + '"></span>&nbsp;' + action;
                  }
                  elCell.innerHTML = action;
                  
                  // create button
                  var button = new YAHOO.widget.Button(
                  {
                      container: me.id + '-button-' + shortName
                  });
                  
                  // if the user is already a member of the site show leave button
                  // otherwise show join button
                  if (shortName in me.memberOfSites)
                  {
                     // delete site button
                     var deleteButton = new YAHOO.widget.Button(
                     {
                         container: me.id + '-deleteButton-' + shortName
                     });
                     deleteButton.set("label", me._msg("site-finder.delete"));
                     deleteButton.set("onclick",
                     {
                        fn: me.doDelete,
                        obj:
                        {
                           shortName: shortName,
                           title: title
                        },
                        scope: me
                     });

                     // leave button
                     button.set("label", me._msg("site-finder.leave"));
                     button.set("onclick",
                     {
                        fn: me.doLeave,
                        obj:
                        {
                           shortName: shortName,
                           title: title
                        },
                        scope: me
                     });
                  }
                  else
                  {
                     button.set("label", me._msg("site-finder.join"));
                     button.set("onclick",
                     {
                        fn: me.doJoin,
                        obj:
                        {
                           shortName: shortName, 
                           title: title
                        },
                        scope: me
                     });
                  }
                  
                  me.buttons[shortName] =
                  {
                     button: button
                  };
               }
            }
            else
            {
               // output padding div so layout is not messed up due to missing buttons
               elCell.innerHTML = '<div></div>';
            }
         };

         // DataTable column defintions
         var columnDefinitions = [
         {
            key: "shortName", label: "Short Name", sortable: false, formatter: renderCellThumbnail
         },
         {
            key: "title", label: "Title", sortable: false, formatter: renderCellDescription
         },
         {
            key: "description", label: "Description", formatter: renderCellActions
         }
         ];

         // DataTable definition
         this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + "-sites", columnDefinitions, this.widgets.dataSource,
         {
            renderLoopSize: 32,
            initialLoad: false,
            MSG_EMPTY: this._msg("message.instructions")
         });
         this.widgets.dataTable.subscribe("rowDeleteEvent", this.onRowDeleteEvent, this, true);

         
         // Override abstract function within DataTable to set custom error message
         this.widgets.dataTable.doBeforeLoadData = function SiteFinder_doBeforeLoadData(sRequest, oResponse, oPayload)
         {
            if (oResponse.error)
            {
               try
               {
                  var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                  this.widgets.dataTable.set("MSG_ERROR", response.message);
               }
               catch(e)
               {
                  me._setDefaultDataTableErrors(me.widgets.dataTable);
               }
            }
            else if (oResponse.results)
            {
               if (oResponse.results.length == 0)
               {
                  me.widgets.dataTable.set("MSG_EMPTY", '<span style="white-space: nowrap;">' + me._msg("message.empty") + '</span>');
               }
               me.renderLoopSize = oResponse.results.length >> (YAHOO.env.ua.gecko) ? 3 : 5;
            }
            
            // Must return true to have the "Searching..." message replaced by the error message
            return true;
         }
      },
      
      /**
       * Search event handler
       *
       * @method doSearch
       */
      doSearch: function SiteFinder_doSearch()
      {
         this.searchTerm = Dom.get(this.id + "-term").value;
         this._performSearch(this.searchTerm);
      },
      
      /**
       * Join event handler
       *
       * @method doJoin
       * @param event {object} The event object
       * @param site {string} The shortName of the site to join
       */
      doJoin: function SiteFinder_doJoin(event, site)
      {
         var user = this.options.currentUser;
         
         // make ajax call to site service to join user
         Alfresco.util.Ajax.jsonRequest(
         {
            url: Alfresco.constants.PROXY_URI + "api/sites/" + site.shortName + "/memberships/" + encodeURIComponent(user),
            method: "PUT",
            dataObj:
            {
               role: "SiteConsumer",
               person:
               {
                  userName: user
               }
            },
            successCallback:
            {
               fn: this._joinSuccess,
               obj: site,
               scope: this
            },
            failureMessage: this._msg("site-finder.join-failure", this.options.currentUser, site.title)
         });
      },
      
      /**
       * Callback handler used when a user is successfully added to a site
       * 
       * @method _joinSuccess
       * @param response {object}
       * @param siteData {object}
       */
      _joinSuccess: function SiteFinder__joinSuccess(response, site)
      {
         // add site to site membership list
         this.memberOfSites[site.shortName] = true;
         
         // show popup message to confirm
         Alfresco.util.PopupManager.displayMessage(
         {
            text: this._msg("site-finder.join-success", this.options.currentUser, site.title)
         });
         
         // redo the search again to get updated info
         this.doSearch();
      },
      
      /**
       * Leave event handler
       * 
       * @method doLeave
       * @param event {object} The event object
       * @param site {string} The shortName of the site to leave
       */
      doLeave: function SiteFinder_doLeave(event, site)
      {
         var user = this.options.currentUser;
         
         // make ajax call to site service to join user
         Alfresco.util.Ajax.request(
         {
            url: Alfresco.constants.PROXY_URI + "api/sites/" + site.shortName + "/memberships/" + encodeURIComponent(user),
            method: "DELETE",
            successCallback:
            {
               fn: this._leaveSuccess,
               obj: site,
               scope: this
            },
            failureMessage: this._msg("site-finder.leave-failure", this.options.currentUser, site.title)
         });
      },
      
      /**
       * Callback handler used when a user is successfully removed from a site
       * 
       * @method _leaveSuccess
       * @param response {object}
       * @param siteData {object}
       */
      _leaveSuccess: function SiteFinder__leaveSuccess(response, site)
      {
         // remove site from site membership list
         delete this.memberOfSites[site.shortName];
         
         // show popup message to confirm
         Alfresco.util.PopupManager.displayMessage(
         {
            text: this._msg("site-finder.leave-success", this.options.currentUser, site.title)
         });
         
         // redo the search again to get updated info
         this.doSearch();
      },


      /**
       * Delete event handler
       *
       * @method doDelete
       * @param event {object} The event object
       * @param site {object} An object literal of the site to delete
       */
      doDelete: function SiteFinder_doDelete(event, site)
      {
         Alfresco.module.getDeleteSiteInstance().show({site: site});
      },

      /**
       * Resets the YUI DataTable errors to our custom messages
       *
       *
       * NOTE: Scope could be YAHOO.widget.DataTable, so can't use "this"
       *
       * @method _setDefaultDataTableErrors
       * @param dataTable {object} Instance of the DataTable
       */
      _setDefaultDataTableErrors: function SiteFinder__setDefaultDataTableErrors(dataTable)
      {
         var msg = Alfresco.util.message;
         dataTable.set("MSG_EMPTY", msg("message.empty", "Alfresco.SiteFinder"));
         dataTable.set("MSG_ERROR", msg("message.error", "Alfresco.SiteFinder"));
      },
      
      /**
       * Updates site list by calling data webscript with current search term
       *
       * @method _performSearch
       * @param searchTerm {string} The term to search for
       */
      _performSearch: function SiteFinder__performSearch(searchTerm)
      {
         // Reset the custom error messages
         this._setDefaultDataTableErrors(this.widgets.dataTable);
         
         // Display loading message
         this.widgets.dataTable.set("MSG_EMPTY", Alfresco.util.message("site-finder.searching", "Alfresco.SiteFinder"));
         
         // empty results table
         this.widgets.dataTable.deleteRows(0, this.widgets.dataTable.getRecordSet().getLength());
         
         function successHandler(sRequest, oResponse, oPayload)
         {
            this.searchTerm = searchTerm;
            this.widgets.dataTable.onDataReturnInitializeTable.call(this.widgets.dataTable, sRequest, oResponse, oPayload);
         }
         
         function failureHandler(sRequest, oResponse)
         {
            if (oResponse.status == 401)
            {
               // Our session has likely timed-out, so refresh to offer the login page
               window.location.reload();
            }
            else
            {
               try
               {
                  var response = YAHOO.lang.JSON.parse(oResponse.responseText);
                  this.widgets.dataTable.set("MSG_ERROR", response.message);
                  this.widgets.dataTable.showTableMessage(response.message, YAHOO.widget.DataTable.CLASS_ERROR);
               }
               catch(e)
               {
                  this._setDefaultDataTableErrors(this.widgets.dataTable);
               }
            }
         }
         
         this.widgets.dataSource.sendRequest(this._buildSearchParams(searchTerm),
         {
               success: successHandler,
               failure: failureHandler,
               scope: this
         });
      },

      /**
       * Build URI parameter string for finding sites
       *
       * @method _buildSearchParams
       * @param searchTerm {string} Path to query
       */
      _buildSearchParams: function SiteFinder__buildSearchParams(searchTerm)
      {
         var params = YAHOO.lang.substitute("size={maxResults}",
         {
            maxResults : this.options.maxResults
         });

         return params;
      },


      /**
       * Fired any another component, DeleteSite, to let other components know
       * that a site has been deleted.
       * Performs the search again.
       *
       * @method onSiteDeleted
       * @param layer {object} Event fired (unused)
       * @param args {array} Event parameters (unused)
       */
      onSiteDeleted: function SiteFinder_onSiteDeleted(layer, args)
      {
         var site = args[1].site;
         var rs = this.widgets.dataTable.getRecordSet();
         var length = rs.getLength();
         for (var i = 0; i < length; i++)
         {
            var record = rs.getRecord(i);
            if(record.getData("shortName") == site.shortName)
            {
               this.widgets.dataTable.deleteRow(record);
            }
         }

      },

      /**
       * Fired by YUI:s DataTable when a row has been added to the data table list.
       * Keeps track of added files.
       *
       * @method onRowDeleteEvent
       * @param event {object} a DataTable "rowDelete" event
       */
      onRowDeleteEvent: function SiteFinder_onRowDeleteEvent(event)
      {
         if (this.widgets.dataTable.getRecordSet().getLength() === 0)
         {
            this.widgets.dataTable.showTableMessage(this._msg("site-finder.enter-search-term", this.name), "siteFinderTableMessage");
         }
      },

      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function SiteFinder__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.SiteFinder", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();