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
 * DatePicker component.
 * 
 * @namespace Alfresco
 * @class Alfresco.DatePicker
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
    * DatePicker constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @param {String} currentValueHtmlId The HTML id of the parent element
    * @return {Alfresco.DatePicker} The new DatePicker instance
    * @constructor
    */
   Alfresco.DatePicker = function(htmlId, currentValueHtmlId)
   {
      // Mandatory properties
      this.name = "Alfresco.DatePicker";
      this.id = htmlId;
      this.currentValueHtmlId = currentValueHtmlId;

      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button"], this.onComponentsLoaded, this);
      
      // Initialise prototype properties
      this.widgets = {};

      return this;
   };
   
   Alfresco.DatePicker.prototype =
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
          * The current value
          *
          * @property currentValue
          * @type string
          */
         currentValue: "",
         
         /**
          * Flag to determine whether a time field should be visible
          * 
          * @property showTime
          * @type boolean
          * @default false
          */
         showTime: false,
         
         /**
          * Flag to determine whether the picker is in disabled mode
          *
          * @property disabled
          * @type boolean
          * @default false
          */
         disabled: false,
         
         /**
          * Flag to indicate whether the field is mandatory
          *
          * @property mandatory
          * @type boolean
          * @default false
          */
         mandatory: false
      },

      /**
       * Object container for storing YUI widget instances.
       * 
       * @property widgets
       * @type object
       */
      widgets: null,

      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       * @return {Alfresco.DatePicker} returns 'this' for method chaining
       */
      setOptions: function DatePicker_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component.
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       * @return {Alfresco.DatePicker} returns 'this' for method chaining
       */
      setMessages: function DatePicker_setMessages(obj)
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
      onComponentsLoaded: function DatePicker_onComponentsLoaded()
      {
         Event.onContentReady(this.id, this.onReady, this, true);
      },

      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function DatePicker_onReady()
      {
         var theDate = null;
         
         // calculate current date
         if (this.options.currentValue !== null && this.options.currentValue !== "")
         {
            theDate = Alfresco.util.fromISO8601(this.options.currentValue);
         }
         else
         {
            theDate = new Date();
         }
         
         var page = (theDate.getMonth() + 1) + "/" + theDate.getFullYear();
         var selected = (theDate.getMonth() + 1) + "/" + theDate.getDate() + "/" + theDate.getFullYear();   
         var dateEntry = theDate.toString(this._msg("form.control.date-picker.entry.date.format"));
         var timeEntry = theDate.toString(this._msg("form.control.date-picker.entry.time.format"));
         
         // populate the input fields
         if (this.options.currentValue !== "")
         {
            // show the formatted date
            Dom.get(this.id + "-date").value = dateEntry;
            
            if (this.options.showTime)
            {
               Dom.get(this.id + "-time").value = timeEntry;
            }
         }
         
         // construct the picker
         this.widgets.calendar = new YAHOO.widget.Calendar(this.id, this.id, { title:this._msg("form.control.date-picker.choose"), close:true });
         this.widgets.calendar.cfg.setProperty("pagedate", page);
         this.widgets.calendar.cfg.setProperty("selected", selected);
         
         // setup events
         this.widgets.calendar.selectEvent.subscribe(this._handlePickerChange, this, true);
         Event.addListener(this.id + "-date", "keyup", this._handleFieldChange, this, true);
         Event.addListener(this.id + "-time", "keyup", this._handleFieldChange, this, true);
         Event.addListener(this.id + "-icon", "click", this._showPicker, this, true);
         
         // render the calendar control
         this.widgets.calendar.render();

         // Iw value was set in visible fields, make sure they are validated and put in the hidden field as well 
         if (this.options.currentValue !== "")
         {
            this._handleFieldChange(null);
         }
      },
      
      /**
       * Handles the date picker icon being clicked.
       * 
       * @method _showPicker
       * @param event The event that occurred
       * @private
       */
      _showPicker: function DatePicker__showPicker(event)
      {
         // show the popup calendar widget
         this.widgets.calendar.show();
      },
      
      /**
       * Handles the date being changed in the date picker YUI control.
       * 
       * @method _handlePickerChange
       * @param type
       * @param args
       * @param obj
       * @private
       */
      _handlePickerChange: function DatePicker__handlePickerChange(type, args, obj)
      {
         // update the date field
         var selected = args[0];
         var selDate = this.widgets.calendar.toDate(selected[0]);
         var dateEntry = selDate.toString(this._msg("form.control.date-picker.entry.date.format"));
         Dom.get(this.id + "-date").value = dateEntry;
         
         // update the time field if necessary
         if (this.options.showTime)
         {
            var time = Dom.get(this.id + "-time").value;
            if (time.length > 0)
            {
               var dateTime = Dom.get(this.id + "-date").value + " " + time;
               var dateTimePattern = this._msg("form.control.date-picker.entry.date.format") + " " + this._msg("form.control.date-picker.entry.time.format");
               selDate = Date.parseExact(dateTime, dateTimePattern);
            }
         }
         
         // if we have a valid date, convert to ISO format and set value on hidden field
         if (selDate != null)
         {
            Dom.removeClass(this.id + "-time", "invalid");
            var isoValue = Alfresco.util.toISO8601(selDate, {"milliseconds":true});
            Dom.get(this.currentValueHtmlId).value = isoValue;
            
            if (Alfresco.logger.isDebugEnabled())
               Alfresco.logger.debug("Hidden field '" + this.currentValueHtmlId + "' updated to '" + isoValue + "'");
            
            // inform the forms runtime that the control value has been updated (if field is mandatory)
            if (this.options.mandatory)
            {
               YAHOO.Bubbling.fire("mandatoryControlValueUpdated", this);
            }
         }
         else
         {
            Dom.addClass(this.id + "-time", "invalid");
         }
         
         // hide the popup calendar
         this.widgets.calendar.hide();
      },
      
      /**
       * Handles the date or time being changed in either input field.
       * 
       * @method _handleFieldChange
       * @param event The event that occurred
       * @private
       */
      _handleFieldChange: function DatePicker__handleFieldChange(event)
      {
         var changedDate = Dom.get(this.id + "-date").value;
         if (changedDate.length > 0)
         {
            // convert to format expected by YUI
            var parsedDate = Date.parseExact(changedDate, this._msg("form.control.date-picker.entry.date.format"));
            if (parsedDate != null)
            {
               this.widgets.calendar.select((parsedDate.getMonth() + 1) + "/" + parsedDate.getDate() + "/" + parsedDate.getFullYear());
               var selectedDates = this.widgets.calendar.getSelectedDates();
               if (selectedDates.length > 0)
               {
                  Dom.removeClass(this.id + "-date", "invalid");
                  var firstDate = selectedDates[0];
                  this.widgets.calendar.cfg.setProperty("pagedate", (firstDate.getMonth()+1) + "/" + firstDate.getFullYear());
                  this.widgets.calendar.render();
                  
                  // NOTE: we don't need to check the time value in here as the _handlePickerChange
                  //       function gets called as well as a result of rendering the picker above,
                  //       that's also why we don't update the hidden field in here either.
               }
            }
            else
            {
               Dom.addClass(this.id + "-date", "invalid");
            }
         }
         else
         {
            // when the date is completely cleared remove the hidden field and remove the invalid class
            Dom.removeClass(this.id + "-date", "invalid");
            Dom.get(this.currentValueHtmlId).value = "";
            
            if (Alfresco.logger.isDebugEnabled())
               Alfresco.logger.debug("Hidden field '" + this.currentValueHtmlId + "' has been reset");
            
            // inform the forms runtime that the control value has been updated
            if (this.options.mandatory)
            {
               YAHOO.Bubbling.fire("mandatoryControlValueUpdated", this);
            }
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
      _msg: function DatePicker__msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.DatePicker", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();
