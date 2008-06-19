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

/*
 *** Alfresco.module.AddEvent
*/
(function()
{
   Alfresco.module.AddEvent = function(containerId)
   {
      this.name = "Alfresco.module.AddEvent";
      this.id = containerId;

      this.panel = null;

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["button", "calendar", "container", "connection"], this.componentsLoaded, this);

      return this;
   };

   Alfresco.module.AddEvent.prototype =
   {
		/**
	    * AddModule module instance.
	    *
	    * @property panel
	    * @type Alfresco.module.AddEvent
	    */
		panel: null,
		
		/**
		 * Stores the URI of the event IF an edit is happening 
		 *
		 * @property eventURI
		 * @type String
		 */
		eventURI: null,
		
		/**
       * Sets the current site for this component.
       *
       * @property siteId
       * @type string
       */
		setSiteId: function(siteId)
		{
			this.siteId = siteId;
		},

		/**
		 * Fired by YUILoaderHelper when required component script files have
		 * been loaded into the browser.
		 *
		 * @method onComponentsLoaded
		 */
   		componentsLoaded: function()
      	{
         	/* Shortcut for dummy instance */
         	if (this.id === null)
         	{
            	return;
         	}
      	},

		/**
		 * Renders the event create form. If the form has been previously rendered
		 * it clears the form of any previously entered values otherwise fires off a
		 * request to web script that generates the form.
		 *
		 * @method show
		 */
	   	show: function(uri)
      	{
			var args = {
				"htmlid": this.id,
				"site": this.siteId
			}
			
			if (uri)
			{
				this.eventURI = uri;
				args['uri'] = this.eventURI;
			}
			else 
			{
				this.eventURI = null; // reset
			}
			
			Alfresco.util.Ajax.request(
		    {
		    	url: Alfresco.constants.URL_SERVICECONTEXT + "components/calendar/add-event",
				dataObj: args,
				successCallback:
				{
					fn: this.templateLoaded,
					scope: this
				},
		        failureMessage: "Could not load add event form"
			});
      	},

		/**
		 * Fired when the event create form has loaded successfully.
		 * Sets up the various widgets on the form and initialises the forms runtime.
		 *
		 * @method templateLoaded
		 * @param response {object} DomEvent
		 */
      	templateLoaded: function(response)
      	{
			var div = document.getElementById("addEventPanel");
         	div.innerHTML = response.serverResponse.responseText;

         	this.panel = new YAHOO.widget.Panel(div,
         	{
            	fixedcenter: true,
            	visible: false,
            	constraintoviewport: true
         	});

		 	this.panel.render(document.body);

			if (!this.eventURI) // Create
			{
				// OK Button
				var okButton = Alfresco.util.createYUIButton(this, "ok-button", null,
	         	{
	            	type: "submit"
	         	});
	
				var eventForm = new Alfresco.forms.Form(this.id + "-addEvent-form");
				eventForm.addValidation(this.id + "-title", Alfresco.forms.validation.mandatory, null, "blur");
	         	eventForm.setShowSubmitStateDynamically(true);
		   		eventForm.setSubmitElements(okButton);
				eventForm.setAJAXSubmit(true,
				{
					successCallback:
					{
						fn: this.onCreateEventSuccess,
						scope: this
					}
				});
	       		eventForm.init();
	
				// Initialise the start and end dates to today
				var Dom = YAHOO.util.Dom;
				var today = new Date();

				var dateStr = Alfresco.util.formatDate(today, "dddd, d mmmm yyyy");
				Dom.get("fd").value = dateStr;
				Dom.get("td").value = dateStr;

				var ansiDate = Alfresco.util.formatDate(today, "yyyy/mm/dd");
				Dom.get("from").value = ansiDate;
				Dom.get("to").value = ansiDate;
			}
			else 
			{
				// OK Button
				var okButton = Alfresco.util.createYUIButton(this, "ok-button", this.onOKSelect,
	         	{
	            	type: "push"
	         	});
			}
			
			var cancelButton = Alfresco.util.createYUIButton(this, "cancel-button", this.onCancelButtonClick);

			/**
			 * Button declarations that, when clicked, display
			 * the calendar date picker widget.
			 */
			var startButton = new YAHOO.widget.Button({
				 type: "push",
				 id: "calendarpicker",
				 container: this.id + "-startdate"
			});
			startButton.on("click", this.onDateSelectButton);

			var endButton = new YAHOO.widget.Button({
				type: "push",
				id: "calendarendpicker",
				container: this.id + "-enddate"
			});
			endButton.on("click", this.onDateSelectButton);

			// Display the panel
			this.panel.show();
		},
		
		onOKSelect: function(e)
		{
			if (this.eventURI)
			{
				// TODO: this exists in the forms runtime, currently private
				var form = document.getElementById(this.id + "-addEvent-form");
				if (form)
		        {
		        	var formData = {};
		            var length = form.elements.length;
		            for (var i = 0; i < length; i++)
		            {
		               var element = form.elements[i];
		               var name = element.name;
		               var value = element.value;
		               if (name)
		               {
		                  formData[name] = value;
		               }
		            }
		
					// Submit PUT request 
					Alfresco.util.Ajax.request(
					{
						method: Alfresco.util.Ajax.PUT,
				      	url: Alfresco.constants.PROXY_URI + this.eventURI,
						requestContentType: Alfresco.util.Ajax.JSON,
						dataObj: formData,
						successCallback:
						{
							fn: this.onEventUpdated,
							scope: this
						},
				      	failureMessage: "Update event failed"
				   });
		      	}
			}
		},
		
		onEventUpdated: function(e)
		{
			this.panel.hide();
			// Fire off "eventUpdated" event
			YAHOO.Bubbling.fire('eventUpdated');
		},

		/**
		 * Event handler that gets fired when a user clicks on the date selection
		 * button in the event creation form. Displays a mini YUI calendar.
		 * Gets called for both the start and end date buttons.
		 *
		 * @method onCreateEventSuccess
		 * @param e {object} DomEvent
		 */
		onDateSelectButton: function(e)
		{
			var oCalendarMenu = new YAHOO.widget.Overlay("calendarmenu");
			oCalendarMenu.setBody("&#32;");
			oCalendarMenu.body.id = "calendarcontainer";

			var container = this.get("container");
			// Render the Overlay instance into the Button's parent element
			oCalendarMenu.render(container);

			// Align the Overlay to the Button instance
			oCalendarMenu.align();

   			var oCalendar = new YAHOO.widget.Calendar("buttoncalendar", oCalendarMenu.body.id);
			oCalendar.render();

			oCalendar.changePageEvent.subscribe(function () {
				window.setTimeout(function () {
					oCalendarMenu.show();
				}, 0);
			});

			oCalendar.selectEvent.subscribe(function (type, args) {
				var date;
				var Dom = YAHOO.util.Dom;

				if (args) {
					var prettyId, hiddenId;
					if (container.indexOf("enddate") > -1)
					{
						prettyId = "td";
						hiddenId = "to";
					}
					else
					{
						prettyId = "fd";
					     hiddenId = "from";
					}

					date = args[0][0];
					var selectedDate = new Date(date[0], (date[1]-1), date[2]);

					Dom.get(prettyId).value = Alfresco.util.formatDate(selectedDate, "dddd, d mmmm yyyy");
					Dom.get(hiddenId).value = Alfresco.util.formatDate(selectedDate, "yyyy/mm/d");
				}

				oCalendarMenu.hide();
			});
		},

		/**
		 * Event handler that gets fired when a user clicks
		 * on the cancel button in the event create form.
		 *
		 * @method onCancelButtonClick
		 * @param e {object} DomEvent
		 * @param obj {object} Object passed back from addListener method
		 */
		onCancelButtonClick: function(e, obj)
		{
		     this.panel.hide();
		},

		/**
		 * Event handler that gets fired when an event is (successfully) created.
		 * It in turns fires an 'onEventSaved' event passing in the name and start date
		 * of the newly created event.
		 *
		 * @method onCreateEventSuccess
		 * @param e {object} DomEvent
		 */
	  	onCreateEventSuccess: function(e)
	  	{
			this.panel.hide();

			var result = YAHOO.lang.JSON.parse(e.serverResponse.responseText);
			if (result.event)
			{
				YAHOO.Bubbling.fire('onEventSaved',
				{
					name: result.event.name,
					from: result.event.from,
					start: result.event.start,
					end: result.event.end,
					uri: result.event.uri
				});
			}
	  	}
   };
})();

/* Dummy instance to load optional YUI components early */
new Alfresco.module.AddEvent(null);