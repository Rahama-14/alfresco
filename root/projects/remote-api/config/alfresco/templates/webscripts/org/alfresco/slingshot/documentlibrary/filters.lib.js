var Filters =
{
   /**
    * Type map to filter required types.
    * NOTE: "documents" filter also returns folders to show UI hint about hidden folders.
    */
   TYPE_MAP:
   {
      "documents": '+(TYPE:"{http://www.alfresco.org/model/content/1.0}content" OR TYPE:"{http://www.alfresco.org/model/application/1.0}filelink" OR TYPE:"{http://www.alfresco.org/model/content/1.0}folder")',
      "folders": '+(TYPE:"{http://www.alfresco.org/model/content/1.0}folder" OR TYPE:"{http://www.alfresco.org/model/application/1.0}folderlink")',
      "images": "-TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\" +@cm\\:content.mimetype:image/*"
   },

   /**
    * Encode a path with ISO9075 encoding
    *
    * @method iso9075EncodePath
    * @param path {string} Path to be encoded
    * @return {string} Encoded path
    */
   iso9075EncodePath: function Filter_iso9075EncodePath(path)
   {
      var parts = path.split("/");
      for (var i = 1, ii = parts.length; i < ii; i++)
      {
         parts[i] = "cm:" + search.ISO9075Encode(parts[i]);
      }
      return parts.join("/");
   },

   /**
    * Create filter parameters based on input parameters
    *
    * @method getFilterParams
    * @param filter {string} Required filter
    * @param parsedArgs {object} Parsed arguments object literal
    * @param optional {object} Optional arguments depending on filter type
    * @return {object} Object literal containing parameters to be used in Lucene search
    */
   getFilterParams: function Filter_getFilterParams(filter, parsedArgs, optional)
   {
      var filterParams =
      {
         query: "+PATH:\"" + parsedArgs.pathNode.qnamePath + "/*\"",
         limitResults: null,
         sort: [
         {
            column: "@{http://www.alfresco.org/model/content/1.0}name",
            ascending: true
         }],
         language: "lucene",
         templates: null,
         variablePath: true
      };

      optional = optional || {};

      // Max returned results specified?
      var argMax = args.max;
      if ((argMax !== null) && !isNaN(argMax))
      {
         filterParams.limitResults = argMax;
      }

      var favourites = optional.favourites;
      if (typeof favourites == "undefined")
      {
         favourites = [];
      }

      // Create query based on passed-in arguments
      var filterData = String(args.filterData),
         filterQuery = "";

      // Common types and aspects to filter from the UI
      var filterQueryDefaults =
         " -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\"" +
         " -TYPE:\"{http://www.alfresco.org/model/content/1.0}systemfolder\"" +
         " -TYPE:\"{http://www.alfresco.org/model/forum/1.0}forums\"" +
         " -TYPE:\"{http://www.alfresco.org/model/forum/1.0}forum\"" +
         " -TYPE:\"{http://www.alfresco.org/model/forum/1.0}topic\"" +
         " -TYPE:\"{http://www.alfresco.org/model/forum/1.0}post\"" +
         " -@cm\\:lockType:READ_ONLY_LOCK";

      switch (String(filter))
      {
         case "all":
            filterQuery = "+PATH:\"" + parsedArgs.rootNode.qnamePath + "//*\"";
            filterQuery += " -TYPE:\"{http://www.alfresco.org/model/content/1.0}folder\"";
            filterParams.query = filterQuery + filterQueryDefaults;
            break;

         case "recentlyAdded":
         case "recentlyModified":
         case "recentlyCreatedByMe":
         case "recentlyModifiedByMe":
            var onlySelf = (filter.indexOf("ByMe")) > 0 ? true : false,
               dateField = (filter.indexOf("Created") > 0) ? "created" : "modified",
               ownerField = (dateField == "created") ? "creator" : "modifier";

            // Default to 7 days - can be overridden using "days" argument
            var dayCount = 7,
               argDays = args.days;
            if ((argDays !== null) && !isNaN(argDays))
            {
               dayCount = argDays;
            }

            // Default limit to 50 documents - can be overridden using "max" argument
            if (filterParams.limitResults === null)
            {
               filterParams.limitResults = 50;
            }

            var date = new Date();
            var toQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();
            date.setDate(date.getDate() - dayCount);
            var fromQuery = date.getFullYear() + "\\-" + (date.getMonth() + 1) + "\\-" + date.getDate();

            filterQuery = "+PATH:\"" + parsedArgs.rootNode.qnamePath;
            if (parsedArgs.nodeRef == "alfresco://sites/home")
            {
               // Special case for "Sites home" pseudo-nodeRef
               filterQuery += "/*/cm:documentLibrary";
            }
            filterQuery += "//*\"";
            filterQuery += " +@cm\\:" + dateField + ":[" + fromQuery + "T00\\:00\\:00.000 TO " + toQuery + "T23\\:59\\:59.999]";
            if (onlySelf)
            {
               filterQuery += " +@cm\\:" + ownerField + ":" + person.properties.userName;
            }
            filterQuery += " -TYPE:\"{http://www.alfresco.org/model/content/1.0}folder\"";

            filterParams.sort = [
            {
               column: "@{http://www.alfresco.org/model/content/1.0}" + dateField,
               ascending: false
            }];
            filterParams.query = filterQuery + filterQueryDefaults;
            break;

         case "editingMe":
            filterQuery = "+PATH:\"" + parsedArgs.rootNode.qnamePath + "//*\"";
            filterQuery += " +ASPECT:\"{http://www.alfresco.org/model/content/1.0}workingcopy\"";
            filterQuery += " +@cm\\:workingCopyOwner:" + person.properties.userName;
            filterParams.query = filterQuery;
            break;

         case "editingOthers":
            filterQuery = "+PATH:\"" + parsedArgs.rootNode.qnamePath + "//*\"";
            filterQuery += " +ASPECT:\"{http://www.alfresco.org/model/content/1.0}workingcopy\"";
            filterQuery += " -@cm\\:workingCopyOwner:" + person.properties.userName;
            filterParams.query = filterQuery;
            break;

         case "favourites":
            var foundOne = false;

            for (var favourite in favourites)
            {
               if (foundOne)
               {
                  filterQuery += " OR ";
               }
               foundOne = true;
               filterQuery += "ID:\"" + favourite + "\"";
            }
            filterParams.query = filterQuery.length > 0 ? "+PATH:\"" + parsedArgs.rootNode.qnamePath + "//*\" +(" + filterQuery + ")" : "+ID:\"\"";
            break;

         case "node":
            filterParams.variablePath = false;
            filterParams.query = "+ID:\"" + parsedArgs.nodeRef + "\"";
            break;

         case "tag":
            // Remove any trailing "/" character
            if (filterData.charAt(filterData.length - 1) == "/")
            {
               filterData = filterData.slice(0, -1);
            }
            filterParams.query = "+PATH:\"" + parsedArgs.rootNode.qnamePath + "//*\" +PATH:\"/cm:taggable/cm:" + search.ISO9075Encode(filterData) + "/member\"";
            break;

         case "category":
            // Remove any trailing "/" character
            if (filterData.charAt(filterData.length - 1) == "/")
            {
               filterData = filterData.slice(0, -1);
            }
            filterParams.query = "+PATH:\"/cm:generalclassifiable" + Filters.iso9075EncodePath(filterData) + "/member\"";
            break;

         default: // "path"
            filterParams.variablePath = false;
            filterQuery = "+PATH:\"" + parsedArgs.pathNode.qnamePath + "/*\"";
            filterParams.query = filterQuery + filterQueryDefaults;
            break;
      }

      // Specialise by passed-in type
      if (filterParams.query !== "")
      {
         filterParams.query += " " + (Filters.TYPE_MAP[parsedArgs.type] || "");
      }

      return filterParams;
   }
};
