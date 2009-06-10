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
 * GroupFinder component.
 * 
 * @namespace Alfresco
 * @class Alfresco.GroupFinder
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
    * GroupFinder constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.GroupFinder} The new GroupFinder instance
    * @constructor
    */
   Alfresco.GroupFinder = function(htmlId)
   {
      this.name = "Alfresco.GroupFinder";
      this.id = htmlId;
      
      // Initialise prototype properties
      this.widgets = {};
      this.itemSelectButtons = {};
      this.searchTerm = "";
      this.singleSelectedItem = "";
      this.selectedItems = {};

      // Register this component
      Alfresco.util.ComponentManager.register(this);

      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "json"], this.onComponentsLoaded, this);
   
      /**
       * Decoupled event listeners
       */
      YAHOO.Bubbling.on("itemDeselected", this.onItemDeselected, this);

      return this;
   };
   
   YAHOO.lang.augmentObject(Alfresco.GroupFinder,
   {
      VIEW_MODE_DEFAULT: "",
      VIEW_MODE_COMPACT: "COMPACT",
      VIEW_MODE_FULLPAGE: "FULLPAGE"
   });
   
   Alfresco.GroupFinder.prototype =
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
          */
         siteId: "",

         /**
          * View mode
          * 
          * @property viewMode
          * @type string
          * @default Alfresco.GroupFinder.VIEW_MODE_DEFAULT
          */
         viewMode: Alfresco.GroupFinder.VIEW_MODE_DEFAULT,

         /**
          * Single Select mode flag
          * 
          * @property singleSelectMode
          * @type boolean
          * @default false
          */
         singleSelectMode: false,
         
         /**
          * Number of characters required for a search.
          * 
          * @property minSearchTermLength
          * @type number
          * @default 3
          */
         minSearchTermLength: 3,
         
         /**
          * Maximum number of items to display in the results list
          * 
          * @property maxSearchResults
          * @type number
          * @default 100
          */
         maxSearchResults: 100,
         
         /**
          * If true, then automatically prefix a wildcard character to the search term
          * 
          * @property wildcardPrefix
          * @type boolean
          * @default false
          */
         wildcardPrefix: false
      },

      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: null,
      
      /**
       * Object container for storing YUI button instances, indexed by groupname.
       * 
       * @property itemSelectButtons
       * @type object
       */
      itemSelectButtons: null,
      
      /**
       * Current search term, obtained from form input field.
       * 
       * @property searchTerm
       * @type string
       */
      searchTerm: null,
      
      /**
       * Single selected item, for when in single select mode
       * 
       * @property singleSelectedItem
       * @type string
       */
      singleSelectedItem: null,

      /**
       * Selected items. Keeps a list of selected items for correct Add button state.
       * 
       * @property selectedItems
       * @type object
       */
      selectedItems: null,

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.GroupFinder} returns 'this' for method chaining
       */
      setOptions: function GroupFinder_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.GroupFinder} returns 'this' for method chaining
       */
      setMessages: function GroupFinder_setMessages(obj)
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
      onComponentsLoaded: function GroupFinder_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function GroupFinder_onReady()
      {  
         var me = this;
         
         // View mode
         if (this.options.viewMode == Alfresco.GroupFinder.VIEW_MODE_COMPACT)
         {
            Dom.addClass(this.id + "-body", "compact");
         }
         else if (this.options.viewMode == Alfresco.GroupFinder.VIEW_MODE_FULLPAGE)
         {
            Dom.setStyle(this.id + "-results", "height", "auto");
         }
         else
         {
            Dom.setStyle(this.id + "-results", "height", "300px");
         }
         
         // Search button
         this.widgets.searchButton = Alfresco.util.createYUIButton(this, "group-search-button", this.onSearchClick);

         // DataSource definition  
         this.widgets.dataSource = new YAHOO.util.DataSource(Alfresco.constants.PROXY_URI + "api/groups?includeInternal=false&");
         this.widgets.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
         this.widgets.dataSource.connXhrMode = "queueRequests";
         this.widgets.dataSource.responseSchema =
         {
             resultsList: "data",
             fields: ["shortName", "fullName", "displayName", "userCount", "groupCount"]
         };

         this.widgets.dataSource.doBeforeParseData = function GroupFinder_doBeforeParseData(oRequest, oFullResponse)
         {
            var updatedResponse = oFullResponse;
            
            if (oFullResponse && oFullResponse.data)
            {
               var items = oFullResponse.data;
               
               // crop item list to max length if required
               if (items.length > me.options.maxSearchResults)
               {
                  items = items.slice(0, me.options.maxSearchResults - 1);
               }

               // we need to wrap the array inside a JSON object so the DataTable is happy
               updatedResponse =
               {
                  data: items
               };
            }
            
            return updatedResponse;
         };
         
         // Setup the DataTable
         this._setupDataTable();
         
         // register the "enter" event on the search text field
         var searchText = Dom.get(this.id + "-search-text");
         
         // declare variable to keep JSLint and YUI Compressor happy
         var enterListener = new YAHOO.util.KeyListener(searchText,
         {
            keys: YAHOO.util.KeyListener.KEY.ENTER
         },
         {
            fn: function() 
            {
               me.onSearchClick();
            },
            scope: this,
            correctScope: true
         }, "keydown");
         enterListener.enable();
         
         // Set initial focus
         searchText.focus();
      },
      
      /**
       * Setup the YUI DataTable with custom renderers.
       *
       * @method _setupDataTable
       * @private
       */
      _setupDataTable: function GroupFinder__setupDataTable()
      {
         /**
          * DataTable Cell Renderers
          *
          * Each cell has a custom renderer defined as a custom function. See YUI documentation for details.
          * These MUST be inline in order to have access to the Alfresco.GroupFinder class (via the "me" variable).
          */
         var me = this;
          
         /**
          * Icon custom datacell formatter
          *
          * @method renderCellIcon
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellIcon = function GroupFinder_renderCellIcon(elCell, oRecord, oColumn, oData)
         {
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

            elCell.innerHTML = '<img class="avatar" src="' + Alfresco.constants.URL_CONTEXT + 'components/images/group-64.png" alt="avatar" />';
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
         var renderCellDescription = function GroupFinder_renderCellDescription(elCell, oRecord, oColumn, oData)
         {
            var displayName = oRecord.getData("displayName");
            
            var desc = '<h3 class="itemname">' + $html(displayName) + '</h3>';
            if (me.options.viewMode !== Alfresco.GroupFinder.VIEW_MODE_COMPACT)
            {
               desc += '<div class="detail"><span>' + me._msg("label.name") + ":</span> " + $html(oRecord.getData("fullName")) + '</div>';
               desc += '<div class="detail">';
               desc += '<span class="item"><span>' + me._msg("label.users") + ":</span> " + $html(oRecord.getData("userCount")) + '</span>';
               desc += '<span class="item"><span>' + me._msg("label.subgroups") + ":</span> " + $html(oRecord.getData("groupCount")) + '</span>';
               desc += '</div>';
            }
            elCell.innerHTML = desc;
         };
         
         /**
          * Add button datacell formatter
          *
          * @method renderCellIcon
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellAddButton = function GroupFinder_renderCellAddButton(elCell, oRecord, oColumn, oData)
         {
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");
            Dom.setStyle(elCell.parentNode, "text-align", "right");
            
            var itemName = oRecord.getData("shortName");
            var desc = '<span id="' + me.id + '-select-' + itemName + '"></span>';
            elCell.innerHTML = desc;
            
            // create button if require - it is not required in the fullpage view mode
            if (me.options.viewMode !== Alfresco.GroupFinder.VIEW_MODE_FULLPAGE)
            {
               var button = new YAHOO.widget.Button(
               {
                  type: "button",
                  label: me._msg("button.add") + " >>",
                  name: me.id + "-selectbutton-" + itemName,
                  container: me.id + '-select-' + itemName,
                  onclick:
                  {
                     fn: me.onItemSelect,
                     obj: oRecord,
                     scope: me
                  }
               });
               me.itemSelectButtons[itemName] = button;
               
               if ((itemName in me.selectedItems) || (me.options.singleSelectMode && me.singleSelectedItem !== ""))
               {
                  me.itemSelectButtons[itemName].set("disabled", true);
               }
            }
         };

         // DataTable column defintions
         var columnDefinitions = [
         {
            key: "icon", label: "Icon", sortable: false, formatter: renderCellIcon, width: this.options.viewMode == Alfresco.GroupFinder.VIEW_MODE_COMPACT ? 36 : 70
         },
         {
            key: "description", label: "Description", sortable: false, formatter: renderCellDescription
         },
         {
            key: "actions", label: "Actions", sortable: false, formatter: renderCellAddButton, width: 80
         }];

         // DataTable definition
         this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + "-results", columnDefinitions, this.widgets.dataSource,
         {
            renderLoopSize: 32,
            initialLoad: false,
            MSG_EMPTY: this._msg("message.instructions")
         });

         this.widgets.dataTable.doBeforeLoadData = function GroupFinder_doBeforeLoadData(sRequest, oResponse, oPayload)
         {
            if (oResponse.results)
            {
               this.renderLoopSize = oResponse.results.length >> (YAHOO.env.ua.gecko === 1.8) ? 3 : 5;
            }
            return true;
         };

         // Enable row highlighting
         this.widgets.dataTable.subscribe("rowMouseoverEvent", this.widgets.dataTable.onEventHighlightRow);
         this.widgets.dataTable.subscribe("rowMouseoutEvent", this.widgets.dataTable.onEventUnhighlightRow);
      },
      
      /**
       * Public function to clear the results DataTable
       */
      clearResults: function GroupFinder_clearResults()
      {
         // Clear results DataTable
         if (this.widgets.dataTable)
         {
            var recordCount = this.widgets.dataTable.getRecordSet().getLength();
            this.widgets.dataTable.deleteRows(0, recordCount);
         }
         Dom.get(this.id + "-search-text").value = "";
         this.singleSelectedItem = "";
         this.selectedItems = {};
      },


      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */

      /**
       * Select person button click handler
       *
       * @method onItemSelect
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onItemSelect: function GroupFinder_onItemSelect(event, p_obj)
      {
         var itemName = p_obj.getData("fullName");
         
         // Fire the personSelected bubble event
         YAHOO.Bubbling.fire("itemSelected",
         {
            itemName: itemName,
            displayName: p_obj.getData("displayName")
         });
         
         // Add the userName to the selectedItems object
         this.selectedItems[itemName] = true;
         this.singleSelectedItem = itemName;
         
         // Disable the add button(s)
         if (this.options.singleSelectMode)
         {
            for (var button in this.itemSelectButtons)
            {
               if (this.itemSelectButtons.hasOwnProperty(button))
               {
                  this.itemSelectButtons[button].set("disabled", true);
               }
            }
         }
         else
         {
            this.itemSelectButtons[itemName].set("disabled", true);
         }
      },

      /**
       * Search button click event handler
       *
       * @method onSearchClick
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onSearchClick: function GroupFinder_onSearchClick(e, p_obj)
      {
         var searchTerm = Dom.get(this.id + "-search-text").value;
         if (searchTerm.length < this.options.minSearchTermLength)
         {
            Alfresco.util.PopupManager.displayMessage(
            {
               text: this._msg("message.minimum-length", this.options.minSearchTermLength)
            });
            return;
         }
         
         this.itemSelectButtons = {};
         this._performSearch(searchTerm);
      },


      /**
       * BUBBLING LIBRARY EVENT HANDLERS FOR PAGE EVENTS
       * Disconnected event handlers for inter-component event notification
       */

      /**
       * Item Deselected event handler
       *
       * @method onItemDeselected
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onItemDeselected: function DL_onItemDeselected(layer, args)
      {
         var obj = args[1];
         // Should be item details in the arguments
         if (obj && (obj.itemName !== null))
         {
            delete this.selectedItems[obj.itemName];
            this.singleSelectedItem = "";
            // Re-enable the add button(s)
            if (this.options.singleSelectMode)
            {
               for (var button in this.itemSelectButtons)
               {
                  if (this.itemSelectButtons.hasOwnProperty(button))
                  {
                     this.itemSelectButtons[button].set("disabled", false);
                  }
               }
            }
            else
            {
               this.itemSelectButtons[obj.itemName].set("disabled", false);
            }
         }
      },


      /**
       * PRIVATE FUNCTIONS
       */
      
      /**
       * Resets the YUI DataTable errors to our custom messages
       * NOTE: Scope could be YAHOO.widget.DataTable, so can't use "this"
       *
       * @method _setDefaultDataTableErrors
       * @param dataTable {object} Instance of the DataTable
       */
      _setDefaultDataTableErrors: function GroupFinder__setDefaultDataTableErrors(dataTable)
      {
         var msg = Alfresco.util.message;
         dataTable.set("MSG_EMPTY", msg("message.empty", "Alfresco.GroupFinder"));
         dataTable.set("MSG_ERROR", msg("message.error", "Alfresco.GroupFinder"));
      },
      
      /**
       * Updates results list by calling data webscript
       *
       * @method _performSearch
       * @param searchTerm {string} Search term from input field
       */
      _performSearch: function GroupFinder__performSearch(searchTerm)
      {
         // Reset the custom error messages
         this._setDefaultDataTableErrors(this.widgets.dataTable);
         
         // Don't display any message
         this.widgets.dataTable.set("MSG_EMPTY", this._msg("message.searching"));
         
         // Empty results table
         this.widgets.dataTable.deleteRows(0, this.widgets.dataTable.getRecordSet().getLength());
         this.widgets.dataTable.render();
         
         var successHandler = function GroupFinder__pS_successHandler(sRequest, oResponse, oPayload)
         {
            this._setDefaultDataTableErrors(this.widgets.dataTable);
            this.widgets.dataTable.onDataReturnInitializeTable.call(this.widgets.dataTable, sRequest, oResponse, oPayload);
         };
         
         var failureHandler = function GroupFinder__pS_failureHandler(sRequest, oResponse)
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
         };
         
         this.searchTerm = searchTerm;
         this.widgets.dataSource.sendRequest(this._buildSearchParams(searchTerm),
         {
            success: successHandler,
            failure: failureHandler,
            scope: this
         });
      },

      /**
       * Build URI parameter string for Group Finder JSON data webscript
       *
       * @method _buildSearchParams
       * @param searchTerm {string} Search terms to query
       */
      _buildSearchParams: function GroupFinder__buildSearchParams(searchTerm)
      {
         return "shortNameFilter=" + (this.options.wildcardPrefix ? "*" : "") + encodeURIComponent(searchTerm);
      },
      
      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function GroupFinder__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.GroupFinder", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();