<import resource="classpath:alfresco/site-webscripts/org/alfresco/callutils.js">
<import resource="classpath:alfresco/site-webscripts/org/alfresco/components/calendar/helper.js">

// Check whether the current user is a member of the site first and then if they are
// the role of the user - until there is a method of doing this check on the web tier 
// we have to make a call back to the repo to get this information.

var role = null;
var json = remote.call("/api/sites/" + page.url.templateArgs.site + "/memberships/" + stringUtils.urlEncode(user.name));
if (json.status == 200)
{
   var obj = eval('(' + json + ')');
   role = obj.role;
}

// set role appropriately
if (role !== null)
{
   model.role = role;
}
else
{
   model.role = "Consumer"; // default to safe option
}
model.viewType = CalendarScriptHelper.getView();