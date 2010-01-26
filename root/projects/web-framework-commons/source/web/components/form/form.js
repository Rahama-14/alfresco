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
 * Form UI component.
 * 
 * @namespace Alfresco
 * @class Alfresco.FormUI
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
    * FormUI constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.FormUI} The new FormUI instance
    * @constructor
    */
   Alfresco.FormUI = function FormUI_consructor(htmlId, parentId)
   {
      this.name = "Alfresco.FormUI";
      this.id = htmlId;
      this.parentId = parentId;
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);
      
      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "menu", "container"], this.onComponentsLoaded, this);

      // Initialise prototype properties
      this.buttons = {};
      this.formsRuntime = null;
      
      /* Decoupled event listeners */
      YAHOO.Bubbling.on("metadataRefresh", this.onFormRefresh, this);
      YAHOO.Bubbling.on("mandatoryControlValueUpdated", this.onMandatoryControlValueUpdated, this);

      return this;
   };

   Alfresco.FormUI.prototype =
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
          * Mode the current form is in, can be "view", "edit" or "create", defaults to "edit".
          * 
          * @property mode
          * @type string
          */ 
         mode: "edit",
         
         /**
          * Encoding type to be used when the form is submitted, can be "multipart/form-data",
          * "application/x-www-form-urlencoded" or "application/json", defaults to "multipart/form-data".
          * 
          * @property enctype
          * @type string
          */ 
         enctype: "multipart/form-data",
         
         /**
          * List of objects representing the constraints to setup on the form fields
          * 
          * @property fieldConstraints
          * @type array[object]
          */
         fieldConstraints: [],

         /**
          * Arguments used to build the form.
          * Used to Ajax-rebuild the form when in "view" mode
          * 
          * @property arguments
          * @type object
          */
         arguments: {}
      },
      
      /**
       * Object container for storing YUI button instances.
       * 
       * @property buttons
       * @type object
       */
      buttons: null,
       
      /**
       * The forms runtime instance.
       * 
       * @property
       * @type object
       */
      formsRuntime: null, 
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.Search} returns 'this' for method chaining
       */
      setMessages: function FormUI_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.Search} returns 'this' for method chaining
       */
      setOptions: function FormUI_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function FormUI_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },

      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function FormUI_onReady()
      {
         if (this.options.mode !== "view")
         {
            // make buttons YUI buttons
            this.buttons.submit = Alfresco.util.createYUIButton(this, "submit", null,
            {
               type: "submit"
            });

            // force the generated button to have a name of "-" so it gets ignored in
            // JSON submit. TODO: remove this when JSON submit behaviour is configurable
            Dom.get(this.id + "-submit-button").name = "-";

            if (Dom.get(this.id + "-reset") !== null)
            {
               this.buttons.reset = Alfresco.util.createYUIButton(this, "reset", null,
               {
                  type: "reset"
               });

               // force the generated button to have a name of "-" so it gets ignored in
               // JSON submit. TODO: remove this when JSON submit behaviour is configurable
               Dom.get(this.id + "-reset-button").name = "-";
            }

            if (Dom.get(this.id + "-cancel") !== null)
            {
               this.buttons.cancel = Alfresco.util.createYUIButton(this, "cancel", null);

               // force the generated button to have a name of "-" so it gets ignored in
               // JSON submit. TODO: remove this when JSON submit behaviour is configurable
               Dom.get(this.id + "-cancel-button").name = "-";
            }

            // fire event to inform any listening components that the form HTML is ready
            YAHOO.Bubbling.fire("formContentReady", this);

            this.formsRuntime = new Alfresco.forms.Form(this.id);
            this.formsRuntime.setShowSubmitStateDynamically(true, false);
            this.formsRuntime.setSubmitElements(this.buttons.submit);
            
            // setup JSON/AJAX mode if appropriate
            if (this.options.enctype === "application/json")
            {
               this.formsRuntime.setAJAXSubmit(true,
               {
                  successCallback:
                  {
                     fn: this.onJsonPostSuccess,
                     scope: this
                  },
                  failureCallback:
                  {
                     fn: this.onJsonPostFailure,
                     scope: this
                  }
               });
               this.formsRuntime.setSubmitAsJSON(true);
            }
            
            // add any field constraints present
            for (var c = 0; c < this.options.fieldConstraints.length; c++)
            {
               var fc = this.options.fieldConstraints[c];
               this.formsRuntime.addValidation(fc.fieldId, fc.handler, fc.params, fc.event, fc.message);
            }
            
            // fire event to inform any listening components that the form is about to be initialised
            YAHOO.Bubbling.fire("beforeFormRuntimeInit", 
            {
               component: this,
               runtime: this.formsRuntime 
            });
            
            this.formsRuntime.init();
            
            // fire event to inform any listening components that the form has finished initialising
            YAHOO.Bubbling.fire("afterFormRuntimeInit",
            {
               component: this,
               runtime: this.formsRuntime 
            });
         }
      },
      
      /**
       * Default handler used when submit mode is JSON and the sumbission was successful
       *
       * @method onJsonPostSuccess
       * @param response The response from the submission
       */
      onJsonPostSuccess: function FormUI_onJsonPostSuccess(response)
      {
         // TODO: Display the JSON response here by default, when it's returned!
         
         Alfresco.util.PopupManager.displayPrompt(
         {
            text: response.serverResponse.responseText
         });
      },
      
      /**
       * Default handler used when submit mode is JSON and the sumbission failed
       *
       * @method onJsonPostFailure
       * @param response The response from the submission
       */
      onJsonPostFailure: function FormUI_onJsonPostFailure(response)
      {
         var errorMsg = this._msg("form.jsonsubmit.failed");
         if (response.json.message)
         {
            errorMsg = errorMsg + ": " + response.json.message;
         }
         
         Alfresco.util.PopupManager.displayPrompt(
         {
            text: errorMsg
         });
      },
      
      /**
       * Form refresh event handler
       *
       * @method onFormRefresh
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onFormRefresh: function FormUI_onFormRefresh(layer, args)
      {
         // Can't do anything if basic arguments weren't set
         if (this.options.arguments)
         {
            var itemKind = this.options.arguments.itemKind,
               itemId = this.options.arguments.itemId,
               formId = this.options.arguments.formId;
            
            if (itemKind && itemId)
            {
               var fnFormLoaded = function(response, p_formUI)
               {
                  Dom.get(p_formUI.parentId).innerHTML = response.serverResponse.responseText;
               };
               
               var data =
               {
                  htmlid: this.id,
                  formUI: false,
                  mode: this.options.mode,
                  itemKind: itemKind,
                  itemId: itemId,
                  formId: formId
               };

               Alfresco.util.Ajax.request(
               {
                  url: Alfresco.constants.URL_SERVICECONTEXT + "components/form",
                  dataObj: data,
                  successCallback:
                  {
                     fn: fnFormLoaded,
                     obj: this,
                     scope: this
                  },
                  scope: this,
                  execScripts: true
               });
            }
         }
      },
      
      /**
       * Mandatory control value updated event handler
       *
       * @method onMandatoryControlValueUpdated
       * @param layer {object} Event fired
       * @param args {array} Event parameters (depends on event type)
       */
      onMandatoryControlValueUpdated: function FormUI_onMandatoryControlValueUpdated(layer, args)
      {
         // the value of a mandatory control on the page (usually represented by a hidden field)
         // has been updated, force the forms runtime to check if form state is still valid
         this.formsRuntime.updateSubmitElements();
      },
      
      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function FormUI__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.FormUI", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();


/**
 * Helper function to add the current state of the given multi-select list to
 * the given hidden field.
 * 
 * @method updateMultiSelectListValue
 * @param list {string} The id of the multi-select element
 * @param hiddenField {string} The id of the hidden field to populate the value with
 * @param signalChange {boolean} If true a bubbling event is sent to inform any
 *        interested listeners that the hidden field value changed
 * @static
 */
Alfresco.util.updateMultiSelectListValue = function(list, hiddenField, signalChange)
{
   var listElement = YUIDom.get(list);
   
   if (listElement !== null)
   {
      var values = new Array();
      for (var j = 0, jj = listElement.options.length; j < jj; j++)
      {
         if (listElement.options[j].selected)
         {
            values.push(listElement.options[j].value);
         }
      }
      
      YUIDom.get(hiddenField).value = values.join(",");
      
      if (signalChange)
      {
         YAHOO.Bubbling.fire("mandatoryControlValueUpdated", this);
      }
   }
};
