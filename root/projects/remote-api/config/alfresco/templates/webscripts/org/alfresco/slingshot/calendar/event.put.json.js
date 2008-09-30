<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/calendar/lib/calendar.lib.js">
/**
 * Update event properties
 * @method PUT
 * @param uri {string} /calendar/event/{siteid}/{eventname}
 */
function getTemplateParams()
{
   // Grab the URI parameters
   var siteid = "" + url.templateArgs.siteid;
   var eventname = "" + url.templateArgs.eventname;

   if (siteid === null || siteid.length === 0)
   {
      return null;
   }

   if (eventname === null || eventname.length === 0)
   {
      return null;
   }

   return {
      "siteid": siteid,
      "eventname": eventname
   };
}

function main()
{
   var params = getTemplateParams();
   if (params === null)
   {
      return {
         "error": "Invalid parameters"
      };
   }

   // Get the site
   var site = siteService.getSite(params.siteid);
   if (site === null)
   {
      return {
         "error": "Could find not specified site"
      };
   }

   var eventsFolder = getCalendarContainer(site);
   if (eventsFolder === null)
   {
      return {
         "error": "Could not find specified calendar"
      };
   }

   var event = eventsFolder.childByNamePath(params.eventname);
   if (event === null)
   {
      return {
         "error": "Could not find specified event to update"
      };
   }

   var props = [
      "what",
      "desc",
      "where"
   ];

   var propsmap =
   {
      "what" : "ia:whatEvent",
      "desc" : "ia:descriptionEvent",
      "where" : "ia:whereEvent"
   };

   for (var i=0; i < props.length; i++)
   {
      var prop = props[i], value;
      if (!json.isNull(prop))
      {
         value = json.get(prop);
         event.properties[ propsmap[prop] ] = value;
      }
   }
   
   if (!json.isNull("tags"))
   {
      var tags = String(json.get("tags")); // space delimited string
      if (tags !== "") 
      {
         var tagsArray = tags.split(" ");
         event.tags = tagsArray;
      }
      else
      {
         event.tags = []; // reset
      }
   }
   
   try 
   {
      // Handle date formatting as a separate case      
      var from = json.get("from");
      var to = json.get("to");
     
      if (json.isNull("allday"))
      {
         from += " " + json.get("start");
         to += " " + json.get("end");
      }
      
      event.properties["ia:fromDate"] = new Date(from);
      event.properties["ia:toDate"] = new Date(to);

      var pad = function (value, length)
      {
         value = String(value);
         length = parseInt(length) || 2;
         while (value.length < length)
         {
            value = "0" + value;
         }
         return value;
      };

      var isoDate = from.getFullYear() + "-" + pad(from.getMonth() + 1) + "-" + pad(from.getDate());
      var data =
      {
         title: json.get("what"),
         page: json.get("page") + "?date=" + isoDate
      }
      activities.postActivity("org.alfresco.calendar.event-created", params.siteid, "calendar", jsonUtils.toJSONString(data));
   }
   catch(e)
   {
      if (logger.isLoggingEnabled())
      {
         logger.log(e);
      }
   }

   event.save();
   return {};
}

model.result = main();
