model.addedContent = [];
model.modifiedContent = [];

// read config - use default values if not found
var maxItems = 3,
   conf = new XML(config.script);

if (conf["max-items"] != null)
{
   maxItems = parseInt(conf["max-items"]);
}

var result = remote.call("/slingshot/profile/usercontents?user=" + stringUtils.urlEncode(page.url.templateArgs["userid"]) + "&maxResults=" + maxItems);
if (result.status == 200)
{
   // Create javascript objects from the server response
   var data = eval('(' + result + ')');
   
   ['created','modified'].forEach(function(type)
   {
      var store = (type === 'created') ? model.addedContent : model.modifiedContent,
         contents = data[type].items,
         dateType = type + 'On',
         content;
      
      for (var i = 0, len = contents.length; i < len; i++)
      {
         content = contents[i];
         if (store.length < maxItems)
         {
            // convert createdOn and modifiedOn fields to date
            if (content[dateType])
            {
               content[dateType] = new Date(content[dateType]);
            }
            store.push(content);
         }
      }
      
      model[type === 'created' ? "addedContent" : "modifiedContent"] = store;
   });
}

model.numAddedContent = model.addedContent.length;
model.numModifiedContent = model.modifiedContent.length;