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
 * Alfresco.Calendar
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
       Event = YAHOO.util.Event,
       Element = YAHOO.util.Element;
   var dateFormat = Alfresco.thirdparty.dateFormat;
   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;
   
   Alfresco.CalendarView = function(htmlId)
   {
      this.name = "Alfresco.CalendarView";
      this.id = htmlId;
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);
      
      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["calendar", "button"], this.componentsLoaded, this);
      
      return this;
   };
   
   Alfresco.CalendarView.prototype =
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
        * @default null
        */
         siteId: ""
      },
      /**
       * Object container for storing module instances.
       * 
       * @property modules
       * @type object
       */
        modules: null,      
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       */
      setOptions: function CalendarView_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      /**
       * Set messages for this component
       *
       * @method setMessages
       * @param obj {object} Object literal specifying a set of messages
       */
      setMessages: function(obj)
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
      componentsLoaded: function()
      {
         Event.onContentReady(this.id, this.init, this, true);
      },
      
      /**
       * Fired by YUI when parent element is available for scripting.
       * Initialises components, including YUI widgets.
       *
       * @method init
       */
      init: function()
      {
         YAHOO.Bubbling.on("eventEdited", this.onEventEdited, this);
         YAHOO.Bubbling.on("eventSaved", this.onEventSaved, this);
         YAHOO.Bubbling.on("eventDeleted", this.onEventDeleted, this);
         YAHOO.Bubbling.on("eventResized", this.onEventResized, this);

         YAHOO.Bubbling.on("tagSelected", this.onTagSelected, this);
         YAHOO.Bubbling.on("todayNav", this.onTodayNav, this);
         YAHOO.Bubbling.on("nextNav", this.onNav, this);
         YAHOO.Bubbling.on("prevNav", this.onNav, this);
         YAHOO.Bubbling.on("viewChanged", this.onViewChanged, this);
         
         this.calendarView = this.options.view;
         this.startDate = (YAHOO.lang.isString(this.options.startDate)) ? Alfresco.util.fromISO8601(this.options.startDate): this.options.startDate;
         this.container = Dom.get(this.id);

         this.initDD(); 
         this.initEvents();
         this.initCalendarEvents();
         this.addButton = Alfresco.CalendarHelper.renderTemplate('createEventButton',this.id);
         
         if (this.calendarView !== Alfresco.CalendarView.VIEWTYPE_MONTH)
         {
            this.getEvents(Alfresco.util.formatDate(this.options.startDate,'yyyy-mm-dd'));
         }
         else 
         {
            var events = [];
            for (var event in this.events)
            {
               events.push(this.events[event].getData().registry);
            }
            //have to delay as mini calendar hasn't registered for event yet as it's not loaded yet.
            YAHOO.lang.later(1000,this,function(events)
            { 
               YAHOO.Bubbling.fire("eventDataLoad",events);
            },
            [events]);
         }
         this.isShowingEarlyRows = false;
         this.titleEl = Dom.get('calTitle');
         switch(this.calendarView)
         {
            case Alfresco.CalendarView.VIEWTYPE_MONTH:
                this.titleEl.innerHTML = Alfresco.util.formatDate(this.options.titleDate,'mmmm yyyy');
                break;
            case Alfresco.CalendarView.VIEWTYPE_WEEK:
                this.titleEl.innerHTML = Alfresco.util.formatDate(this.options.titleDate,'d mmmm yyyy');
                break;
            case Alfresco.CalendarView.VIEWTYPE_DAY:
                this.titleEl.innerHTML = Alfresco.util.formatDate(this.options.titleDate,'dddd, d mmmm');
                break;
         }
         //highlight current date
         if (this.calendarView===Alfresco.CalendarView.VIEWTYPE_MONTH)
         {
            var now = new Date();
            if (this.options.startDate.getFullYear()===now.getFullYear() && (this.options.startDate.getMonth()==now.getMonth()))
            {
              var el = Dom.get('cal-'+(Alfresco.util.toISO8601(now).split('T')[0]));
              Dom.addClass(el,'current');
            }
         }

         if (!YAHOO.widget.DateMath.HOUR)
         {
            YAHOO.widget.DateMath.add = function()
            {
                var origAddFunc = YAHOO.widget.DateMath.add;
                YAHOO.widget.DateMath.HOUR = 'H';
                YAHOO.widget.DateMath.SECOND = 'S';
                YAHOO.widget.DateMath.MINUTE = 'Mn';
                return function(date,field,amount)
                {

                    switch(field){
                        case YAHOO.widget.DateMath.MONTH:
                        case YAHOO.widget.DateMath.DAY:
                        case YAHOO.widget.DateMath.YEAR:
                        case YAHOO.widget.DateMath.WEEK:
                            return origAddFunc.apply(YAHOO.widget.DateMath,arguments);
                            break;
                        case YAHOO.widget.DateMath.HOUR:
                            var newHour = date.getHours()+amount;
                            var day = 0;
                            if (newHour < 0)
                            {
                                while(newHour < 0)
                                {
                                    newHour+=24;
                                    day-=1;

                                }
                                // newHour = 23;
                            }
                            if (newHour > 24)
                            {
                                while(newHour > 24)
                                {
                                    newHour-=24;
                                    day+=1;

                                }
                            }
                            YAHOO.widget.DateMath._addDays(date,day);
                            date.setHours(newHour);                                
                            break;
                        case YAHOO.widget.DateMath.MINUTE:
                            date.setMinutes(date.getMinutes()+amount);
                            break;
                        case YAHOO.widget.DateMath.SECOND:
                            date.setMinutes(date.getSeconds()+amount);
                        
                    }
                    return date;
                };
            }();
         }
      },
      
      /**
       * Initialises drag and drop targets.
       * 
       * @method initDD
       */
      initDD : function() {
          this.dragGroup = (this.calendarView===Alfresco.CalendarView.VIEWTYPE_MONTH) ? 'day' : 'hourSegment';

          var dragTargets = Dom.getElementsByClassName(this.dragGroup,'div',YAHOO.util.Dom.get(this.options.id));
          dragTargets = dragTargets.concat(Dom.getElementsByClassName('target','div',YAHOO.util.Dom.get(this.options.id)));
          this.dragTargetRegion = YAHOO.util.Dom.getRegion(dragTargets[0]);

          for (var i=0,el;el=dragTargets[i];i++) 
          {
              new YAHOO.util.DDTarget(el, this.dragGroup);
          }
      },
      
      /**
       * initialise config object for calendar events
       * 
       * @method initCalendarEvents
       *  
       */
      initCalendarEvents : function() {
         var tickSize = (this.dragTargetRegion.bottom-this.dragTargetRegion.top)/2;
            this.calEventConfig = {
                //work out div.hourSegment half-height so we can get xTick value for resize
                resize: {
                    xTicks :  tickSize
                },
                yTick : (this.calendarView!==Alfresco.CalendarView.VIEWTYPE_MONTH) ? tickSize : null,
                xTick : (this.calendarView!==Alfresco.CalendarView.VIEWTYPE_MONTH) ? 100 : null,
                view  : this.calendarView,
                resizable : ((this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK) | (this.calendarView===Alfresco.CalendarView.VIEWTYPE_DAY))
            };
         var vEventEls = Dom.getElementsByClassName('vevent',null,YAHOO.util.Dom.get(this.options.id));
         var numVEvents = vEventEls.length;
         this.events = [];
         if (this.calendarView===Alfresco.CalendarView.VIEWTYPE_DAY)
         {
            this.calEventConfig.performRender = false;
         }
         for (var i=0;i<numVEvents;i++)
         {
            var vEventEl = vEventEls[i];
            var id = Event.generateId(vEventEl);
            vEventEl.id = id;
            if ((this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK) | (this.calendarView===Alfresco.CalendarView.VIEWTYPE_DAY)) {
                this.calEventConfig.resizable = (Dom.hasClass(vEventEl,'allday')) ? false : true;
            }
              this.calEventConfig.draggable = Dom.hasClass(vEventEl,'allday') ? false : true;
              this.events[id] = new Alfresco.calendarEvent(vEventEl, this.dragGroup,this.calEventConfig);
              this.events[id].on('eventMoved', this.onEventMoved, this.events[id], this);

            if ((this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK) | (this.calendarView===Alfresco.CalendarView.VIEWTYPE_DAY))
            {
                this.adjustHeightByHour(vEventEl);
            }
        }
      },
      
      /**
       * Initialises event handling
       * All events are handled through event delegation via the onInteractionEvent handler
       * 
       * @method initEvents
       * 
       */
      initEvents : function()
      {
          Event.on(this.id,'click',this.onInteractionEvent,this,true);
          Event.on(this.id,'dblclick',this.onInteractionEvent,this,true);
          if (this.calendarView == Alfresco.CalendarView.VIEWTYPE_MONTH) 
          {
            Event.on(this.id,'mouseover',this.onInteractionEvent,this,true);
            Event.on(this.id,'mouseout',this.onInteractionEvent,this,true);
          }
      },
      
      /**
       * Retrieves events from server
       * 
       * @method getEvents
       *  
       */
      getEvents : function()
      {
          Alfresco.util.Ajax.request(
          {
            url: Alfresco.constants.PROXY_URI + "calendar/events/user",
            dataObj:
            {
               from: Alfresco.util.toISO8601(this.options.startDate).split('T')[0]
            },
            
            successCallback: //filter out non relevant events for current view
            {
               fn: function(o) 
               {
                  // var filterThreshold = null;
                  //                   if (this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK)
                  //                   {
                  //                       filterThreshold = YAHOO.widget.DateMath.getWeekNumber(this.options.startDate);
                  //                   }
                  //                   if (this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK)
                  //                   {
                  //                       filterThreshold = YAHOO.widget.DateMath.getWeekNumber(this.options.startDate);
                  //                   }
                  var data = YAHOO.lang.JSON.parse(o.serverResponse.responseText).events;
                  var events = [];
                  var comparisonFn = null;
                  switch(this.calendarView)
                  {
                    case Alfresco.CalendarView.VIEWTYPE_DAY:
                        comparisonFn = function(d) {
                            var day = this.options.startDate.getDate();
                            var m = this.options.startDate.getMonth();
                            var y = this.options.startDate.getFullYear();

                            return function(d)
                            {
                                return ((d.getMonth()===m) && (y === d.getFullYear()) && (d.getDate()===day) );
                            };
                        }.apply(this);
                        break; 
                    case Alfresco.CalendarView.VIEWTYPE_WEEK:
                        comparisonFn = function() {
                            //have to use a copy as YAHOO.widget.DateMath.getWeekNumber
                            //clears the time of the specified date 
                            var startDateCopy = new Date();
                            startDateCopy.setTime(this.options.startDate.getTime());
                            var currentWeek = YAHOO.widget.DateMath.getWeekNumber(startDateCopy);
                            var y = this.options.startDate.getFullYear();
                            return function(d)
                            {
                                return ((y === d.getFullYear()) && (YAHOO.widget.DateMath.getWeekNumber(d)===currentWeek) ) ;
                            };
                        }.apply(this);
                        break; 
                    case Alfresco.CalendarView.VIEWTYPE_MONTH:
                        break; 
                    case Alfresco.CalendarView.VIEWTYPE_AGENDA:
                        comparisonFn = function() {
                            
                            var m = this.options.startDate.getMonth()+1;
                            var y = this.options.startDate.getFullYear();
                            return function(d)
                            {
                                return ((y === d.getFullYear()) && (m==(d.getMonth()+1)) );
                            };
                        }.apply(this);
                        break;
                  }
                  for (var i=0;i<data.length;i++){
                    var ev = data[i];
                    var date = new Date(Date.parse(ev.when));
                    if (comparisonFn(date))
                    {
                        var datum = {};
                        datum.desc = ev.description || '';
                        datum.name = ev.title;
                        datum.where = ev.where;
                        datum.contEl = 'div';
                        datum.from = dateFormat(date,dateFormat.masks.isoDate)+'T'+ev.start;
                        datum.to =dateFormat(date,dateFormat.masks.isoDate)+'T'+ev.end; 
                        datum.uri = '/calendar/event/'+this.options.siteId+'/'+ev.name;
                        datum.hidden ='';
                        datum.allday = '';
                        datum.el = 'div';
                        datum.duration = Alfresco.CalendarHelper.getDuration(Alfresco.util.fromISO8601(datum.from),Alfresco.util.fromISO8601(datum.to));
                        datum.key = datum.from.split(":")[0]+':00';
                        datum.start = ev.start;
                        datum.end = ev.end;
                        datum.tags = ev.tags;
                        events.push(datum);
                    };
                  };
                  this.addEvents(events);
               },
               scope: this
            },
               failureMessage: Alfresco.util.message("load.fail", "Alfresco.CalendarView")
           });
      },
      
      /**
       * Add events to DOM
       *  
       * @method addEvents
       * 
       */
      addEvents : function(events) 
      {
        var view = this.calendarView;
        if ( (view===Alfresco.CalendarView.VIEWTYPE_WEEK)  | (view===Alfresco.CalendarView.VIEWTYPE_DAY))
        {
          var offsetTop = 0;
          var min;
          var getTargetEl = function(ev)
          {
            return function(ev) {

                var segments  = Dom.getElementsByClassName('hourSegment','div',Dom.get('cal-'+ev.key));
                var min = parseInt(ev.from.split('T')[1].split(':')[1],10);
                var targetEl = (min>=30) ? segments[1] : segments[0];
                //on the hour or half hour
                if (min%2!==0)
                {
                    var reg = Dom.getRegion(segments[0]);
                    offsetTop = parseInt((reg.bottom-reg.top)/2,10);
                }
                return targetEl;
            };                
          }(this.calendarView);
        
          var len = events.length;
          var tdsWithEvents = [];
          for (var i = 0;i<len;i++)
          {
              var ev = events[i];
              var vEventEl = Alfresco.CalendarHelper.renderTemplate('vevent',ev);
              var id = Event.generateId(vEventEl);
              vEventEl.id = id;
              //all day
              if (ev.start===ev.end) {
                this.renderAllDayEvents(vEventEl,ev);
              }
              else {
                var targetEl = getTargetEl(ev);

                if (targetEl)
                {
                    targetEl.appendChild(vEventEl);                
                }
                Dom.setStyle(vEventEl,'top',offsetTop+'px');              

                var td = Dom.getAncestorByTagName(vEventEl,'td');

                //maintain list of tds in with events have been added
                if (!tdsWithEvents[td.id])
                {
                  tdsWithEvents[td.id]=td;
                }    
              }
          }
          //render multiple events correctly
          for (var td in tdsWithEvents)
          {
            this.renderMultipleEvents(tdsWithEvents[td]);
          }
        }
        else if (view === Alfresco.CalendarView.VIEWTYPE_AGENDA)
        {
          //sort events by day
          var sortedEvents = [];
          var numEvents = events.length;
          
          for (var i=0;i<numEvents;i++)
          {
            var event = events[i];
            var date = event.from.split('T')[0];
            if (!sortedEvents[date])
            {
              sortedEvents[date]=[];
            }
            sortedEvents[date].push(event);
          }
          //render
          var appendNode = Dom.getElementsByClassName('agendaview')[0];
          for (var event in sortedEvents)
          {
            var date = Alfresco.util.formatDate(Alfresco.util.fromISO8601(sortedEvents[event][0].from),'dddd, d mmmm');
            var header = Alfresco.CalendarHelper.renderTemplate('agendaDay',{date:date});
            var eventsHTML = '';
            var ul = document.createElement('ul');
            var agendaEvts = sortedEvents[event];
            for (var i = 0; i<agendaEvts.length;i++)
            {
              var event = agendaEvts[i];
              event.hidden='';
              event.contel = 'div';
              event.el = 'li';
              ul.appendChild(Alfresco.CalendarHelper.renderTemplate('vevent',event));
            }
            appendNode.appendChild(header);
            appendNode.appendChild(ul);
          }          
        }
        this.initCalendarEvents();
        YAHOO.Bubbling.fire("eventDataLoad",events); 
      },

      /**
       * Event Delegation handler. Delegates to correct handlers using CSS selectors
       *
       * @method onInteractionEvent
       * @param e {object} DomEvent
       * @param args {array} event arguments
       */
      onInteractionEvent: function(e,args) {
        var elTarget = Event.getTarget(e);
        
        if (e.type === 'mouseover'){
          if ( YAHOO.util.Selector.test(elTarget, 'div.'+this.dragGroup) ) {
              Dom.addClass(elTarget,'highlight');
              elTarget.appendChild(this.addButton);
          }
        }
        else if (e.type === 'mouseout'){
          if ( YAHOO.util.Selector.test(elTarget, 'div.'+this.dragGroup) ) {
              Dom.addClass(elTarget,'highlight');
          }
        }
        
        if (e.type==='click') 
        {
          if (YAHOO.util.Selector.test(elTarget, 'a#collapseTriggerLink'))
          {
              this.toggleEarlyTableRows();
              YAHOO.util.Event.preventDefault(e);                
          }
          if ( YAHOO.util.Selector.test(elTarget, 'button#addEventButton') )
          {
              this.showAddDialog(elTarget);
          }
          else if ( YAHOO.util.Selector.test(elTarget, 'a.summary') )
          {
              this.showDialog(e,elTarget);
          }
          if ( YAHOO.util.Selector.test(elTarget, 'li.moreEvents a') )
          {
              this.onShowMore(e,args,elTarget);
          }
        }
      },
      /**
       *  Adds events in month view
       * 
       * @method _addEventInMonthView
       * @param targetEl {object} Element in which to add event
       * @param data {object} Value object of event data
       * @param vEventEl {object} reference to exising event element
       * 
       * @return vEventEl {object} reference to event element
       */
      _addEventInMonthView : function(targetEl,data,vEventEl)
      {
        var ul = targetEl.getElementsByTagName('ul');
        var elUl = null;
        var someHidden = false;
        //if allday remove from view and re-add
        if (data.allday!==true | data.allday!=='true')
        {
          data.el = 'li';  
          data.contEl = 'div';
        }
        else {
          data.el = 'div';
          data.contEl='div';
          vEventEl.parentNode.removeChild(vEventEl);
          vEventEl=null;
        }
        if (data.dtstart) { // have to convert
          data.fromDate = data.dtstart;
          data.toDate = data.dtend;
          data.where = data.location;
          data.desc = data.description;
          data.name = data.summary;
          data.duration = Alfresco.CalendarHelper.getDuration(Alfresco.util.fromISO8601(data.dtstart),Alfresco.util.fromISO8601(data.dtend));
          data.start = data.dtstart.split('T')[1].substring(0,5);
          data.end = data.dtend.split('T')[1].substring(0,5);
          data.allday = (data.allday==='true') ? 'allday' : '';
          data.tags = (YAHOO.lang.isArray(data.tags)) ? data.tags.join(' ') : data.tags; 
        }

        //day has no event so add ul
        if (ul.length === 0) {
          elUl = document.createElement('ul');
          Dom.addClass(elUl,'dayEvents');
          elUl = targetEl.appendChild(elUl);
        }
        else 
        { 
          elUl = ul[0];
        }
        var dayEvents = elUl.getElementsByTagName('li');
        if (dayEvents.length>=5)
        {
          data.hidden = 'hidden';
          someHidden = true;
        }
        else data.hidden = '';
        if (data.tags && YAHOO.lang.isArray(data.tags))
        {
          data.tags = data.tags.join(' ');
        }
        var vEventEl = vEventEl || Alfresco.CalendarHelper.renderTemplate('vevent',data);
        if (data.allday==='allday')
        {
          elUl.parentNode.appendChild(vEventEl);
          return vEventEl;
        }
        else if (YAHOO.util.Dom.getElementsByClassName('moreEvents','li',elUl).length>0)
        {
          elUl.insertBefore(vEventEl,dayEvents[dayEvents.length-1]);
        }
        else {
          elUl.appendChild(vEventEl);
          if (someHidden) {
            elUl.innerHTML +='<li class="moreEvents"><a href="">+ 5 More</a></li>';
          }
        }
        return vEventEl;
      },
      
      /**
       * Render all day events
       * 
       * @method renderAllDayEvents
       * @param eventEl {object} reference to event element
       * @param data {object} Value object of event data
       * 
       */
      renderAllDayEvents : function(eventEl,data) 
      {
        // put into all day section
        if (this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK)
        {
          var dayOfWeek = Alfresco.util.fromISO8601(data.dtstart || data.from).getDay();
          targetEl = Dom.getElementsByClassName('alldayRow','tr')[0].getElementsByTagName('td')[dayOfWeek].getElementsByTagName('div')[0];
        }
        else if (this.calendarView===Alfresco.CalendarView.VIEWTYPE_DAY)
        { 
          targetEl = Dom.get('allday');
        } 
        else if ((this.calendarView===Alfresco.CalendarView.VIEWTYPE_MONTH))
        {
          targetEl = Dom.get(eventEl.id);
          targetEl = Dom.getElementsByClassName('day','div',targetEl)[0];
          event = this._addEventInMonthView(targetEl,data,eventEl);
        }
        if ((this.calendarView===Alfresco.CalendarView.VIEWTYPE_DAY) | (this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK))
        {
          targetEl.appendChild(eventEl);
        }  
        if ((this.calendarView!==Alfresco.CalendarView.VIEWTYPE_AGENDA))
        {
          Dom.addClass(eventEl,'allday');
          // Dom.setStyle(eventEl,'width','100%');
          Dom.setStyle(eventEl,'height','auto');
          Dom.setStyle(eventEl,'top','auto');
          Dom.setStyle(eventEl,'left','auto');              
        }
      },
      
      /**
       * Handler for eventEdited event. Updates event in DOM in response to updated event data.
       * 
       * @method  onEventEdited
       * 
       * @param e {object} event object
       * @param o {object} new event data
       *  
       */
      onEventEdited : function(e,o) 
      {
        Alfresco.util.PopupManager.displayMessage(
        {
           text: Alfresco.util.message('message.edited.success',this.name)
        });
        var data = o[1].data;
        var id = o[1].id;

        var event = null;
        if (this.events[id]) {
            event = this.events[id];
        }
        
        var eventEl = event.getEl();          
        var targetEl = null;
        var dateParts = data.dtstart.split('T');
        var hour = dateParts[1].split(':')[0];
        var min =  dateParts[1].split(':')[1];
        var id = 'cal-'+dateParts[0];
        //test if event is valid for view must be within startdate and (enddate-1 second) of current view
        var evDate = Alfresco.util.fromISO8601(data.dtstart);
        if ( (evDate.getTime()<this.options.startDate.getTime()) | (evDate.getTime()>this.options.endDate.getTime()))
        {
          if (this.events[eventEl.id])
          {
            delete this.events[eventEl.id];              
          }
          eventEl.parentNode.removeChild(eventEl);
        }
        else {
          if(data.allday && data.allday!='false')
          {
            this.renderAllDayEvents(eventEl,data);

          }
          else 
          { 
            //move to correct cell
            Dom.removeClass(eventEl,'allday');
            
            if ((this.calendarView===Alfresco.CalendarView.VIEWTYPE_MONTH))
            {
              targetEl = Dom.get(id);
              targetEl = Dom.getElementsByClassName('day','div',targetEl)[0];
              eventEl = this._addEventInMonthView(targetEl,data,eventEl);
            }
            if ((this.calendarView===Alfresco.CalendarView.VIEWTYPE_WEEK) | (this.calendarView===Alfresco.CalendarView.VIEWTYPE_DAY))
            {
              
              id += 'T'+hour+':00';
              var index = (parseInt(min,10)>=30) ? 1 : 0;
              targetEl = Dom.get(id);
              targetEl = Dom.getElementsByClassName('hourSegment','div',targetEl)[index];
              targetEl.appendChild(eventEl);
              
              data.duration = Alfresco.CalendarHelper.getDuration(Alfresco.util.fromISO8601(data.dtstart),Alfresco.util.fromISO8601(data.dtend));
              this.events[eventEl.id] = new Alfresco.calendarEvent(eventEl, this.dragGroup,this.calEventConfig);
            }
          }       
        }
        if (data.tags)
        {
          data.category=data.tags;          
        }

        this.events[eventEl.id].update(data);
        // Refresh the tag component
        YAHOO.Bubbling.fire("tagRefresh");
        
      },
      
      /**
       * Handler for when event is saved
       * 
       * @method onEventSaved
       * 
       * @param e {object} event object 
       */
      onEventSaved : function (e)
      {
        Alfresco.util.PopupManager.displayMessage(
        {
           text: Alfresco.util.message('message.created.success',this.name)
        });
        
        var data = YAHOO.lang.JSON.parse(e.serverResponse.responseText).event;

        
        var dtStartDate = Alfresco.util.fromISO8601(data.from+'T'+data.start);
        var dtEndDate = Alfresco.util.fromISO8601(data.to+'T'+data.end);
        data.duration = Alfresco.CalendarHelper.getDuration(dtStartDate,dtEndDate);

        //tagname
        data.el = 'div';  
        //tag with enclosing brackets
        data.contEl = 'div';
        data.hidden ='';
        data.tags = data.tags.join(' ');
        data.allday = (YAHOO.lang.isUndefined(data.allday)) ? '' : data.allday;

        data.from = data.from +'T'+data.start;
        data.to = data.to +'T'+data.end;

        //get containing date TD cell for event
        var targetEl = Dom.get('cal-'+data.from.split(':')[0]+':00');
        //render into allday section
        if(data.allday)
        {

        }
        if (this.calendarView === Alfresco.CalendarView.VIEWTYPE_MONTH)
        {
            targetEl = Dom.get('cal-'+data.from.split('T')[0]);
            targetEl = Dom.getElementsByClassName('day','div',targetEl)[0];
            var vEventEl = this._addEventInMonthView(targetEl,data);
            
        }
        else {
            var vEventEl = Alfresco.CalendarHelper.renderTemplate('vevent',data);
            var min = data.from.split('T')[1].split(':')[1];
            var segments  = Dom.getElementsByClassName('hourSegment','div',targetEl);
            targetEl = (parseInt(min,10)>=30) ? segments[1] : segments[0];
            targetEl.appendChild(vEventEl);
        }
        
        var id = Event.generateId(vEventEl);
        var newCalEvent = new Alfresco.calendarEvent(vEventEl, this.dragGroup,YAHOO.lang.merge(this.calEventConfig,{performRender:false}));
        this.events[id]=newCalEvent;

        newCalEvent.on('eventMoved', this.onEventMoved, newCalEvent, this);
        this.adjustHeightByHour(vEventEl);
        
        YAHOO.Bubbling.fire("eventSaved",data);
        // Refresh the tag component
        YAHOO.Bubbling.fire("tagRefresh");
      },
      
      /**
       * Handler for when an event is deleted
       * 
       * @method  onEventDeleted
       *  
       */
      onEventDeleted : function ()
      {
        Alfresco.util.PopupManager.displayMessage(
        {
           text: Alfresco.util.message('message.deleted.success',this.name)
        });
        
        var id = arguments[1][1].id;
        if (this.calendarView === Alfresco.CalendarView.VIEWTYPE_MONTH)
        {
          var evt = this.events[id].getEl();
          var el = Dom.getNextSibling(evt);
          if (Dom.hasClass(el,'hidden') ) {
              Dom.removeClass(el,'hidden');
          }
          
          if (Dom.hasClass(el,'moreEvents') ) {
              Event.purgeElement(el,true);
              el.parentNode.removeChild(el);
          }
          Event.purgeElement(evt,true);
          evt.parentNode.removeChild(evt);
        }
        else {
            this.events[id].deleteEvent();
        }
        Event.purgeElement(this.events[id].getEl(),true);          
        delete this.events[id];
        // Refresh the tag component
        YAHOO.Bubbling.fire("tagRefresh");
      },
      
      /**
       * Handler for when today button is clicked
       * 
       * @method onTodayNav
       * 
       */
      onTodayNav : function() 
      {
        var today = new Date();
        var params = Alfresco.util.getQueryStringParameters();
        params.date = today.getFullYear() + '-'+Alfresco.CalendarHelper.padZeros(parseInt(today.getMonth(),10)+1)+'-'+Alfresco.CalendarHelper.padZeros(today.getDate());
        window.location = window.location.href.split('?')[0] + Alfresco.util.toQueryString(params);          
      },
      
      /**
       * Handler for when calendar view is changed (day|week|month button is clicked)
       * 
       * @method onViewChanged
       *  
       */
      onViewChanged : function() 
      {
        var views = [Alfresco.CalendarView.VIEWTYPE_DAY,Alfresco.CalendarView.VIEWTYPE_WEEK,Alfresco.CalendarView.VIEWTYPE_MONTH,Alfresco.CalendarView.VIEWTYPE_AGENDA];
        var params = Alfresco.util.getQueryStringParameters();
        params.view = views[arguments[1][1].activeView];
        window.location = window.location.href.split('?')[0] + Alfresco.util.toQueryString(params);
      },

      /**
       * Handler for when previous or next button is clicked
       * 
       * @method onNav 
       * @param e {object}
       *  
       */
      onNav : function(e) 
      {
        var increment = 1;
        if (e==='prevNav') {
            increment = -1;
        }
        var date = YAHOO.widget.DateMath.add(this.options.startDate,YAHOO.widget.DateMath[this.calendarView.toUpperCase()],increment);
        var params = Alfresco.util.getQueryStringParameters();
        params.date = Alfresco.util.formatDate(date,'yyyy-mm-dd');
        var newLoc = window.location.href.split('?')[0] + Alfresco.util.toQueryString(params);
        window.location = newLoc;
      },

      /**
       * Handler for when a tag is selected
       * 
       * @method onTagSelected
       *  
       */
      onTagSelected : function (e,args)
      {
        var tagName = arguments[1][1].tagname;
        var showAllTags = false;
        //all tags
        if (tagName == Alfresco.util.message('label.all-tags','Alfresco.TagComponent'))
        {
          for (var event in this.events)
          {
            this.events[event].show();
          }
          showAllTags = true;
        }
        else {
          for (var event in this.events)
          { 
            var event = this.events[event];
            var eventTags = event.getData('category',true);
            if (YAHOO.lang.isArray(eventTags))
            {
               (Alfresco.util.arrayContains(eventTags,tagName)) ? event.show() : event.hide();
            }
            else 
            {
              event.hide();
            }
          }          
        }
        //add tag info to title
        var tagTitleEl = this.titleEl.getElementsByTagName('span');
        
        if (tagTitleEl.length>1)
        {
          this.titleEl.removeChild(tagTitleEl[0]);
        }
        if (!showAllTags)
        {
          tagTitleEl = Alfresco.CalendarHelper.renderTemplate('taggedTitle',{taggedWith:this._msg('label.tagged-with'),tag:tagName});
          this.titleEl.appendChild(tagTitleEl);          
        }

      },

      /**
       * Adjusts height of specifed event depending on its duration
       *  
       * @method adjustHeightByHour
       * @param el {object} Event element to adjust
       */
      adjustHeightByHour : function(el)
      {
        //TODO - get this from css class;
        var hourHeight = 4.75; //em
        //adjust height dependant on durations
        if (this.calendarView != Alfresco.CalendarView.VIEWTYPE_MONTH)
        {
          var durationObj = hcalendar.parsers['duration'](this.events[el.id].getData('duration'));
          if (durationObj)
          {
            var height = (hourHeight*durationObj.H);
            if (durationObj.M){
                height += (hourHeight*(1/(60/durationObj.M)));
            }
            if (el && height)
            {
              Dom.setStyle(el,'height',height+'em');              
            }
          }
        }  
      },

      /**
       * Displays add dialog
       * 
       * @method showAddDialog
       * @param elTarget {object} Element in which the event occured (click)
       *  
       */
      showAddDialog : function(elTarget) {
          if (YAHOO.lang.isUndefined(elTarget))
          {
              if (this.calendarView === Alfresco.CalendarView.VIEWTYPE_MONTH)
              {
                  elTarget = Dom.get('cal-'+Alfresco.util.toISO8601(this.options.startDate).split('T')[0]);
              }
              else if (this.calendarView !== Alfresco.CalendarView.VIEWTYPE_AGENDA)
              {
                  elTarget = Dom.get('cal-'+Alfresco.util.toISO8601(this.options.startDate).split(':')[0]+':00');
              }
          }
          if (this.calendarView !== Alfresco.CalendarView.VIEWTYPE_AGENDA)
          {
            this.currentDate = this.getClickedDate(elTarget);
          }
          else 
          {
            this.currentDate = this.options.startDate;
          }
          if (!this.eventDialog)
          {
              this.eventDialog = Alfresco.util.DialogManager.getDialog('CalendarView.addEvent');
              this.eventDialog.id = this.id+ "-addEvent";
              if (this.eventDialog.tagLibrary == undefined)
              {
                 this.eventDialog.tagLibrary = new Alfresco.module.TagLibrary( this.eventDialog.id);
                 this.eventDialog.tagLibrary.setOptions({ siteId: this.options.siteId });
              }
          }
          var options = 
          {
               site : this.options.siteId,
               displayDate : this.currentDate,
               actionUrl : Alfresco.constants.PROXY_URI+ "/calendar/create",
               templateUrl: Alfresco.constants.URL_SERVICECONTEXT + "components/calendar/add-event",
               templateRequestParams : {
                      site : this.options.siteId
               },
               doBeforeFormSubmit : 
               {
                 fn : function(form, obj)
                      {                           
                        // Update the tags set in the form
                        this.tagLibrary.updateForm(this.id + "-form", "tags");
                        // Avoid submitting the input field used for entering tags
                        var tagInputElem = YAHOO.util.Dom.get(this.id + "-tag-input-field");
                        if (tagInputElem)
                        {
                           tagInputElem.disabled = true;
                        }
                      },
                 scope:this.eventDialog
               },
               doBeforeAjaxRequest : {
                   fn : function(p_config, p_obj) 
                        {
                            if (p_config.dataObj.tags)
                            {
                              p_config.dataObj.tags = p_config.dataObj.tags.join(' ');
                            }
                            return true;
                        },
                   scope : this.eventDialog
               },
               onSuccess : {
                  fn : this.onEventSaved,
                  scope : this
               },
               onFailure : {
                   fn:function()
                   {
                       Alfresco.util.PopupManager.displayMessage(
                       {
                          text: Alfresco.util.message('message.created.failure',this.name)
                       });
                   },
                   scope:this
               }
           };
           this.eventDialog.setOptions(options);
           this.eventDialog.show();
      },

      /**
       * shows edits or add dialog depending on source of event
       * 
       * @method showDialog
       * @param e {object} Event object
       * @param elTarget {object} Element in which event occured 
       *  
       */
      showDialog : function(e,elTarget)
      {
          //show create event dialog
          if (YAHOO.util.Selector.test(elTarget, 'button#addEventButton')  )
          {
            this.showAddDialog(elTarget);
          }
          //show edit dialog
          else if ( YAHOO.util.Selector.test(elTarget, 'a.summary')  )
          {
            var elPar = Dom.getAncestorByClassName(elTarget,'vevent');
            this.editEvent = this.events[elPar.id];

            var div = document.createElement('div');
            div.id = 'eventInfoPanel';
            document.body.appendChild(div);
            var panel = new Alfresco.EventInfo(this.id + "");
            // panel.event = elTarget.parentNode.id;
            
            panel.setOptions(
              {
               siteId : this.options.siteId,
               eventUri : 'calendar/'+elTarget.href.split('/calendar/')[1],
               displayDate : this.currentDate,
               event  : Dom.getAncestorByClassName(elTarget,'vevent').id
              }
            );
            panel.show(); // event object 
          }
       
          YAHOO.util.Event.preventDefault(e);
          
      },
      
      /**
       * Returns the current date that the user clicked on
       * 
       * @method getClickedDate
       * @param el {DOMElement} the element that was clicked on
       * @returns {Date}
       */
      getClickedDate : function(el) 
      {
          if (el.nodeName.toUpperCase()!=='TD')
          {
              el = Dom.getAncestorByTagName(el,'td');
          }
          return Alfresco.util.fromISO8601(el.id.replace('cal-',''));
      },
      
      /**
       * Handler for when an event is moved(dragged). Updates DOM with new event data
       *
       * @method onEventMoved
       * @param args {object} Event arguments
       * @param calEvent {object} CalendarEvent object - the moved event
       * 
       */
      onEventMoved : function(args,calEvent) 
      {

        var calEventEl = calEvent.getEl();
        var targetEl = arguments[0].targetEl || calEventEl;

        var timeReplace = /T([0-9]{2}):([0-9]{2})/;
        var dateReplace = /^([0-9]{4})-([0-9]{2})-([0-9]{2})/;

        this.currentDate = this.getClickedDate(targetEl);

        var date = Alfresco.util.toISO8601(this.currentDate);
        var newDtStart = calEvent.getData('dtstart');
        if (date !== null)
        {
            newDtStart = newDtStart.replace(dateReplace,date.split('T')[0]);
        }
        if ((this.calendarView  === Alfresco.CalendarView.VIEWTYPE_DAY) | (this.calendarView  === Alfresco.CalendarView.VIEWTYPE_WEEK) ) {
            var hour = Alfresco.CalendarHelper.determineHourSegment(Dom.getRegion(calEventEl),targetEl);
            newDtStart = newDtStart.replace(timeReplace,'T'+hour);
        }
        var newEndDate = Alfresco.CalendarHelper.getEndDate(newDtStart,calEvent.getData('duration',true));

        calEvent.update({
            dtstart : newDtStart,
            dtend : newEndDate
        });
        if (args.dropped)
        {
          this.updateEvent(calEvent);
          //new day/cell
          this.renderMultipleEvents(Dom.getAncestorByTagName(targetEl,'td'));
          //previous day/cell
          this.renderMultipleEvents(arguments[0].previousTargetEl);
        }
      },
      
      /**
       * 
       * Updates event to database
       * 
       * @method updateEvent
       * @param calEvent {object} The CalendarEvent object to update 
       */
      updateEvent : function(calEvent)
      {

        var eventUri = Dom.getElementsByClassName('summary','a',calEvent.getEl())[0].href;
        var dts  = Alfresco.util.fromISO8601(calEvent.getData('dtstart'));
        var dte  = Alfresco.util.fromISO8601(calEvent.getData('dtend'));
        var dataObj = {
            "site" : this.options.siteId,
            "page":"calendar",
            "from":Alfresco.util.formatDate(dts, "dddd, d mmmm yyyy"),
            "to":Alfresco.util.formatDate(dte, "dddd, d mmmm yyyy"),
            "what":calEvent.getData('summary'),
            "where":calEvent.getData('location'),
            "desc":YAHOO.lang.isNull(calEvent.getData('description')) ? '' : calEvent.getData('description'),
            "fromdate":Alfresco.util.formatDate(dts, "dddd, d mmmm yyyy"),
            "start":calEvent.getData('dtstart').split('T')[1].substring(0,5),
            "todate":Alfresco.util.formatDate(dte, "dddd, d mmmm yyyy"),
            "end":calEvent.getData('dtend').split('T')[1].substring(0,5),
            'tags':calEvent.getData('category')
        };
        Alfresco.util.Ajax.request(
         {
            method: Alfresco.util.Ajax.PUT,
            url: Alfresco.constants.PROXY_URI + 'calendar/'+eventUri.split('/calendar/')[1]+'?page=calendar',
            dataObj : dataObj,
            requestContentType : "application/json",
            responseContentType : "application/json",
            successCallback:
            {
               fn: function(){
                 Alfresco.util.PopupManager.displayMessage(
                   {
                          text: Alfresco.util.message('message.edited.success',this.name)
                   });

               },
               scope: this
            },
            failureMessage: Alfresco.util.message('message.edited.failure','Alfresco.CalendarView')
         });
      },
      
      /**
       * Updates date field in dialog when date in selected in popup calendar
       * 
       * @method onDateSelected
       * @param e {object} Event object
       * @param args {object} Event argument object
       */
      onDateSelected : function(e,args,context) {
        if (this.currPopUpCalContext) {
          //ugly
          for (var i=1;i<args[0][0].length;i++)
          {
              args[0][0][i] = Alfresco.CalendarHelper.padZeros(args[0][0][i]); 
          }
          Dom.get(this.currPopUpCalContext).value = args[0][0].join('-');
          //add one hour as default
          if (this.currPopUpCalContext==='dtend')
          {
              Dom.get(this.currPopUpCalContext+'time').value = YAHOO.widget.DateMath.add(
                  Alfresco.util.fromISO8601( Dom.get('dtstart').value+'T'+Dom.get('dtstarttime').value ),
                  YAHOO.widget.DateMath.HOUR,
                  1).format(dateFormat.masks.isoTime);
              
          }
        }
      },
      /**
       * Handler for cancelling dialog
       *  
       * @method onCancelDialog
       * 
       */
      onCancelDialog : function() {
          this.eventDialog.hide();
      },

      /**
       * Handler for showing/hiding the 'show more' overlay
       *
       * @method onShowMore
       * @param e {object} DomEvent
       * @param args {array} event arguments
       * @param elTarget {} HTML element 
       */
      onShowMore : function(e,args,elTarget) {
        //already showing more so show less
        var cell = Dom.getAncestorByTagName(elTarget,'ul');

        if ( Dom.hasClass(elTarget,'active') )
        {
          //reshow original data
          Dom.removeClass(cell,'showing');
          Dom.removeClass(elTarget,'active');
          if ( this.hiddenItems )
          {
              for (var i=0,el;el=this.hiddenItems[i];i++) {
                  Dom.addClass(el,'hidden');
              }
          }
          elTarget.innerHTML=this._msg('label.show-more');
          this.hiddenItems = null;
        }
        else //show more
        {
          Dom.addClass(cell,'showing');

          Dom.addClass(elTarget,'active');
          this.hiddenItems = Dom.getElementsByClassName('hidden','li',cell);
          for (var i=0,el;el=this.hiddenItems[i];i++) {
              Dom.removeClass(el,'hidden');
          }
          elTarget.innerHTML=this._msg('label.show-less');
        }
        Event.preventDefault(e);
      },

      /**
       * Handler for when an event is resized
       * 
       * @method onEventResized
       * 
       * @param e {object} Event object
       * @param o {object} Event argument 
       */
      onEventResized : function(e,o){
          this.updateEvent(o[1]);
      },
      /**
       * Render multiple events correctly
       * 
       * @method renderMultipleEvents
       * @param parNode {object} HTML element in which to render events
       *  
       */
      renderMultipleEvents : function(parNode)
      {
        var existingEvents = YAHOO.util.Dom.getElementsByClassName('vevent','div',parNode);
        var numExistingEvents = existingEvents.length;
        if (numExistingEvents===0)
        {
          return;
        }
        var parRegion = YAHOO.util.Dom.getRegion(parNode);
        var newWidth= (parRegion.right-parRegion.left)/numExistingEvents;
        if (numExistingEvents>1)
        {
          for (var i=0 ; i<numExistingEvents; i++){
            var el = existingEvents[i];
            YAHOO.util.Dom.setStyle(el,'width',newWidth+'px');
            YAHOO.util.Dom.setStyle(el,'left',newWidth*(i)+'px');
          }          
        }
        else 
        {
          var el = existingEvents[0];
          YAHOO.util.Dom.setStyle(el,'width',newWidth+'px');
          YAHOO.util.Dom.setStyle(el,'left',0);
        }
      },
      
      /**
       * 
       * @method onFormValidationError
       *  
       * @param e {object} Event object
       * @param args {object} Value object referencing elements that are invalid
       */
      onFormValidationError : function(e,args)
      {
        var args = args[1];
        YAHOO.util.Dom.addClass(args.field,'error');
      },
      
      /**
       * Shows/hides the early hours of day (midnight till 7am)
       * 
       * @method toggleEarlyTableRows
       *  
       */
      toggleEarlyTableRows : function() {

        var triggerEl = YAHOO.util.Dom.get('collapseTrigger');
        this.earlyEls = YAHOO.util.Dom.getElementsByClassName('early','tr',triggerEl.parentNode);
        var displayStyle = (YAHOO.env.ua.ie) ? 'block' : 'table-row';
        for (var i=0;i<this.earlyEls.length;i++)
        {
          var el = this.earlyEls[i];
         YAHOO.util.Dom.setStyle(el,'display',(this.isShowingEarlyRows) ? 'none' : displayStyle);
        }
        this.isShowingEarlyRows = !this.isShowingEarlyRows;
      },
      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function DL__msg(messageId)
      {
         // return messageId;
         return Alfresco.util.message.call(this, messageId, this.name, Array.prototype.slice.call(arguments).slice(1));
      }
   };
   Alfresco.CalendarView.VIEWTYPE_WEEK = 'week';
   Alfresco.CalendarView.VIEWTYPE_MONTH = 'month';
   Alfresco.CalendarView.VIEWTYPE_DAY = 'day';
   Alfresco.CalendarView.VIEWTYPE_AGENDA = 'agenda'; 
})();


/**
 * Alfresco.CalendarEvent
 * Represents an calendar event
 * 
 * @constructor
 * @subclass YAHOO.util.DD
 * 
 * @param id {String} Id of event element 
 * @param sGroup {String} Name of draggable group
 * @param config {object} Configuration object
 * 
 */
Alfresco.calendarEvent = function(id, sGroup, config) {
    if (config.draggable)
    {
      Alfresco.calendarEvent.superclass.constructor.apply(this, arguments);
      this.initDDBehaviour(id, sGroup, config);      
    }
    this.initEventData(id, (YAHOO.lang.isUndefined(config.performRender)) ? true : config.performRender ) ;
    this.initEvents();
    YAHOO.util.DDM.mode = YAHOO.util.DDM.INTERSECT; 
    
    if (config.resizable===true)
    {
      this.resize = new YAHOO.util.Resize(this.getEl(),{
          handles:['b'],
          hover:true,
          xTicks:config.resize.xTicks // half height of div.hourSegment
      });
      /**
       * Over large resize actions (or after multiple resizes), the bottom edge does quite line up correctly with the hour segments.
       * The handler divides the height of the resized element by the xTick value. This gives the number of
       * 'ticked' positions there are in the current height. This value is then divided by 2 (2 div.hourSegment per hour)
       * and finally by 5. This last number is added to the height which lines up correctly.
       * 
       * numOfTickPos = height/xTick
       * numOfHourSegments = numOfTickPos/2
       * delta = numOfHourSegments/5
       * (dividing by 5 seems to work best here)
       * (delta = numOfTickPos/10 uses one less division)
       */
      this.resize.on('resize',this.onResize,this,true);
      this.resize.on('endResize',function endResize(args){
      this.onResize(args);
      YAHOO.Bubbling.fire('eventResized',this);
     },this,true);
   }
};

YAHOO.extend(Alfresco.calendarEvent, YAHOO.util.DD, {
    /**
     * Initialises custom events
     * 
     * @method initEvents 
     */
    initEvents : function() {
        this.createEvent('eventMoved');
    },
    /**
     * Initialises event data by parsing the element's microformat information
     * 
     * @method initEventData
     * @param id {string} Id of element to use as source of event data
     * @param performRender {Boolean} Flag denoting whether to render data after parsing 
     */
    initEventData: function (id,performRender) {
        this.eventData = new microformatParser(
                      {
                          ufSpec : hcalendar,
                          srcNode : this.getEl()
                      });
        this.eventData.parse();
        if (performRender===true)
        {
            this.eventData.render();
        }
    },
    
    /**
     * Initialise drag and drop behaviour
     * 
     * @method initDDBehaviour
     *  
     * @param id {String} Id of event element 
     * @param sGroup {String} Name of draggable group
     * @param config {object} Configuration object 
     */
    initDDBehaviour: function(id, sGroup, config) {
      if (!id) { 
          return; 
      }

      if (!(YAHOO.util.Dom.hasClass(this.getEl(),'allday')))
      {
          YAHOO.util.DDM.mode = YAHOO.util.DDM.INTERSECT; 
          var el = this.getDragEl();

          // specify that this is not currently a drop target
          this.isTarget = false;
      
      }
      this.initConstraints(YAHOO.util.Dom.getAncestorByTagName(el,'tbody'));
      if (this.config.yTick!==null)
      {
          this.setYTicks(0,this.config.yTick);            
      }
      if (this.config.xTick!==null)
      {
          this.setXTicks(0,this.config.xTick);
      }
    },
    
    /**
     * handler for startDrag event
     * 
     * @method startDrag
     * 
     */
    startDrag: function(x, y) 
    {
      YAHOO.util.Dom.setStyle(this.getEl(),'z-index','99');
    },

    /**
     * Handler for endDrag event
     * 
     * @method endDrag 
     */
    endDrag: function(e) {
      YAHOO.util.Dom.setStyle(this.getEl(),'z-index','1');
    },

    /**
     * Handler for dragDrop event
     * 
     * @method onDragDrop
     */
    onDragDrop: function(e, id) 
    {
       // get the drag and drop object that was targeted
      var oDD;
      if ("string" == typeof id) 
      {
          oDD = YAHOO.util.DDM.getDDById(id);
      }
      else 
      {
          oDD = YAHOO.util.DDM.getBestMatch(id);
      }
      //elem that dragged el was dropped on
      var targetEl = oDD.getEl(); 
      var el = this.getEl();
      var currTd;
      //allda
      
      if ( (YAHOO.util.Dom.hasClass(targetEl,'day')) )
      {
          currTd = targetEl;
          if (YAHOO.util.Dom.hasClass(el,'allday'))
          {
              targetEl.appendChild(el);
              //force a reparse as dom refs get out of sync
              this.eventData.parse(el.parentNode);
          }
          else {
            var ul = targetEl.getElementsByTagName('ul');
            var elUl = null;
            var dayHasExistingEvents = false;
            //day has no event so add ul
            if (ul.length === 0) {

                elUl = document.createElement('ul');
                elUl.className=el.parentNode.className;
                elUl = targetEl.appendChild(elUl);
            }
            // just add to existing ul
            //TODO sort ul by time
            else {
                dayHasExistingEvents = true;
                elUl = ul[0];
            }
            //if dragged onto different day
            if (elUl!==el.parentNode)
            {
              //make sure source UL shows all available events eg unhide (hidden)
              var dayEventsHidden = YAHOO.util.Dom.getElementsByClassName('hidden','li',el.parentNode);
              if (dayEventsHidden.length>0) 
              {
                  YAHOO.util.Dom.removeClass(dayEventsHidden[0],'hidden');
              }
              //must sort and not insert after showmore
              if (dayHasExistingEvents)
              {
                var dayEvents = elUl.getElementsByTagName('li');

                if (dayEvents.length>=4)
                {
                    if (!YAHOO.util.Dom.hasClass(YAHOO.util.Dom.getAncestorByTagName(elUl,'td'),'showing'))
                    {
                        YAHOO.util.Dom.addClass(el,'hidden');
                    }
                    if (YAHOO.util.Dom.getElementsByClassName('moreEvents',null,'li'))
                    {
                        elUl.insertBefore(el,dayEvents[dayEvents.length-1]);
                    }
                }
                else {
                    elUl.appendChild(el);
                }
              }
              else {
                  elUl.appendChild(el);
              }
              
              //force a reparse as dom refs get out of sync
              this.eventData.parse(el.parentNode);
            }
            YAHOO.util.Dom.setStyle(el,'position','static');
          }
          
      }
      if ( (YAHOO.util.Dom.hasClass(targetEl,'hourSegment')) )
      {
        currTd = YAHOO.util.Dom.getAncestorByTagName(el.parentNode,'td');
        targetEl = this.targetEl;
        if (targetEl)
        {
            var delta  =  YAHOO.util.Dom.getY(el)-YAHOO.util.Dom.getY(targetEl);
            //move el
            targetEl.appendChild(el);
            //reset to 0,0 origin
            YAHOO.util.DDM.moveToEl(el,targetEl);
            // if not dragged to top left pos move to delta
            if (parseInt(delta,10)>1)
            {
                YAHOO.util.Dom.setStyle(el,'top',delta+'px');
            }
        }
        this.targetEl = targetEl;
      }
      this.fireEvent('eventMoved',{targetEl:this.targetEl,dropped:true,previousTargetEl:currTd});
    },
    swap: function(el1, el2) 
    {
        var Dom = YAHOO.util.Dom;
        var pos1 = Dom.getXY(el1);
        var pos2 = Dom.getXY(el2);
        Dom.setXY(el1, pos2);
        Dom.setXY(el2, pos1);
    },

    /**
     * Handler for dragOver method
     * 
     * @method onDragOver 
     */
    onDragOver: function(e, id) 
    {
       if ("string" == typeof id) 
       {
        oDD = YAHOO.util.DDM.getDDById(id);
       }
       else 
       {
        oDD = YAHOO.util.DDM.getBestMatch(id);
       }
       //elem that dragged el was dropped on
       var targetEl = this.getBestMatch(id);
       if (targetEl)
       {
         //week and day view
         if ( (YAHOO.util.Dom.hasClass(targetEl,'hourSegment')) )
         {
             var el = this.getEl();
             //resize according to target's width and x coord
             YAHOO.util.Dom.setX(el,Math.max(0,parseInt(YAHOO.util.Dom.getX(targetEl),10)));

         }
         this.targetEl = targetEl;
         this.fireEvent('eventMoved',{targetEl:this.targetEl,dropped:false});
       }
     },
    
    /**
     * Setup co-ordinates to constrain dragging behaviour. Contrains dragging 
     * to tbody element except for first two rows if in Day or Week view
     * 
     * @method initConstraints
     * @param constraintEl {object} Element to which to constrain dragging behaviour 
     */
    initConstraints : function(constraintEl) {
      var Dom = YAHOO.util.Dom;
      if (constraintEl)
      {
        //Get the top, right, bottom and left positions
        var region = Dom.getRegion(constraintEl);
        //Get the element we are working on
        var el = this.getEl();

        //Get the xy position of it
        var xy = Dom.getXY(el);

        //Get the width and height
        var width = parseInt(Dom.getStyle(el, 'width'), 10);
        var height = parseInt(Dom.getStyle(el, 'height'), 10);
        //must not include allday and toggle rows
        if (this.config.view===Alfresco.CalendarView.VIEWTYPE_DAY | this.config.view===Alfresco.CalendarView.VIEWTYPE_WEEK)
        {
          var trRows= constraintEl.getElementsByTagName('tr');
          var alldayRegion = Dom.getRegion(trRows[0]);
          var toggleRegion = Dom.getRegion(trRows[1]);
          region.top+= (alldayRegion.bottom-alldayRegion.top) + (toggleRegion.bottom-toggleRegion.top);
        }
        //Set the constraints based on the above calculations
        this.setXConstraint(xy[0] - region.left,region.right - xy[0] - width);
        this.setYConstraint(xy[1] - region.top, region.bottom - xy[1] - height);
      }
    },
    
    /**
     * updates event data (and DOM) using microformat structure
     * 
     * @method update
     * @param vevent {object} Value object containing event data
     */ 
    update : function(vevent) {
      this.eventData.update(vevent,true);
    },
    
    /**
     * Gets the correct target based on top left position and area
     * 
     * @method getBestMatch
     * @param els {Array} Array of ids of elements to test
     * @return targetEl {object} Best matching HTML element
     */
    getBestMatch : function(els) 
    {
        var range = 2;
        var area = 0;
        var targetEl = null;
        var top = YAHOO.util.Dom.getRegion(this.getEl()).top;
        for (var item in els)
        {
          var el = els[item];
          var overlap = el.overlap;

          if (overlap) 
          { 
            if ((overlap.top - top)<range)
            {
              if (overlap.getArea() > area) {
                   targetEl = el._domRef;
                   area = overlap.getArea();
              }
            }
          }
        }
        return targetEl;
    },
    
    /**
     * Returns specified event data. If no fieldName is passed then returns a dump
     * of all event data
     * 
     * @method getData
     * @param fieldName {String} Name of event to retrieve
     * @param parsedValue {Boolean} Flag to denote whether to return data as parsed or not 
     * 
     * @return {object} field value 
     */
    getData : function(fieldName,parsedValue)
    {
        if (fieldName)
        {
            return this.eventData.get(fieldName,parsedValue);            
        }
        else {
            return this.eventData.getAll();
        }

    },
    
    /**
     * Change specified field of event data
     * 
     * @method setData
     * @param fieldName {String} Name of field to change 
     * @param value {object} value of field to change to
     */
    setData : function(fieldName,value)
    {
        if (!YAHOO.lang.isUndefined(this.eventData[fieldName]))
        {
            this.eventData[fieldName] = value;
        }
    },
    
    /**
     * Deletes event from DOM
     * 
     * @method  deleteEvent
     *  
     */
    deleteEvent : function() {
        this.getEl().parentNode.removeChild(this.getEl());
    },
    
    /**
     * Handler for onResize method
     * 
     * @method onResize
     * @param args {object} event argument object
     */    
    onResize : function(args){
       var xTick = args.target.get('xTicks');
       this.delta = Math.ceil((args.height/xTick)/10);
       YAHOO.util.Dom.setStyle(args.target.getWrapEl(),'height',args.height+this.delta+'px');
       var hours = args.height/args.target.get('xTicks')/4;
       var mins = hours*60;
       var duration = "PT"+parseInt(hours,10)+'H'+mins%60+'M';
       var endDate = Alfresco.CalendarHelper.getEndDate(this.getData('dtstart'),hcalendar.parsers['duration'](duration));
       this.update({
          dtend : endDate,
          duration:duration
       });
    },
    /**
     * Shows event
     *
     */
    show : function()
    {
      YAHOO.util.Dom.setStyle(this.getEl(),'display','');
    },
    /**
     * Hides event
     *
     */
    hide : function()
    {
      YAHOO.util.Dom.setStyle(this.getEl(),'display','none');
    }
});

/**
 * Alfresco.CalendarHelper. Helper object consisting of useful helper methods
 * 
 * @constructor 
 */
Alfresco.CalendarHelper = ( function() {
    var Dom = YAHOO.util.Dom;
    var templates = [];
    return {
        /**
         * Calculates end date depending on specified duration, in ISO8601 format
         * 
         * @method getEndDate
         * @param dateISO {String} startDate in ISO8601 format
         * @param duration {object} Duration object
         */
        getEndDate : function(dateISO,duration) {
          var newDate = Alfresco.util.fromISO8601(dateISO);
          for (var item in duration) {
              newDate = YAHOO.widget.DateMath.add(newDate,(item==='M') ? YAHOO.widget.DateMath.MINUTE : item ,parseInt(duration[item],10));
          }
          return Alfresco.util.toISO8601(newDate).split('+')[0];//newDate.toISO8601String(5);
        },
        
        /**
         * Correctly determines which hour segment the event element is in. Returns the hour
         * 
         * @method determineHourSegment
         * @param ePos {object} Object containing XY position of element to test
         * @param el {object} Event element
         * @return {string} Hour 
         */
        determineHourSegment : function(ePos,el) {
          var r = Dom.getRegion(el);
          var y = ePos[1];
          var threshold = (r.bottom - r.top)/2;
          var inFirstHalfHour = (!Dom.getPreviousSibling(el)); // first half of hour

          var hour = Dom.getAncestorByTagName(el,'tr').getElementsByTagName('h2')[0].innerHTML;
          if (inFirstHalfHour===true) 
          {
              hour = ( y-r.top < threshold) ? hour : hour.replace(':00',':15');

          }
          else {
              hour = ( y-r.top < threshold) ? hour.replace(':00',':30') : hour.replace(':00',':45');

          }
          return hour;
      },
      
      /**
       * calculates duration based on specified start and end dates
       * 
       * 
       * @method getDuration 
       * @param dtStartDate {Date} start date
       * @param dtEndDate {Date} end date
       * @return {String} Duration in ical format eg PT2H15M
       */
      getDuration : function(dtStartDate,dtEndDate){
          var diff = dtEndDate.getTime() - dtStartDate.getTime() ;
          var dateDiff = {};
          var duration = 'P';
          var diff = new Date();
          diff.setTime(Math.abs(dtStartDate.getTime() - dtEndDate.getTime()));

          var timediff = diff.getTime();

          dateDiff[YAHOO.widget.DateMath.WEEK] = Math.floor(timediff / (1000 * 60 * 60 * 24 * 7));
          timediff -= dateDiff[YAHOO.widget.DateMath.WEEK] * (1000 * 60 * 60 * 24 * 7);

          dateDiff[YAHOO.widget.DateMath.DAY] = Math.floor(timediff / (1000 * 60 * 60 * 24)); 
          timediff -= dateDiff[YAHOO.widget.DateMath.DAY] * (1000 * 60 * 60 * 24);

          dateDiff[YAHOO.widget.DateMath.HOUR] = Math.floor(timediff / (1000 * 60 * 60)); 
          timediff -= dateDiff[YAHOO.widget.DateMath.HOUR] * (1000 * 60 * 60);

          dateDiff[YAHOO.widget.DateMath.MINUTE] = Math.floor(timediff / (1000 * 60)); 
          timediff -= dateDiff[YAHOO.widget.DateMath.MINUTE] * (1000 * 60);

          dateDiff[YAHOO.widget.DateMath.SECOND] = Math.floor(timediff / 1000); 
          timediff -= dateDiff[YAHOO.widget.DateMath.SECOND] * 1000;

          if (dateDiff[YAHOO.widget.DateMath.WEEK]>0){
              duration+=dateDiff[YAHOO.widget.DateMath.WEEK]+YAHOO.widget.DateMath.WEEK;
          }
          if (dateDiff[YAHOO.widget.DateMath.DAY]>0){
              duration+=dateDiff[YAHOO.widget.DateMath.DAY]+YAHOO.widget.DateMath.DAY;
          }
          duration+='T';
          if (dateDiff[YAHOO.widget.DateMath.HOUR]>0){
              duration+=dateDiff[YAHOO.widget.DateMath.HOUR]+YAHOO.widget.DateMath.HOUR;
          }
          if (dateDiff[YAHOO.widget.DateMath.MINUTE]>0){
              duration+=dateDiff[YAHOO.widget.DateMath.MINUTE]+'M';
          }
          if (dateDiff[YAHOO.widget.DateMath.SECOND]>0){
              duration+=dateDiff[YAHOO.widget.DateMath.SECOND]+YAHOO.widget.DateMath.SECOND;
          }
          return duration;
      },

      /**
       * Pads specified value with zeros if value is less than 10
       * 
       * @method padZeros 
       * 
       * @param value {Object} value to pad
       * @return {String} padded value
       */
      padZeros : function(value) 
      {
          return (value<10) ? '0'+value : value;
      },
      
      /**
       * Add an template using specified name as a reference
       */
      addTemplate : function(name,template){
          templates[name] = template;
      },
      
      /**
       * Retreives specified template
       * 
       * @method getTemplate
       * @param name {string} Name of template to retrieve
       * @return {string} template
       */
      getTemplate : function(name) {
          return templates[name];
      },
      /**
       * renders template as a DOM HTML element. Element is *not* added to document
       * 
       * @param name Name of template to render
       * @param data Data to render template against
       * @return HTMLElement Newly created div
       */
      renderTemplate : function(name,data) {
        
          var el = document.createElement('div');
          if (templates[name] && el)
          {
              var el = YAHOO.lang.isString(el) ? Dom.get(el) : el;
              var template = templates[name];
              var div = document.createElement('div');
              if (data)
              {
                  template = YAHOO.lang.substitute(template,data);
              }

              div.innerHTML = template;
              el.appendChild(div.firstChild);
                                    
              return el.lastChild;
          }
      },
      
      /**
       * Checks whether start date is earlier than end date.
       * 
       * @method isValidDate
       * @param {Date} dtStartDate Start date
       * @param {Date} dtEndDate End date
       * 
       * @return {Boolean} flag denoting whether date is valid or not.
       */
      isValidDate : function(dtStartDate,dtEndDate) {
          return dtStartDate.getTime() < dtEndDate.getTime();
      }
  };
} ) (); 

Alfresco.CalendarHelper.addTemplate('vevent',
    '<{el} class="vevent {allday} {hidden}"> ' +
	'<{contEl}>' +
		'<p class="dates">' +
		'<span class="dtstart" title="{from}">{start}</span> - ' +
	 	'<span class="dtend" title="{to}">{end}</span>' +
	 	'</p>' +
	  	'<p class="description">{desc}</p>' +
	  	'<a class="summary" href="{uri}">{name}</a>'+
        '<span class="location">{where}</span>' +
		'<span class="duration" title="{duration}">{duration}</span>'+
		'<span class="category">{tags}</span>'+
	'</{contEl}>' +
'</{el}>');
Alfresco.CalendarHelper.addTemplate('agendaDay','<h2>{date}</h2>');
  
Alfresco.CalendarHelper.addTemplate('agendaDayItem',
  '<li class="vevent"><span>{start} - {end}</span>'+
  '<a href="{uri}" class="summary">{name}</a></li>');

Alfresco.CalendarHelper.addTemplate('createEventButton','<button id="addEventButton">add</button>');
Alfresco.CalendarHelper.addTemplate('taggedTitle',"<span class=\"tagged\">{taggedWith} <span>'{tag}'</span></span>");

/**
 * Alfresco.util.DialogManager. Helper object to manage dialogs.
 * 
 * @constructor 
 */
Alfresco.util.DialogManager = ( function () {
        var dialogs = [];
        var dialogConfig = 
        { 
           width: "42em",
           displayDate : new Date(),
           doBeforeDialogShow :
           {
                fn : function (form)
                {
                    var Dom = YAHOO.util.Dom;
                    var today = new Date();
                    // Pretty formatting
                    var dateStr = Alfresco.util.formatDate(today, "dddd, d mmmm yyyy");
                    Dom.get("fd").value = dateStr;
                    Dom.get("td").value = dateStr;
                    Dom.get(this.id+"-from").value = Dom.get("fd").value;
                    Dom.get(this.id+"-to").value = Dom.get("td").value;
                    this.tagLibrary.initialize();
                    form.errorContainer=null;
                },
               scope: Alfresco.util.ComponentManager.findFirst('Alfresco.CalendarView')
           },
           doSetupFormsValidation:
           {
              fn: function (form)
              {
                   var Dom = YAHOO.util.Dom;
                   form.addValidation(this.id + "-title", Alfresco.forms.validation.mandatory, null, "blur");
                   form.addValidation(this.id + "-title", Alfresco.forms.validation.nodeName, null, "keyup");
                   form.addValidation(this.id + "-tag-input-field", Alfresco.module.event.validation.tags, null, "keyup");

                   var dateElements = ["td", "fd", this.id + "-start", this.id + "-end"];
                   for (var i=0; i < dateElements.length; i++)
                   {
                      form.addValidation(dateElements[i],this.options._onDateValidation, { "obj": this }, "blur");
                   }

                   // Setup date validation
                   form.addValidation("td", this.options._onDateValidation, { "obj": this }, "focus");
                   form.addValidation("fd", this.options._onDateValidation, { "obj": this }, "focus");
                   form.addValidation(this.id + "-start", this.options._onDateValidation, { "obj": this }, "blur");
                   form.addValidation(this.id + "-end", this.options._onDateValidation, { "obj": this }, "blur");

                   // form.setShowSubmitStateDynamically(true);
                   form.setSubmitElements(this.okButton);
                   /**
                     * Button declarations that, when clicked, display
                     * the calendar date picker widget.
                     */
                    var startButton = new YAHOO.widget.Button(
                    {
                        type: "push",
                        id: "calendarpicker",
                        container: this.id + "-startdate"
                    });

                    startButton.on("click", this.options.onDateSelectButton);

                    var endButton = new YAHOO.widget.Button(
                    {
                       type: "push",
                       id: "calendarendpicker",
                       container: this.id + "-enddate"
                    });
                    endButton.on("click", this.options.onDateSelectButton);
                    YAHOO.Bubbling.on('formValidationError',Alfresco.util.ComponentManager.findFirst('Alfresco.CalendarView').onFormValidationError,this);
              },
               scope: Alfresco.util.ComponentManager.findFirst('Alfresco.CalendarView')
           },
          /**
           * Event handler that gets fired when a user clicks on the date selection
           * button in the event creation form. Displays a mini YUI calendar.
           * Gets called for both the start and end date buttons.
           *
           * @method onDateSelectButton
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
             var me = this;

             oCalendar.selectEvent.subscribe(function (type, args) {
                var date;
                var Dom = YAHOO.util.Dom;

                if (args) {
                   var prettyId, hiddenId;
                   if (container.indexOf("enddate") > -1)
                   {
                      prettyId = "td";
                   }
                   else
                   {
                      prettyId = "fd";
                   }

                   date = args[0][0];
                   var selectedDate = new Date(date[0], (date[1]-1), date[2]);

                   var elem = Dom.get(prettyId);
                   elem.value = Alfresco.util.formatDate(selectedDate, "dddd, d mmmm yyyy");
                   elem.focus();
                   document.getElementsByName('from')[0].value = elem.value;                   

                   if(prettyId == "fd")
                   {
                      // If a new fromDate was selected
                      var toDate = new Date(Alfresco.util.formatDate(Dom.get("td").value, "yyyy/mm/dd"));
                      if(YAHOO.widget.DateMath.before(toDate, selectedDate))
                      {                     
                         //...adjust the toDate if toDate is earlier than the new fromDate
                         var tdEl = Dom.get("td");
                         tdEl.value = Alfresco.util.formatDate(selectedDate, "dddd, d mmmm yyyy");
                         document.getElementsByName('to')[0].value = tdEl.value;
                      }
                   }
                }
                oCalendarMenu.hide();
             });
         },
           _onDateValidation: function(field, args, event, form, silent)
             {
                var Dom = YAHOO.util.Dom;
                // Check that the end date is after the start date
                var start = Alfresco.util.formatDate(Dom.get("fd").value, "yyyy/mm/dd");
                var startDate = new Date(start + " " + Dom.get(args.obj.id + "-start").value);

                var to = Alfresco.util.formatDate(Dom.get("td").value, "yyyy/mm/dd");
                var toDate = new Date(to + " " + Dom.get(args.obj.id + "-end").value);
                
                //allday events; the date and time can be exactly the same so test for this too
                if (startDate.getTime()===toDate.getTime())
                {
                  return true;
                }
                var after = YAHOO.widget.DateMath.after(toDate, startDate);

                if (Alfresco.logger.isDebugEnabled())
                {
                   Alfresco.logger.debug("Current start date: " + start + " " + Dom.get(args.obj.id + "-start").value);
                   Alfresco.logger.debug("Current end date: " + to + " " + Dom.get(args.obj.id + "-end").value);
                   Alfresco.logger.debug("End date is after start date: " + after);
                }

                if (!after && !silent)
                {
                   form.addError(form.getFieldLabel(field.id) + " cannot be before the start date.", field);
                }
                return after;
             }
        };
        return {
            registerDialog : function(dialogName)
            {

                var dialog = new Alfresco.module.SimpleDialog();
                dialog.setOptions(dialogConfig);
                dialogs[dialogName] = dialog;
                return dialogs[dialogName];
            },
            getDialog : function(dialogName)
            {
                return dialogs[dialogName] || null;
            }
        };
    } ) ();   
                
Alfresco.util.DialogManager.registerDialog('CalendarView.addEvent');




