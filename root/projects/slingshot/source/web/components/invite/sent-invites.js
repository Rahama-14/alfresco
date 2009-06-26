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
 * SentInvites component.
 * 
 * @namespace Alfresco
 * @class Alfresco.SentInvites
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
    * SentInvites constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.SentInvites} The new SentInvites instance
    * @constructor
    */
   Alfresco.SentInvites = function(htmlId)
   {
      /* Mandatory properties */
      this.name = "Alfresco.SentInvites";
      this.id = htmlId;
      
      /* Initialise prototype properties */
      this.widgets = {};
      this.actionButtons = {};
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "json"], this.onComponentsLoaded, this);

      return this;
   };
   
   Alfresco.SentInvites.prototype =
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
          * Whether to set UI focus to this component or not
          * 
          * @property setFocus
          * @type boolean
          * @default false
          */
         setFocus: false
      },

      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: null,
      
      /**
       * Object container for storing YUI button instances, indexed by username.
       * 
       * @property userSelectButtons
       * @type object
       */
      actionButtons: null,
      
      /**
       * Current search term, obtained from form input field.
       * 
       * @property searchTerm
       * @type string
       */
      searchTerm: "",

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.SentInvites} returns 'this' for method chaining
       */
      setOptions: function SentInvites_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.SentInvites} returns 'this' for method chaining
       */
      setMessages: function SentInvites_setMessages(obj)
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
      onComponentsLoaded: function SentInvites_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function SentInvites_onReady()
      {  
         var me = this;

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

         // Set initial focus?
         if (this.options.setFocus)
         {
            searchText.focus();
         }

         // Search button
         this.widgets.searchButton = Alfresco.util.createYUIButton(this, "search-button", this.onSearchClick);

         // DataSource definition  
         var inviteeSearchUrl = Alfresco.constants.PROXY_URI + "api/invites?";
         this.widgets.dataSource = new YAHOO.util.DataSource(inviteeSearchUrl);
         this.widgets.dataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
         this.widgets.dataSource.responseSchema =
         {
             resultsList: "invites",
             fields: ["inviteId", "inviter", "invitee", "siteShortName", "invitationStatus", "role", "sentInviteDate"]
         };

         this.widgets.dataSource.doBeforeParseData = function SentInvites_doBeforeParseData(oRequest, oFullResponse)
         {
            var updatedResponse = oFullResponse;
               
            if (oFullResponse)
            {
               var items = [];
               
               // Determine list of sites to show
               if (me.searchTerm.length > 0)
               {
                  // Filter the results for the search term
                  var lowerCaseTerm = me.searchTerm.toLowerCase();
                  var personData, userName, firstName, lastName, fullName;
                  for (var i = 0, j = oFullResponse.invites.length; i < j; i++)
                  {
                     personData = oFullResponse.invites[i].invitee;
                     userName = (personData.userName || "").toLowerCase();
                     firstName = (personData.firstName || "").toLowerCase();
                     lastName = (personData.lastName || "").toLowerCase();
                     fullName = (firstName + " " + lastName).toLowerCase();
                     
                     // Determine if person matches search term
                     if ((userName.indexOf(lowerCaseTerm) != -1)
                        || (firstName.indexOf(lowerCaseTerm) != -1)
                        || (lastName.indexOf(lowerCaseTerm) != -1)
                        || (fullName.indexOf(lowerCaseTerm) != -1))
                     {
                        // Add user to list
                        items.push(oFullResponse.invites[i]);
                     }
                  }
               }
               else
               {
                  items = oFullResponse.invites;
               }

               // Sort the invites list by the invitee's name
               items.sort(function (invite1, invite2){
                  var name1 = invite1.invitee.firstName + invite1.invitee.lastName,
                     name2 = invite2.invitee.firstName + invite2.invitee.lastName;
                  return (name1 > name2) ? 1 : (name1 < name2) ? -1 : 0;
               });

               // we need to wrap the array inside a JSON object so the DataTable is happy
               updatedResponse =
               {
                  "invites": items
               };
            }
            
            return updatedResponse;
         };
         
         // Setup the DataTable
         this._setupDataTable();

         // Set initial focus?
         if (this.options.setFocus)
         {
            Dom.get(this.id + "-search-text").focus();
         }

         /*
          * Enter key listener function needs to be enclosed due to having "window" scope
          *
          * @method: onKeyEnter
          * @param id
          * @param keyEvent {object} The key event details
          */
         var onKeyEnter = function SentInvites_onKeyEnter(id, keyEvent)
         {
            me.onSearchClick.call(me, keyEvent, null);
            return false;
         };

         // Enter key listener
         var enterListener = new YAHOO.util.KeyListener(this.id + "-search-button",
         {
            keys: YAHOO.util.KeyListener.KEY.ENTER
         }, onKeyEnter, "keydown");
         enterListener.enable();
      },
      
      /**
       * Setup the YUI DataTable with custom renderers.
       *
       * @method _setupDataTable
       * @private
       */
      _setupDataTable: function SentInvites__setupDataTable()
      {
         /**
          * DataTable Cell Renderers
          *
          * Each cell has a custom renderer defined as a custom function. See YUI documentation for details.
          * These MUST be inline in order to have access to the Alfresco.SentInvites class (via the "me" variable).
          */
         var me = this;

         /**
          * Invitation status cell formatter
          *
          * @method renderInvitationStatus
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellInvitationStatus = function SentInvites_renderCellInvitationStatus(elCell, oRecord, oColumn, oData)
         {
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

            var invitationStatus = oRecord.getData('invitationStatus');
            if (invitationStatus == 'pending')
            {
               Dom.addClass(elCell.parentNode, "pending");
               invitationStatus = '<div class="status">' + me._msg('status.pending') + '</div>';
            }
            else if (invitationStatus == 'accepted')
            {
               Dom.addClass(elCell.parentNode, "accepted");
               invitationStatus = '<div class="status">' + me._msg('status.accepted') + '</div>';
            }
            else if (invitationStatus == 'declined')
            {
               Dom.addClass(elCell.parentNode, "declined");
               invitationStatus = '<div class="status">' + me._msg('status.declined') + '</div>';
            }

            elCell.innerHTML = invitationStatus;
         };
    
         /**
          * User avatar custom datacell formatter
          *
          * @method renderCellAvatar
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellAvatar = function SentInvites_renderCellAvatar(elCell, oRecord, oColumn, oData)
         {
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");

            var avatarUrl = Alfresco.constants.URL_CONTEXT + "components/images/no-user-photo-64.png";
            var invitee = oRecord.getData("invitee");
            if (invitee.avatar !== undefined)
            {
               avatarUrl = Alfresco.constants.PROXY_URI + invitee.avatar + "?c=queue&ph=true";
            }

            elCell.innerHTML = '<img class="avatar" src="' + avatarUrl + '" alt="avatar" />';
         };

         var generateUserNameLink = function(person)
         {
            var name = person.userName;
            var firstName = person.firstName;
            var lastName = person.lastName;
            var linkExtra = "";
            if ((firstName !== undefined) || (lastName !== undefined))
            {
               name = firstName ? firstName + " " : "";
               name += lastName ? lastName : "";
               linkExtra =  ' <span class="lighter">(' + $html(person.userName) + ')</span>';
            }
            var link = Alfresco.util.uriTemplate("userpage",
            {
               userid: person.userName,
               pageid: "profile"
            });
            return '<a href="' + link + '">' + $html(name) + '</a>' + linkExtra;
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
         var renderCellDescription = function SentInvites_renderCellDescription(elCell, oRecord, oColumn, oData)
         {
            // Currently rendering all results the same way
            var invitee = oRecord.getData("invitee");
            
            var sentDate = Alfresco.util.formatDate(oRecord.getData('sentInviteDate'));
            var role = me._msg("role." + oRecord.getData("role"));
            var desc = '<div class="to-invitee"><span class="attr-label">' + me._msg('info.to') + ': </span>';
            desc += '<span class="attr-value">' + generateUserNameLink(invitee) + '</span>';
            desc += '</div>';
            desc += '<div>';
            desc += '<span class="attr-label">' + me._msg('info.sent') + ': </span>';
            desc += '<span class="attr-value">' + $html(sentDate) + '</span>';
            desc += '<span class="separator"> | </span>';
            desc += '<span class="attr-label">' + me._msg('info.role') + ': </span>';
            desc += '<span class="attr-value">' + $html(role) + '</span>';
            desc += '</div>';
            elCell.innerHTML = desc;
         };
         
         /**
          * Action button datacell formatter
          *
          * @method renderCellActionButton
          * @param elCell {object}
          * @param oRecord {object}
          * @param oColumn {object}
          * @param oData {object|string}
          */
         var renderCellActionButton = function SentInvites_renderCellActionButton(elCell, oRecord, oColumn, oData)
         {
            Dom.setStyle(elCell.parentNode, "width", oColumn.width + "px");
            
            var userName = oRecord.getData("invitee").userName;
            var desc = '<span id="' + me.id + '-action-' + userName + '"></span>';
            elCell.innerHTML = desc;

            // create button
            var buttonLabel;
            if (oRecord.getData("invitationStatus") == 'pending')
            {
               buttonLabel = me._msg("button.cancel");
            }
            else
            {
               buttonLabel = me._msg("button.clear");
            }
            
            var button = new YAHOO.widget.Button(
            {
               type: "button",
               label: buttonLabel,
               name: me.id + "-selectbutton-" + userName,
               container: me.id + '-action-' + userName,
               onclick:
               {
                  fn: me.onActionClick,
                  obj: oRecord,
                  scope: me
               }
            });
            me.actionButtons[userName] = button;
         };

         // DataTable column defintions
         var columnDefinitions = [
         /*{
            key: "status", label: "Status", sortable: false, formatter: renderCellInvitationStatus, width: 70
         },*/
         {
            key: "avatar", label: "Avatar", sortable: false, formatter: renderCellAvatar, width: 70
         },
         {
            key: "person", label: "Description", sortable: false, formatter: renderCellDescription
         },
         {
            key: "actions", label: "Actions", sortable: false, formatter: renderCellActionButton, width: 100
         }];

         // DataTable definition
         this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + "-results", columnDefinitions, this.widgets.dataSource,
         {
            renderLoopSize: 32,
            initialLoad: false,
            MSG_EMPTY: this._msg("message.empty")
         });
         
         // Enable row highlighting
         this.widgets.dataTable.subscribe("rowMouseoverEvent", this.widgets.dataTable.onEventHighlightRow);
         this.widgets.dataTable.subscribe("rowMouseoutEvent", this.widgets.dataTable.onEventUnhighlightRow);
         
         // trigger the search
         this._performSearch("");
      },
      
      /**
       * Public function to clear the results DataTable
       */
      clearResults: function SentInvites_clearResults()
      {
         // Clear results DataTable
         if (this.widgets.dataTable)
         {
            var recordCount = this.widgets.dataTable.getRecordSet().getLength();
            if (recordCount > 0)
            {
               this.widgets.dataTable.deleteRows(0, recordCount);
            }
         }
         Dom.get(this.id + "-search-text").value = "";
      },

      /**
       * Cancel an invitation.
       */
      cancelInvite: function SentInvites_cancelInvite(record)
      {
         // disable the button
         this.actionButtons[record.getData('invitee').userName].set('disabled', true);
          
         // show a wait message
         this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage(
         {
            text: this._msg("message.removing"),
            spanClass: "wait",
            displayTime: 0
         });
         
         // ajax request success handler
         var success = function SentInvites_cancelInvite_success(response)
         {
            // hide the wait message
            this.widgets.feedbackMessage.destroy();
             
            // remove the record from the list
            var index = this.widgets.dataTable.getRecordIndex(record);
            if (index !== null)
            {
               this.widgets.dataTable.deleteRow(index);
            }
         };
         
         // request failure handler
         var failure = function SentInvites_cancelInvite_failure(response)
         {
            // remove the message
            this.widgets.feedbackMessage.destroy();

            this.actionButtons[record.getData('invitee').userName].set('disabled', true);
         };
         
         // get the url to call
         var url = Alfresco.constants.PROXY_URI + "api/invite/cancel";
         
         // execute ajax request
         Alfresco.util.Ajax.request(
         {
            url: url,
            method: "GET",
            dataObj:
            {
               inviteId: record.getData('inviteId')
            },
            responseContentType : "application/json",
            successMessage: this._msg("message.cancel.success"),
            successCallback:
            {
               fn: success,
               scope: this
            },
            failureMessage: this._msg("message.cancel.failure"),
            failureCallback:
            {
               fn: failure,
               scope: this
            }
         });
         
      },
      
      /**
       * YUI WIDGET EVENT HANDLERS
       * Handlers for standard events fired from YUI widgets, e.g. "click"
       */

      /**
       * Action button click handler
       *
       * @method onActionClick
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onActionClick: function SentInvites_onActionClick(event, p_obj)
      {
         this.cancelInvite(p_obj);
      },

      /**
       * Search button click event handler
       *
       * @method onSearchClick
       * @param e {object} DomEvent
       * @param p_obj {object} Object passed back from addListener method
       */
      onSearchClick: function SentInvites_onSearchClick(e, p_obj)
      {
         // fetch the firstname, lastname nad email
         var searchTermElem = Dom.get(this.id + "-search-text");
         var searchTerm = searchTermElem.value;
         searchTerm = $html(searchTerm);
         
         this._performSearch(searchTerm);
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
      _setDefaultDataTableErrors: function SentInvites__setDefaultDataTableErrors(dataTable)
      {
         var msg = Alfresco.util.message;
         dataTable.set("MSG_EMPTY", msg("message.empty", "Alfresco.SentInvites"));
         dataTable.set("MSG_ERROR", msg("message.error", "Alfresco.SentInvites"));
      },
      
      /**
       * Updates people list by calling data webscript
       *
       * @method _performSearch
       * @param searchTerm {string} Search term from input field
       */
      _performSearch: function SentInvites__performSearch(searchTerm)
      {
         // Reset the custom error messages
         this._setDefaultDataTableErrors(this.widgets.dataTable);
         
         // Don't display any message
         // this.widgets.dataTable.set("MSG_EMPTY", "");
         
         // Empty results table
         var recordCount = this.widgets.dataTable.getRecordSet().getLength();
         if (recordCount > 0)
         {
            this.widgets.dataTable.deleteRows(0, recordCount);
         }
         
         var successHandler = function SentInvites__pS_successHandler(sRequest, oResponse, oPayload)
         {
            this._setDefaultDataTableErrors(this.widgets.dataTable);
            if (oResponse.results.length > 0)
            {
               this.widgets.dataTable.onDataReturnInitializeTable.call(this.widgets.dataTable, sRequest, oResponse, oPayload);
            }
         };
         
         var failureHandler = function SentInvites__pS_failureHandler(sRequest, oResponse)
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
       * Build URI parameter string for doclist JSON data webscript
       *
       * @method _buildSearchParams
       * @param path {string} Path to query
       */
      _buildSearchParams: function SentInvites__buildSearchParams(searchTerm)
      {
         return YAHOO.lang.substitute("siteShortName={siteShortName}",
         {
            siteShortName: encodeURIComponent(this.options.siteId)
         })
      },
      
      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function SentInvites__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.SentInvites", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();
