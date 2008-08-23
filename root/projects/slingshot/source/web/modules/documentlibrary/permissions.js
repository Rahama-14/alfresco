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
 * Document Library "Permissions" module for Document Library.
 * 
 * @namespace Alfresco.module
 * @class Alfresco.module.DoclibPermissions
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


   Alfresco.module.DoclibPermissions = function(htmlId)
   {
      // Mandatory properties
      this.name = "Alfresco.module.DoclibPermissions";
      this.id = htmlId;

      // Initialise prototype properties
      this.widgets = {};
      this.modules = {};
      this.rolePickers = {};

      // Load YUI Components
      Alfresco.util.YUILoaderHelper.require(["button", "container", "connection", "json"], this.onComponentsLoaded, this);

      if (htmlId !== null)
      {
         // Register this component
         Alfresco.util.ComponentManager.register(this);
      }

      return this;
   };
   
   Alfresco.module.DoclibPermissions.prototype =
   {
      /**
       * Object container for initialization options
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
          * Available roles
          *
          * @property: roles
          * @type: array
          * @default: null
          */
         roles: null,

         /**
          * Files to be included in workflow
          *
          * @property: files
          * @type: array
          * @default: null
          */
         files: null,

         /**
          * Width for the dialog
          *
          * @property: width
          * @type: integer
          * @default: 40em
          */
         width: "40em"
      },
      
      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: null,

      /**
       * Object container for storing module instances.
       * 
       * @property modules
       * @type object
       */
      modules: null,

      /**
       * Object container for storing role picker UI elements.
       * 
       * @property rolePickers
       * @type object
       */
      rolePickers: null,

      /**
       * Container element for template in DOM.
       * 
       * @property containerDiv
       * @type DOMElement
       */
      containerDiv: null,

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.module.DoclibPermissions} returns 'this' for method chaining
       */
      setOptions: function DLP_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },

      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.module.DoclibPermissions} returns 'this' for method chaining
       */
      setMessages: function DLP_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function DLP_onComponentsLoaded()
      {
      },

      /**
       * Main entry point
       * @method showDialog
       */
      showDialog: function DLP_showDialog()
      {
         // DocLib Actions module
         if (!this.modules.actions)
         {
            this.modules.actions = new Alfresco.module.DoclibActions();
         }
         
         if (!this.containerDiv)
         {
            // Load the UI template from the server
            Alfresco.util.Ajax.request(
            {
               url: Alfresco.constants.URL_SERVICECONTEXT + "modules/documentlibrary/permissions",
               dataObj:
               {
                  htmlid: this.id,
                  site: this.options.siteId
               },
               successCallback:
               {
                  fn: this.onTemplateLoaded,
                  scope: this
               },
               failureMessage: "Could not load Document Library Permissions template",
               execScripts: true
            });
         }
         else
         {
            // Show the dialog
            this._showDialog();
         }
      },

      /**
       * Event callback when dialog template has been loaded
       *
       * @method onTemplateLoaded
       * @param response {object} Server response from load template XHR request
       */
      onTemplateLoaded: function DLP_onTemplateLoaded(response)
      {
         // Inject the template from the XHR request into a new DIV element
         this.containerDiv = document.createElement("div");
         this.containerDiv.setAttribute("style", "display:none");
         this.containerDiv.innerHTML = response.serverResponse.responseText;

         // The panel is created from the HTML returned in the XHR request, not the container
         var dialogDiv = Dom.getFirstChild(this.containerDiv);
         while (dialogDiv && dialogDiv.tagName.toLowerCase() != "div")
         {
            dialogDiv = Dom.getNextSibling(dialogDiv);
         }
         
         // Create and render the YUI dialog
         this.widgets.dialog = new YAHOO.widget.Panel(dialogDiv,
         {
            modal: true,
            draggable: false,
            fixedcenter: true,
            close: true,
            visible: false,
            width: this.options.width
         });
         this.widgets.dialog.render(document.body);
         
         // OK and cancel buttons
         this.widgets.okButton = Alfresco.util.createYUIButton(this, "ok", this.onOK);
         this.widgets.cancelButton = Alfresco.util.createYUIButton(this, "cancel", this.onCancel);
         
         // Mark-up the group/role drop-downs
         var roles = YAHOO.util.Selector.query('button.site-group', this.widgets.dialog.element);
         var roleElement, roleValue;
         for (var i = 0, j = roles.length; i < j; i++)
         {
            roleElementId = roles[i].id;
            roleValue = roles[i].value;
            this.rolePickers[roleValue] = new YAHOO.widget.Button(roleElementId,
            {
               type: "menu", 
               menu: roleElementId + "-select",
            });
            this.rolePickers[roleValue].getMenu().subscribe("click", this.onRoleSelected, this.rolePickers[roleValue]);
         }
         
         // Quick action buttons
         this.widgets.resetAll = Alfresco.util.createYUIButton(this, "reset-all", this.onResetAll);
         this.widgets.denyAll = Alfresco.util.createYUIButton(this, "deny-all", this.onDenyAll);
         this.widgets.allowAllMembers = Alfresco.util.createYUIButton(this, "allow-members-collaborate", this.onAllowAllMembers);

         // Show the dialog
         this._showDialog();
      },


      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */

      /**
       * Role menu item selected event handler
       *
       * @method onRoleSelected
       * @param e {object} DomEvent
       */
      onRoleSelected: function DLP_onRoleSelected(p_sType, p_aArgs, p_oButton)
      {
         var target = p_aArgs[1];
         p_oButton.set("label", target.cfg.getProperty("text"));
         p_oButton.set("name", target.value);
      },
      
      /**
       * Reset All button event handler
       *
       * @method onResetAll
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onResetAll: function DLP_onResetAll(e, p_obj)
      {
         this._applyPermissions("reset-all");
      },
      
      /**
       * Deny All button event handler
       *
       * @method onDenyAll
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onDenyAll: function DLP_onDenyAll(e, p_obj)
      {
         this._applyPermissions("deny-all");
      },
      
      /**
       * All All Members button event handler
       *
       * @method onAllowAllMembers
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onAllowAllMembers: function DLP_onAllowAllMembers(e, p_obj)
      {
         this._applyPermissions("allow-members-collaborate");
      },
      
      /**
       * Dialog OK button event handler
       *
       * @method onOK
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onOK: function DLP_onOK(e, p_obj)
      {
         // Generate data webscript parameters from UI elements
         var permissions = this._parseUI();
         this._applyPermissions("set", permissions);
      },
      
      /**
       * Apply permissions by calling data webscript with given operation
       *
       * @method _applyPermission
       * @param operation {string} set|reset-all|allow-members-collaborate|deny-all
       * @param params {object} Permission parameters
       */
      _applyPermissions: function DLP__applyPermissions(operation, permissions)
      {
         var files, multipleFiles = [];

         // Single/multi files into array of nodeRefs
         files = this.options.files;
         for (var i = 0, j = files.length; i < j; i++)
         {
            multipleFiles.push(files[i].nodeRef);
         }
         
         // Success callback function
         var fnSuccess = function DLP__onOK_success(p_data)
         {
            var result;
            var successCount = p_data.json.successCount;
            var failureCount = p_data.json.failureCount;
            
            this._hideDialog();

            // Did the operation succeed?
            if (!p_data.json.overallSuccess)
            {
               Alfresco.util.PopupManager.displayMessage(
               {
                  text: this._msg("message.permissions.failure")
               });
               return;
            }
            
            YAHOO.Bubbling.fire("filesPermissionsUpdated",
            {
               successCount: successCount,
               failureCount: failureCount
            });
            
            for (var i = 0, j = p_data.json.totalResults; i < j; i++)
            {
               result = p_data.json.results[i];
               
               if (result.success)
               {
                  YAHOO.Bubbling.fire(result.type == "folder" ? "folderPermissionsUpdated" : "filePermissionsUpdated",
                  {
                     multiple: true,
                     nodeRef: result.nodeRef
                  });
               }
            }
            
            Alfresco.util.PopupManager.displayMessage(
            {
               text: this._msg("message.permissions.success", successCount)
            });
         }
         
         // Failure callback function
         var fnFailure = function DLP__onOK_failure(p_data)
         {
            this._hideDialog();

            Alfresco.util.PopupManager.displayMessage(
            {
               text: this._msg("message.permissions.failure")
            });
         }

         // Construct the data object for the genericAction call
         this.modules.actions.genericAction(
         {
            success:
            {
               callback:
               {
                  fn: fnSuccess,
                  scope: this
               }
            },
            failure:
            {
               callback:
               {
                  fn: fnFailure,
                  scope: this
               }
            },
            webscript:
            {
               name: "permissions/{operation}/site/{site}",
               method: Alfresco.util.Ajax.POST
            },
            params:
            {
               site: this.options.siteId,
               operation: operation
            },
            config:
            {
               requestContentType: Alfresco.util.Ajax.JSON,
               dataObj:
               {
                  nodeRefs: multipleFiles,
                  permissions: permissions
               }
            }
         });

         this.widgets.okButton.set("disabled", true);
         this.widgets.cancelButton.set("disabled", true);
      },

      /**
       * Dialog Cancel button event handler
       *
       * @method onCancel
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onCancel: function DLP_onCancel(e, p_obj)
      {
         this._hideDialog();
      },


      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Internal show dialog function
       * @method _showDialog
       */
      _showDialog: function DLP__showDialog()
      {
         var i, j;
         
         // Enable buttons
         this.widgets.okButton.set("disabled", false);
         this.widgets.cancelButton.set("disabled", false);

         // Dialog title
         var titleDiv = Dom.get(this.id + "-title");
         if (YAHOO.lang.isArray(this.options.files))
         {
            titleDiv.innerHTML = this._msg("title.multi", this.options.files.length);
         }
         else
         {
            var fileSpan = '<span class="light">' + this.options.files.displayName + '</span>';
            titleDiv.innerHTML = this._msg("title.single", fileSpan);
            // Convert to array
            this.options.files = [this.options.files];
         }
         
         // Default values - "None" initially
         for (rolePicker in this.rolePickers)
         {
            this.rolePickers[rolePicker].set("name", "");
            this.rolePickers[rolePicker].set("label", this._msg("role.None"));
         }
         
         var defaultRoles = this.options.files[0].permissions.roles;
         var permissions;
         for (i = 0, j = defaultRoles.length; i < j; i++)
         {
            permissions = defaultRoles[i].split(";");
            if (permissions[2] in this.options.roles)
            {
               this.rolePickers[permissions[1]].set("name", permissions[2]);
               this.rolePickers[permissions[1]].set("label", this._msg("role." + permissions[2]));
            }
         }

         // Register the ESC key to close the dialog
         var escapeListener = new YAHOO.util.KeyListener(document,
         {
            keys: YAHOO.util.KeyListener.KEY.ESCAPE
         },
         {
            fn: function(id, keyEvent)
            {
               this.onCancel();
            },
            scope: this,
            correctScope: true
         });
         escapeListener.enable();

         // Show the dialog
         this.widgets.dialog.show();
      },

      /**
       * Hide the dialog, removing the caret-fix patch
       *
       * @method _hideDialog
       * @private
       */
      _hideDialog: function DLP__hideDialog()
      {
         // Grab the form element
         var formElement = Dom.get(this.id + "-form");

         // Undo Firefox caret issue
         Alfresco.util.undoCaretFix(formElement);
         this.widgets.dialog.hide();
      },

      /**
       * Parse the UI elements into a parameters object
       *
       * @method _parseUI
       * @return {object} Parameters ready for webscript execution
       * @private
       */
      _parseUI: function DLP__parseUI()
      {
         var params = [];
         var role;
         
         for (picker in this.rolePickers)
         {
            role = this.rolePickers[picker].get("name");
            if ((role != "") && (role != "None"))
            {
               params.push(
               {
                  group: this.rolePickers[picker].get("value"),
                  role: role
               });
            }
         }
         
         return params;
      },
      
      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
       _msg: function DLP__msg(messageId)
       {
          return Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
       }
   };
})();

/* Dummy instance to load optional YUI components early */
new Alfresco.module.DoclibPermissions(null);