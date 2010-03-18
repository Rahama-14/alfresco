<import resource="classpath:alfresco/site-webscripts/org/alfresco/utils/feed.utils.js">

/**
 * Main entry point for the webscript
 */
function main ()
{
   var uri = args.feedurl;
   if (!uri)
   {
      // Use the default
      var conf = new XML(config.script);
      uri = conf.feed[0].toString();
   }

   var connector = remote.connect("http");
   var re = /^http:\/\//;
   if (!re.test(uri))
   {
      uri = "http://" + uri;
   }
   model.uri = uri;
   model.limit = args.limit || 999;
   model.target = args.target || "_self";

   var feed = getRSSFeed(uri);
   if (feed.error)
   {
      model.title = msg.get("title.error." + feed.error);
      model.error = true;
      model.items = [];
   }
   else
   {
      model.title = feed.title;
      model.items = feed.items;
   }


   var userIsSiteManager = true;
   //Check whether we are within the context of a site
   if (page.url.templateArgs.site)
   {
	   //If we are, call the repository to see if the user is site manager or not
	   userIsSiteManager = false;
      var obj = context.properties["memberships"];
      if (!obj)
      {
   	   var json = remote.call("/api/sites/" + page.url.templateArgs.site + "/memberships/" + stringUtils.urlEncode(user.name));
   	   if (json.status == 200)
   	   {
   	      obj = eval('(' + json + ')');
   	   }
   	}
   	if (obj)
   	{
	      userIsSiteManager = (obj.role == "SiteManager");
   	}
   }
   model.userIsSiteManager = userIsSiteManager;
}

/**
 * Start webscript
 */
main();