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
 * DispositionEdit component.
 *
 * User can add, edit and remove action (steps) in the disposition schedule
 * and to each action ad and remove events.
 * Meta data properties can be edited on a different page.
 *
 * @namespace Alfresco
 * @class Alfresco.DispositionEdit
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event;

   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;

   /**
    * DispositionEdit constructor.
    *
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.DispositionEdit} The new component instance
    * @constructor
    */
   Alfresco.DispositionEdit = function DispositionEdit_constructor(htmlId)
   {
      Alfresco.DispositionEdit.superclass.constructor.call(this, "Alfresco.DispositionEdit", htmlId, ["button", "container"]);

      return this;
   };

   YAHOO.extend(Alfresco.DispositionEdit, Alfresco.component.Base,
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
          * The nodeRef to the object that owns the disposition schedule that is configured
          *
          * @property nodeRef
          * @type {string}
          */
         nodeRef: null,

         /**
          * The url to the filePlan that is configured
          *
          * @property fileplanUrl
          * @type {string}
          */
         siteId: null,

         /**
          * The events available and information about if they are autmatic or not
          *
          * @property events
          * @type {object} with objects like { label: string, automatic: boolean}
          *       and the event name {string} as the key.
          */
         events: {}
      },

      /**
       * Fired by YUI when parent element is available for scripting
       * @method onReady
       */
      onReady: function DispositionEdit_onReady()
      {
         // Save a reference to important elements
         this.widgets.actionListEl = Dom.get(this.id + "-actionList");
         this.widgets.flowButtons = Dom.get(this.id + "-flowButtons");

         // Save reference to buttons so we can change label and such later
         this.widgets.createButton = Alfresco.util.createYUIButton(this, "createaction-button", this.onCreateActionButtonClick,
         {
            disabled: true
         });
         this.widgets.doneButton = Alfresco.util.createYUIButton(this, "done-button", this.onDoneActionsButtonClick);

         // Get the templates and remove the dummy placeholders from the DOM
         this.widgets.eventTemplateEl = Dom.get(this.id + "-event-template");
         var dummyEl = Dom.get(this.id + "-event-template-dummy");
         dummyEl.parentNode.removeChild(dummyEl);
         this.widgets.actionTemplateEl = Dom.get(this.id + "-action-template");
         dummyEl = Dom.get(this.id + "-action-template-dummy");
         dummyEl.parentNode.removeChild(dummyEl);

         this._loadActions();
      },

      /**
       * Loads actions from the server
       *
       * @method _loadActions
       * @private
       */
      _loadActions: function DispositionEdit__loadActions()
      {
         Alfresco.util.Ajax.jsonGet(
         {
            url: Alfresco.constants.PROXY_URI_RELATIVE + "api/node/" + this.options.nodeRef.replace(":/", "") + "/dispositionschedule",
            successCallback:
            {
               fn: function(response)
               {
                  if (response.json)
                  {
                     var schedule = response.json.data,
                        actions = schedule.actions ? schedule.actions : [],
                        action,
                        actionEl;
                     
                     for (var i = 0, ii = actions.length; i < ii; i++)
                     {
                        action = actions[i];
                        actionEl = this._createAction(action);
                        actionEl = this.widgets.actionListEl.appendChild(actionEl);
                        this._setupActionForm(action, actionEl);
                     }
                  }
                  this.widgets.createButton.set("disabled", false);                  
               },
               scope: this
            },
            failureCallback:
            {
               fn: function(response)
               {                  
                  Alfresco.util.PopupManager.displayPrompt(
                  {
                     text: this.msg("message.getActionFailure", this.name)
                  });
               },
               scope: this
            }
         });
      },

      /**
       * Create a action in the list
       *
       * @method _createAction
       * @param action The action info object
       * @private
       */
      _createAction: function DispositionEdit__createAction(action)
      {
         var me = this;
         
         // Clone template
         var actionEl = this.widgets.actionTemplateEl.cloneNode(true),
            elId = Alfresco.util.generateDomId(actionEl);

         // Action Id
         Dom.getElementsByClassName("id", "input", actionEl)[0].value = action.id;

         // Period
         var period = action.period ? action.period.split("|") : [];

         // Sequence Number
         Dom.getElementsByClassName("no", "div", actionEl)[0].innerHTML = action.index + 1;

         // Description
         Dom.getElementsByClassName("description", "textarea", actionEl)[0].value = action.description ? action.description : "";

         // Action
         var actionType = action.name,
            actionTypeEl = Dom.getElementsByClassName("action-type", "select", actionEl)[0];
         Event.addListener(actionTypeEl, "change", this.onActionTypeSelectChange,
         {
            actionEl: actionEl
         }, this);
         var actionTitle = Alfresco.util.setSelectedIndex(actionTypeEl, actionType);
         actionTitle = actionTitle ? actionTitle : actionType; 

         // Period Amount
         var periodAmount = (period && period.length > 1) ? period[1] : null,
            periodAmountEl = Dom.getElementsByClassName("period-amount", "input", actionEl)[0];
         periodAmountEl.value = periodAmount ? periodAmount : "";
         periodAmountEl.setAttribute("id", elId + "-periodAmount");
         Event.addListener(periodAmountEl, "keyup", this.onPeriodAmountTextKeyUp,
         {
            actionEl: actionEl
         }, this);

         // Period Action
         var periodAction = action.periodProperty,
            periodActionEl = Dom.getElementsByClassName("period-action", "select", actionEl)[0];
         Alfresco.util.setSelectedIndex(periodActionEl, periodAction);

         // Period Unit
         var periodUnit = (period && period.length > 0) ? period[0] : null,
            periodUnitEl = Dom.getElementsByClassName("period-unit", "select", actionEl)[0];
         Event.addListener(periodUnitEl, "change", this.onPeriodUnitSelectChange,
         {
            actionEl: actionEl,
            periodAmountEl: periodAmountEl,
            periodActionEl: periodActionEl
         }, this);
         Alfresco.util.setSelectedIndex(periodUnitEl, periodUnit);

         // Add event button
         var addEventButtonEl = Dom.getElementsByClassName("addevent-button", "input", actionEl)[0],
               addEventMenuEl = Dom.getElementsByClassName("addevent-menu", "select", actionEl)[0];
         var addEventButton = Alfresco.util.createYUIButton(this, "addevent-button", null,
         {
            type: "menu",
            lazyloadmenu: false, 
            menu: addEventMenuEl
         }, addEventButtonEl);

         addEventButton.getMenu().subscribe("beforeShow", function(p_sType, p_aArgs, aItemsArray)
         {
            me._disableUnusedEvents(addEventButton, actionEl);
         }, []);

         addEventButton.getMenu().subscribe("click", function(p_sType, p_aArgs)
         {
            if (p_aArgs[1] && !p_aArgs[1].cfg.getProperty("disabled"))
            {
               me._addSelectedEventItem(p_aArgs[1].value, actionEl, addEventButton);
            }
         });


         // Enable/Disable period & events section
         var periodEnabledCheckBox = Dom.getElementsByClassName("period-enabled", "input", actionEl)[0];
         periodEnabledCheckBox.checked =  periodUnit || periodAmount;
         this._disablePeriodSection(!periodEnabledCheckBox.checked, actionEl);
         var eventsEnabledCheckBox = Dom.getElementsByClassName("events-enabled", "input", actionEl)[0];
         eventsEnabledCheckBox.checked = action.events && action.events.length > 0;
         this._disableEventsSection(!eventsEnabledCheckBox.checked, actionEl);
         if (!periodEnabledCheckBox.checked || !eventsEnabledCheckBox.checked)
         {
            Dom.addClass(actionEl, "relation-disabled");
         }

         // Add listeners to toggle enabling/disabling
         Event.addListener(periodEnabledCheckBox, "click", this.onRelationEnablingCheckBoxClick,
         {
            actionEl: actionEl,
            checkBoxEls: [periodEnabledCheckBox, eventsEnabledCheckBox]
         }, this);

         Event.addListener(periodEnabledCheckBox, "click", function(e, checkBox)
         {
            this._disablePeriodSection(!checkBox.checked, actionEl);
         }, periodEnabledCheckBox, this);

         Event.addListener(eventsEnabledCheckBox, "click", this.onRelationEnablingCheckBoxClick,
         {
            actionEl: actionEl,
            checkBoxEls: [eventsEnabledCheckBox, periodEnabledCheckBox]
         }, this);

         Event.addListener(eventsEnabledCheckBox, "click", function(e, checkBox)
         {
            this._disableEventsSection(!checkBox.checked, actionEl);
         }, eventsEnabledCheckBox, this);

         // Relation
         var relationSelect = Dom.getElementsByClassName("relation", "select", actionEl)[0];
         Dom.addClass(actionEl, action.eligibleOnFirstCompleteEvent ? "or" : "and");
         Alfresco.util.setSelectedIndex(relationSelect, action.eligibleOnFirstCompleteEvent + "");
         Event.addListener(relationSelect, "change", this.onRelationSelectChange,
         {
            actionEl: actionEl,
            relationSelect: relationSelect
         }, this);

         // Add listener to display edit and delete action
         var detailsEl = Dom.getElementsByClassName("details", "div", actionEl)[0],
            editEl = Dom.getElementsByClassName("edit", "span", actionEl)[0];
         Event.addListener(editEl, "click", this.onEditActionClick,
         {
            actionEl: actionEl,
            detailsEl: detailsEl
         }, this);

         var deleteEl = Dom.getElementsByClassName("delete", "span", actionEl)[0];
         Event.addListener(deleteEl, "click", this.onDeleteActionClick, actionEl, this);

         // Add events
         var eventListEl = Dom.getElementsByClassName("events-list", "ul", actionEl)[0];
         for (var i = 0; action.events && i < action.events.length; i++)
         {
            eventListEl.appendChild(this._createEvent(i, action.events[i]));
         }

         // Make sure enabling and disabling is correct
         this._disableEnablePeriodElements(periodUnitEl, periodAmountEl, periodActionEl);

         // Title
         this._refreshTitle(actionEl, actionTitle);

         return actionEl;
      },

      /**
       * Create an action in the list
       *
       * @method _setupActionForm
       * @param action The action info object
       * @param actionEl The action HTMLElement
       * @private
       */
      _setupActionForm: function DispositionEdit__setupActionForm(action, actionEl)
      {
         // Find id
         var elId = actionEl.attributes["id"].value;

         // Setup form
         var formEl = Dom.getElementsByClassName("action-form", "form", actionEl)[0],
            formId = elId + "-action-form";
         
         if (YAHOO.env.ua.ie > 0 && YAHOO.env.ua.ie < 8)
         {
            // MSIE 6 & 7 mix up name and id attributes on form children. Which is always useful.
            formEl.attributes["id"].value = formId;
         }
         else
         {
            formEl.setAttribute("id", formId);
         }
         var actionForm = new Alfresco.forms.Form(formId);

         // Add validation
         actionForm.addValidation(elId + "-periodAmount", Alfresco.forms.validation.number, null, "keyup");
         //actionForm.addValidation(elId + "-periodAmount", Alfresco.forms.validation.mandatory, null, "keyup");
         var periodEnabledCheckBox = Dom.getElementsByClassName("period-enabled", "input", actionEl)[0];
         Event.addListener(periodEnabledCheckBox, "click", function(e, obj)
         {
            // Make sure save button is updated
            obj.form.updateSubmitElements();
         },
         {
            form: actionForm
         }, this);

         // Create buttons
         var saveActionEl = Dom.getElementsByClassName("saveaction", "span", actionEl)[0],
            saveActionButton = Alfresco.util.createYUIButton(this, "saveaction-button", null,
            {
               type: "submit"
            }, saveActionEl),
            cancelEl = Dom.getElementsByClassName("cancel", "span", actionEl)[0],
            cancelActionButton = Alfresco.util.createYUIButton(this, "cancel-button", null, {}, cancelEl);
         
         cancelActionButton.on("click", this.onCancelActionButtonClick,
         {
            action: action,
            actionEl: actionEl
         }, this);

         // Set form url
         var actionId = Dom.getElementsByClassName("id", "input", formEl)[0].value;
         if (actionId && actionId.length > 0)
         {
            actionForm.setAjaxSubmitMethod(Alfresco.util.Ajax.PUT);
            formEl.attributes.action.nodeValue = Alfresco.constants.PROXY_URI_RELATIVE + "api/node/" + this.options.nodeRef.replace(":/", "") + "/dispositionschedule/dispositionactiondefinitions/" + actionId;
         }
         else
         {
            actionForm.setAjaxSubmitMethod(Alfresco.util.Ajax.POST);
            formEl.attributes.action.nodeValue = Alfresco.constants.PROXY_URI_RELATIVE + "api/node/" + this.options.nodeRef.replace(":/", "") + "/dispositionschedule/dispositionactiondefinitions";
         }

         // Setup form buttons
         actionForm.setShowSubmitStateDynamically(true, false);
         actionForm.setSubmitElements(saveActionButton);
         actionForm.doBeforeFormSubmit =
         {
            fn: function(formEl, obj)
            {
               // Merge period value
               var puEl = Dom.getElementsByClassName("period-unit", "select", formEl)[0],
                  paEl = Dom.getElementsByClassName("period-amount", "input", formEl)[0];
               var pa = "", pu = "";
               if (!paEl.disabled)
               {
                  pa = paEl.value;
               }
               if (!puEl.disabled)
               {
                  pu = puEl.options[puEl.selectedIndex].value;
               }
               var periodEl = Dom.getElementsByClassName("period", "input", formEl)[0];
               periodEl.value = pu + "|" + pa;               

               // Disable buttons during submit
               obj.saveButton.set("disabled", true);
               obj.cancelButton.set("disabled", true);

               // Display a pengding message
               this.widgets.feedbackMessage = Alfresco.util.PopupManager.displayMessage(
               {
                  text: this.msg("message.savingAction", this.name),
                  spanClass: "wait",
                  displayTime: 0
               });
            },
            obj: {
               saveButton: saveActionButton,
               cancelButton: cancelActionButton,
               actionForm: actionForm
            },
            scope: this
         };

         // Submit as an ajax submit (not leave the page), in json format
         actionForm.setAJAXSubmit(true,
         {
            successCallback:
            {
               fn: function(serverResponse, obj)
               {
                  this.widgets.feedbackMessage.destroy();
                  obj.saveButton.set("disabled", false);
                  obj.cancelButton.set("disabled", false);
                  Dom.removeClass(obj.actionEl, "expanded");
                  Dom.addClass(obj.actionEl, "collapsed");

                  // Save new info for future cancels and hide details div
                  Dom.getElementsByClassName("id", "input", actionEl)[0].value = serverResponse.json.data.id;
                  action = YAHOO.lang.merge(action, serverResponse.json.data);
                  var details = Dom.getElementsByClassName("details", "div", actionEl)[0];
                  Dom.setStyle(details, "display", "none");

                  // Refresh the title from the choices
                  this._refreshTitle(obj.actionEl);

                  // Display add step button
                  Dom.removeClass(this.widgets.flowButtons, "hidden");
               },
               obj:
               {
                  saveButton: saveActionButton,
                  cancelButton: cancelActionButton,
                  actionEl: actionEl
               },
               scope: this
            },
            failureCallback:
            {
               fn: function(serverResponse, obj)
               {
                  this.widgets.feedbackMessage.destroy();
                  obj.saveButton.set("disabled", false);
                  obj.cancelButton.set("disabled", false);
                  Alfresco.util.PopupManager.displayPrompt(
                  {
                     text: this.msg("message.saveActionFailure", this.name)
                  });
               },
               obj:
               {
                  saveButton: saveActionButton,
                  cancelButton: cancelActionButton
               },
               scope: this
            }
         });
         actionForm.setSubmitAsJSON(true);
         actionForm.init();
      },

      /**
       * Disable the period elements
       *
       * @method _disablePeriodSection
       * @param disabled
       * @param actionEl The action HTMLElement
       */
      _disablePeriodSection: function DispositionEdit__disablePeriodSection(disabled, actionEl)
      {
         Dom.getElementsByClassName("period-amount", "input", actionEl)[0].disabled = disabled;
         Dom.getElementsByClassName("period-unit", "select", actionEl)[0].disabled = disabled;
         Dom.getElementsByClassName("period-action", "select", actionEl)[0].disabled = disabled;
      },

      /**
       * handles disabling and enabling of period elements
       *
       * @method _disableEnablePeriodElements
       * @param periodUnitEl The period unit select
       * @param periodAmountEl The period unit text input
       * @param periodActionEl The period action select
       */
      _disableEnablePeriodElements: function DispositionEdit__disableEnablePeriodElements(periodUnitEl, periodAmountEl, periodActionEl)
      {
         var periodUnit = periodUnitEl.options[periodUnitEl.selectedIndex].value;
         periodAmountEl.disabled = Alfresco.util.arrayContains(["none", "immediately", "fmend", "fqend", "fyend", "monthend", "quarterend", "yearend"], periodUnit);
         if (periodAmountEl.disabled)
         {
            periodAmountEl.value = "";
         }
         periodActionEl.disabled = Alfresco.util.arrayContains(["none", "immediately"], periodUnit);
      },

      /**
       * Disable the events elements
       *
       * @method _disableEventsSection
       * @param disabled
       * @param actionEl The action HTMLElement
       */
      _disableEventsSection: function DispositionEdit__disableEventsSection(disabled, actionEl)
      {
         var eventsDivEl = Dom.getElementsByClassName("events", "div", actionEl)[0];
         Dom.setStyle(eventsDivEl, "display", disabled ? "none" : "block");
         var eventEls = Dom.getElementsByClassName("action-event-name-value", "select", actionEl);
         for (var i = 0; i < eventEls.length; i++)
         {
            eventEls[i].disabled = disabled;
         }
      },

      /**
       * Set the title header
       *
       * @method _refreshTitle
       * @param actionEl The action HTML element
       * @param actionType (optional) Will use the text from the action drop down if not given
       */
      _refreshTitle: function DispositionEdit__refreshTitle(actionEl, actionType)
      {
         var actionTypeEl = Dom.getElementsByClassName("action-type", "select", actionEl)[0],
               periodUnitSelect = Dom.getElementsByClassName("period-unit", "select", actionEl)[0],
               periodAmountEl = Dom.getElementsByClassName("period-amount", "input", actionEl)[0];
         var title = "",
               actionType = actionType ? actionType : actionTypeEl.options[actionTypeEl.selectedIndex].text;
         if (!periodUnitSelect.disabled && periodUnitSelect.options[periodUnitSelect.selectedIndex].value != "none")
         {
            if (periodAmountEl.disabled || periodAmountEl.value == "" || periodAmountEl.value == "0")
            {
               title = this.msg(
                     "label.title.noTime",
                     actionType,
                     periodUnitSelect.options[periodUnitSelect.selectedIndex].text.toLowerCase());
            }
            else
            {
               title = this.msg(
                     "label.title.complex",
                     actionType,
                     periodAmountEl.value,
                     periodUnitSelect.options[periodUnitSelect.selectedIndex].text.toLowerCase());
            }
         }
         else
         {
            title = this.msg(
                  "label.title.simple",
                  actionType
                  );
         }

         Dom.getElementsByClassName("title", "div", actionEl)[0].innerHTML = title;         
      },

      /**
       * Create a action in the list
       *
       * @method _createEvent
       * @param p_sequence {number} The (zero-based) sequence number of the event
       * @param p_eventName {string} The event key/name
       * @private
       */
      _createEvent: function DispositionEdit__createEvent(p_sequence, p_eventName)
      {
         // Clone template
         var eventEl = this.widgets.eventTemplateEl.cloneNode(true),
            elId = Alfresco.util.generateDomId(eventEl);

         Dom.addClass(eventEl, p_sequence % 2 === 0 ? "even" : "odd");
         if (p_sequence === 0)
         {
            Dom.addClass(eventEl, "first");
         }

         // Event Type
         var eventNameHidden = Dom.getElementsByClassName("action-event-name-value", "input", eventEl)[0],
               eventNameLabel = Dom.getElementsByClassName("action-event-name-label", "span", eventEl)[0],
               label = this.options.events[p_eventName] ? this.options.events[p_eventName].label + "" : p_eventName;;
         eventNameHidden.value = p_eventName;
         eventNameLabel.innerHTML = label;

         // Display data
         var automatic = this.options.events[p_eventName] ? this.options.events[p_eventName].automatic + "" : null,
            completion = "";
         if (automatic)
         {
            completion = this.msg("label.automatic." + automatic);
         }
         Dom.getElementsByClassName("action-event-completion", "div", eventEl)[0].innerHTML = completion;

         // Add listener to delete event
         var deleteEventEl = Dom.getElementsByClassName("delete", "span", eventEl)[0];
         Event.addListener(deleteEventEl, "click", function(e, obj)
         {            
            this._deleteClickedEvent(eventEl);
         }, null, this);

         return eventEl;
      },


      /**
       * Refreshes the action sequence labels.
       *
       * Ideally li.style.list-style: should be used with decimal and inline
       * so this would be handled automatically but hasn't been due to styling issues.
       *
       * @method _refreshActionList
       * @private
       */
      _refreshActionList: function DispositionEdit__refreshActionList()
      {
         var actionNos = Dom.getElementsByClassName("no", "div", this.widgets.actionListEl);
         for (var i = 0; i < actionNos.length; i++)
         {
            actionNos[i].innerHTML = i + 1;
         }         
      },

      /**
       * Refreshes the event list so first event doesn't display the relation.
       *
       * @method _refreshEventList
       * @param eventList The event list HTMLElement
       * @private
       */
      _refreshEventList: function DispositionEdit__refreshEventList(eventList)
      {
         var events = eventList.getElementsByTagName("li");
         for (var i = 0; i < events.length; i++)
         {
            Dom.removeClass(events[i], "even");
            Dom.removeClass(events[i], "odd");
            Dom.removeClass(events[i], "first");
            Dom.addClass(events[i], ((i + 1) % 2) === 0 ? "odd" : "even");
            if (i === 0)
            {
               Dom.addClass(events[i], "first");
            }
         }
      },


      /**
       * Called when user changes the action type amount
       *
       * @method onActionTypeSelectChange
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onActionTypeSelectChange: function DispositionEdit_onActionTypeSelectChange(e, obj)
      {
         this._refreshTitle(obj.actionEl);
      },

      /**
       * Called when user changes the period amount
       *
       * @method onPeriodAmountTextKeyUp
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onPeriodAmountTextKeyUp: function DispositionEdit_onPeriodAmountTextKeyUp(e, obj)
      {
         this._refreshTitle(obj.actionEl);
      },

      /**
       * Called when user toggles one of the checkboxes related to
       *
       * @method onPeriodUnitSelectChange
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onPeriodUnitSelectChange: function DispositionEdit_onPeriodUnitSelectChange(e, obj)
      {
         var periodUnitEl = e.target;
         this._disableEnablePeriodElements(periodUnitEl, obj.periodAmountEl, obj.periodActionEl);
         this._refreshTitle(obj.actionEl);
      },

      /**
       * Called when user toggles one of the checkboxes related to
       *
       * @method onRelationEnablingCheckBoxClick
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onRelationEnablingCheckBoxClick: function DispositionEdit_onRelationEnablingCheckBoxClick(e, obj)
      {
         for (var i = 0; i < obj.checkBoxEls.length; i++)
         {
            if (!obj.checkBoxEls[i].checked)
            {
               Dom.addClass(obj.actionEl, "relation-disabled");
               return;
            }
         }
         Dom.removeClass(obj.actionEl, "relation-disabled");
      },

      /**
       * Called when user changes the relation option select
       *
       * @method onRelationSelectChange
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onRelationSelectChange: function DispositionEdit_onRelationSelectChange(e, obj)
      {
         var relation = obj.relationSelect.options[obj.relationSelect.selectedIndex].value;
         Dom.removeClass(obj.actionEl, "or");
         Dom.removeClass(obj.actionEl, "and");
         Dom.addClass(obj.actionEl, relation == "true" ? "or" : "and");
      },

      /**
       * Makes sure only unused events are enabled in the menu
       *
       * @method _disableUnusedEvents
       * @param addEventButton The button used to add event
       * @param actionEl The action HTMLElement
       */
      _disableUnusedEvents: function DispositionEdit__disableUnusedEvents(addEventButton, actionEl)
      {
         // Find out what events that has been used
         var actionEventValues = Dom.getElementsByClassName("action-event-name-value", "input", actionEl);
         var usedEventNames = {};
         for (var i = 0; i < actionEventValues.length; i++)
         {
            usedEventNames[actionEventValues[i].value] = true;
         }

         // Clear event menu and disable/enable events in the menu
         var items = addEventButton.getMenu().getItems(),
               item;
         for (i = 0; i < items.length; i++)
         {
            item = items[i];
            item.cfg.setProperty("disabled", usedEventNames[item.value] ? true : false);
         }
      },

      /**
       * Called when user has selected an event to add
       *
       * @method _addSelectedEventItem
       * @param eventType The type of event that shall get added
       * @param actionEl The action HTMLElement
       */
      _addSelectedEventItem: function DispositionEdit__addSelectedEventItem(eventType, actionEl)
      {
         var eventListEl = Dom.getElementsByClassName("events-list", "ul", actionEl)[0],
            sequence = eventListEl.getElementsByTagName("li").length;

         // Create new event
         var eventEl = this._createEvent(sequence, eventType);
         eventListEl.appendChild(eventEl);
      },

      /**
       * Called when user clicks the delete event icon
       *
       * @method _deleteClickedEvent
       * @param eventEl The event HTMLElement
       */
      _deleteClickedEvent: function DispositionEdit__deleteClickedEvent(eventEl)
      {
         var parent = eventEl.parentNode;
         parent.removeChild(eventEl);
         this._refreshEventList(parent);
      },

      /**
       * Called when user clicks the edit action icon
       *
       * @method onEditClick
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onEditActionClick: function DispositionEdit_onEditClick(e, obj)
      {
         Alfresco.util.Anim.fadeIn(obj.detailsEl);
         Dom.removeClass(obj.actionEl, "collapsed");
         Dom.addClass(obj.actionEl, "expanded");
      },

      /**
       * Called when user clicks the delete action icon
       *
       * @method onDeleteActionClick
       * @param e click event object
       * @param actionEl THe action HTMLElement
       */
      onDeleteActionClick: function DispositionEdit_onDeleteActionClick(e, actionEl)
      {
         var me = this;
         Alfresco.util.PopupManager.displayPrompt(
         {
            title: Alfresco.util.message("title.deleteAction", this.name),
            text: Alfresco.util.message("label.confirmDeleteAction", this.name),
            noEscape: true,
            buttons: [
            {
               text: Alfresco.util.message("button.yes", this.name),
               handler: function DispositionEdit_delete()
               {
                  this.destroy();
                  me._onDeleteActionConfirmedClick.call(me, actionEl);
               }
            },
            {
               text: Alfresco.util.message("button.no", this.name),
               handler: function DispositionEdit_cancel()
               {
                  me.deletePromptActive = false;
                  this.destroy();
               },
               isDefault: true
            }]
         });
      },

      /**
       * Called when user clicks the delete action icon
       *
       * @method onDeleteActionClick
       * @param actionEl THe action HTMLElement
       */
      _onDeleteActionConfirmedClick: function DispositionEdit_onDeleteActionClick(actionEl)
      {
         var actionId = Dom.getElementsByClassName("id", "input", actionEl)[0].value,
            feedbackMessage = Alfresco.util.PopupManager.displayMessage(
            {
               text: this.msg("message.deletingAction"),
               spanClass: "wait",
               displayTime: 0
            });

         // user has confirmed, perform the actual delete
         Alfresco.util.Ajax.jsonDelete(
         {
            url: Alfresco.constants.PROXY_URI_RELATIVE + "api/node/" + this.options.nodeRef.replace(":/", "") + "/dispositionschedule/dispositionactiondefinitions/" + actionId,
            dataObj: {},
            successCallback:
            {
               fn: function(response)
               {
                  feedbackMessage.destroy();
                  Alfresco.util.PopupManager.displayMessage(
                  {
                     text: this.msg("message.deleteActionSuccess")
                  });
                  actionEl.parentNode.removeChild(actionEl);
                  this._refreshActionList();
               },
               scope: this
            },
            failureCallback:
            {
               fn: function(response)
               {
                  feedbackMessage.destroy();
                  Alfresco.util.PopupManager.displayMessage(
                  {
                     text: this.msg("message.deleteActionFailure", this.name)
                  });
               },
               scope: this
            }
         });
      },

      /**
       * Called when user clicks the cancel action button
       *
       * @method onCancelActionButtonClick
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onCancelActionButtonClick: function DispositionEdit_onCancelActionButtonClick(e, obj)
      {
         var actionId = Dom.getElementsByClassName("id", "input", obj.actionEl)[0].value;
         if (actionId && actionId.length > 0)
         {
            /**
             * It is a previous action, cancel it by removing the action element
             * from the dom and insert a new fresh one by using the template and
             * the original data.
             */
            var newActionEl = this._createAction(obj.action);
            obj.actionEl.parentNode.insertBefore(newActionEl, obj.actionEl);
            obj.actionEl.parentNode.removeChild(obj.actionEl);
            this._setupActionForm(obj.action, newActionEl);
            this._refreshActionList();
         }
         else
         {
            // It was an unsaved action, just remove it
            obj.actionEl.parentNode.removeChild(obj.actionEl);
         }
         Dom.removeClass(this.widgets.flowButtons, "hidden");
      },

      /**
       * Called when user clicks the cancel action button
       *
       * @method onCreateActionButtonClick
       * @param e click event object
       * @param obj callback object containg action info & HTMLElements
       */
      onCreateActionButtonClick: function DispositionEdit_onCreateActionButtonClick(e, obj)
      {
         var action =
         {
            id: "",
            index: Dom.getElementsByClassName("action", "li", this.widgets.actionListEl).length,
            title: this.msg("label.title.new"),
            name: "",
            type: "",
            period : null,
            periodProperty: null,
            description: "",
            eligibleOnFirstCompleteEvent: true,
            events: []
         };
         var newActionEl = this._createAction(action);
         this.widgets.actionListEl.appendChild(newActionEl);
         this._setupActionForm(action, newActionEl);
         this.onEditActionClick(null,
         {
            detailsEl: Dom.getElementsByClassName("details", "div", newActionEl)[0],
            actionEl: newActionEl
         });
         Dom.addClass(this.widgets.flowButtons, "hidden");
      },

      /**
       * Fired when the user clicks the Cancel button.
       * Takes the user back to the details edit page without saving anything.
       *
       * @method onDoneActionsButtonClick
       * @param event {object} a "click" event
       */
      onDoneActionsButtonClick: function DispositionEdit_onDoneActionsButtonClick(event)
      {
         // Disable buttons to avoid double submits or cancel during post
         this.widgets.doneButton.set("disabled", true);

         // Send the user to this page again without saving changes
         document.location.href = Alfresco.constants.URL_CONTEXT + "page/site/" + this.options.siteId + "/record-category-details?nodeRef=" + this.options.nodeRef;
      }
   });
})();