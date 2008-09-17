function getNoOfColumns(template)
{
   var noOfColumns = 0;
   while(template.properties["gridColumn" + (noOfColumns + 1)] !== null)
   {
      noOfColumns++;
   }                                       
   return noOfColumns;
}


// Get current template
var dashboardId;
if(args.dashboardType == "user")
{                              
   dashboardId = "user/" + user.name + "/dashboard";
}
else if(args.dashboardType == "site")
{
   dashboardId = "site/" + page.url.templateArgs.site + "/dashboard";   
}
var currentTemplate = sitedata.findTemplate(dashboardId);
var currentNoOfColumns = getNoOfColumns(currentTemplate);
var currentLayout = {templateId: currentTemplate.id, noOfColumns: currentNoOfColumns, description: currentTemplate.description};

// Hardcoded templates until proper service exists
var layouts = [
   {templateId: "dashboard-1-column",             noOfColumns: 1, description: "<b>One</b> column"},
   {templateId: "dashboard-2-columns-wide-right", noOfColumns: 2, description: "<b>Two</b> columns: narrow left, wide right"},
   {templateId: "dashboard-2-columns-wide-left",  noOfColumns: 2, description: "<b>Two</b> columns: wide left, narrow right"},
   {templateId: "dashboard-3-columns",            noOfColumns: 3, description: "<b>Three</b> columns: narrow left, "},
   {templateId: "dashboard-4-columns",            noOfColumns: 4, description: "<b>Four</b> columns"}
];

// Prepare model for template
model.currentLayout = currentLayout;
model.layouts = layouts;