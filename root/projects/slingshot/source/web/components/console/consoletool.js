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
 * ConsoleTool tool base component.
 * Provides common functionality for all Console Panel based tools.
 * 
 * @namespace Alfresco
 * @class Alfresco.ConsoleTool
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
       Event = YAHOO.util.Event;
   
   /**
    * ConsoleTool constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.ConsoleTool} The new ConsoleTool instance
    * @constructor
    */
   Alfresco.ConsoleTool = function(htmlId)
   {
      this.id = htmlId;
      
      /* History navigation event */
      YAHOO.Bubbling.on("stateChanged", this.onStateChanged, this);
      
      /* Define panel handlers */
      var parent = this;

      Alfresco.ConsolePanelHandler = function(id)
      {
         this.id = id;
         this.widgets = {};
         this.modules = {};
         this.forms = {};

         // register the panel with the parent object
         parent.panels.push(this);
      };
      
      /** Alfresco.ConsolePanelHandler prototype */
      Alfresco.ConsolePanelHandler.prototype =
      {
         id : null,

         /**
          * Object container for storing YUI widget instances.
          *
          * @property widgets
          * @type object
          */
         widgets: {},

         /**
          * Object container for storing module instances.
          *
          * @property modules
          * @type object
          */
         modules: {},

         /**
          * Object container for storing form instances.
          *
          * @property forms
          * @type object
          */
         forms: {},

         /**
          * Event handler - called once only when panel first initialised
          * @method onLoad
          */
         onLoad: function onLoad()
         {
         },
         
         /**
          * Event handler - called just before panel is going to be made visible
          * @method onBeforeShow
          */
         onBeforeShow: function onBeforeShow()
         {
         },
         
         /**
          * Event handler - called after the panel has been made visible
          * @method onShow
          */
         onShow: function onShow()
         {
         },
         
         /**
          * Event handler - called to request the panel update it's current state
          * @method onUpdate
          */
         onUpdate: function onUpdate()
         {
         },
         
         /**
          * Event handler - called after the panel has been made invisible
          * @method onHide
          */
         onHide: function onHide()
         {
         }
      };
      
      return this;
   }
   
   Alfresco.ConsoleTool.prototype =
   {
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: {},
      
      /**
       * Object container for storing module instances.
       * 
       * @property modules
       * @type object
       */
      modules: {},
      
      /**
       * Object container for storing YUI pop dialog instances.
       * 
       * @property popups
       * @type object
       */
      popups: {},
      
      /**
       * List of the available UI panel handler objects; such as Search, View, Edit etc.
       * 
       * @property panels
       * @type array
       */
      panels: [],
      
      /**
       * The current UI panel ID on display
       * 
       * @property currentPanelId
       * @type string
       */
      currentPanelId: "",
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.ConsoleTool} returns 'this' for method chaining
       */
      setOptions: function ConsoleTool_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.ConsoleTool} returns 'this' for method chaining
       */
      setMessages: function ConsoleTool_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },
   
      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function ConsoleTool_onReady()
      {
         // YUI History
         var bookmarkedState = YAHOO.util.History.getBookmarkedState("state") || this.encodeHistoryState({"panel": this.panels[0].id});
         
         // Register History Manager callbacks
         YAHOO.util.History.register("state", bookmarkedState, function CU_onHistoryManagerStateChanged(newState)
         {
            YAHOO.Bubbling.fire("stateChanged",
            {
               state: newState
            });
         });
         
         // Continue only when History Manager fires its onReady event
         YAHOO.util.History.onReady(this.onHistoryManagerReady, this, true);
         
         // Initialize the browser history management library
         try
         {
             YAHOO.util.History.initialize("yui-history-field", "yui-history-iframe");
         }
         catch(e)
         {
            /*
             * The only exception that gets thrown here is when the browser is
             * not supported (Opera, or not A-grade)
             */
            Alfresco.logger.error("Alfresco.ConsoleTool: Couldn't initialize HistoryManager.", e);
            this.onHistoryManagerReady();
         }
      },
      
      /**
       * Fired by YUI when History Manager is initialised and available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onHistoryManagerReady
       */
      onHistoryManagerReady: function ConsoleTool_onHistoryManagerReady()
      {
         // Fire the onLoad() panel lifecycle event for each registered panel
         // To perform one-off setup of contained widgets and internal event handlers
         for (var i in this.panels)
         {
            this.panels[i].onLoad();
         }
         
         // display the initial panel based on history state or default
         var bookmarkedState = YAHOO.util.History.getBookmarkedState("state") || this.encodeHistoryState({"panel": this.panels[0].id});
         YAHOO.Bubbling.fire("stateChanged",
         {
            state: bookmarkedState
         });
      },
      
      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */
      
      /**
       * History manager state change event handler
       *
       * @method onStateChanged
       * @param e {object} DomEvent
       * @param args {array} Event parameters (depends on event type)
       */
      onStateChanged: function ConsoleUsers_onStateChanged(e, args)
      {
         var state = this.decodeHistoryState(args[1].state);
         
         // test if panel has actually changed?
         if (state.panel)
         {
            this.showPanel(state.panel);
         }
      },
      
      /**
       * Decode packed URL state into its component name value pairs.
       * 
       * @method decodeHistoryState
       * @param state {string} packed state from the url history
       * @private
       */
      decodeHistoryState: function ConsoleTool_decodeHistoryState(state)
      {
         var obj = {};
         var pairs = state.split("&");
         for (var i=0; i<pairs.length; i++)
         {
            var pair = pairs[i].split("=");
            obj[pair[0]] = decodeURIComponent(pair[1]);
         }
         return obj;
      },
      
      /**
       * Encode state object into a packed string for use as url history value.
       * 
       * @method encodeHistoryState
       * @param obj {object} state object
       * @private
       */
      encodeHistoryState: function ConsoleTool_encodeHistoryState(obj)
      {
         // wrap up current state values
         var stateObj = {};
         if (this.currentPanelId !== "")
         {
            stateObj.panel = this.currentPanelId;
         }
         
         // convert to encoded url history state - overwriting with any supplied values
         var state = "";
         if (obj.panel || stateObj.panel)
         {
            state += "panel=" + encodeURIComponent(obj.panel ? obj.panel : stateObj.panel);
         }
         return state;
      },
      
      /**
       * PRIVATE FUNCTIONS
       */
      
      /**
       * Make the specified panel visible - hiding any others and firing
       * the various Panel events as we go.
       * 
       * @method showPanel
       * @param panelId {string} ID of the panel to make visible
       * @private
       */
      showPanel: function ConsoleTool_showPanel(panelId)
      {
         if (this.currentPanelId !== panelId)
         {
            this.currentPanelId = panelId;
            var newPanel = null;
            for (var index in this.panels)
            {
               var panel = this.panels[index];
               if (panel.id === panelId)
               {
                  newPanel = panel;
               }
               else
               {
                  Dom.setStyle(this.id + "-" + panel.id, "display", "none");
                  
                  // Fire the onHide() panel lifecycle event
                  panel.onHide();
               }
            }
            
            if (newPanel != null)
            {
               // Fire the onBeforeShow() panel lifecycle event
               newPanel.onBeforeShow();
               
               // Display the specified panel to the user
               Dom.setStyle(this.id + "-" + panelId, "display", "block");
               
               // Fire the onShow() panel lifecycle event
               newPanel.onShow();
            }
         }
      },
      
      /**
       * Return the object representing the currently visible panel.
       * 
       * @method _getCurrentPanel
       * @return the currently visible panel object
       * @private
       */
      _getCurrentPanel: function ConsoleTool__getCurrentPanel()
      {
         var panel = null;
         for (var index in this.panels)
         {
            if (this.panels[index].id === this.currentPanelId)
            {
               panel = this.panels[index];
               break;
            }
         }
         return panel;
      },
      
      /**
       * Fire an onUpdate() event to the currently visible panel.
       * 
       * @method updateCurrentPanel
       * @private
       */
      updateCurrentPanel: function ConsoleTool_updateCurrentPanel()
      {
         for (var i in this.panels)
         {
            if (this.panels[i].id === this.currentPanelId)
            {
               this.panels[i].onUpdate();
               break;
            }
         }
      },
      
      /**
       * Refresh the UI based on the given state object, can contain any or all of the
       * state object properties, for example from the Users tool:
       * {
       *    panel: thepanelid,
       *    userid: theuserid,
       *    search: thesearchterm
       * }
       * 
       * @method refreshUIState
       * @param state {object} UI state object, see above
       * @private
       */
      refreshUIState: function ConsoleTool_refreshUIState(state)
      {
         YAHOO.util.History.navigate("state", this.encodeHistoryState(state));
      }
   };
})();