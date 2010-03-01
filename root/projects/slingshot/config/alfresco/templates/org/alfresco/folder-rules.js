<import resource="classpath:/alfresco/templates/org/alfresco/documentlibrary.js">

function main()
{
   // Load folder info
   var connector = remote.connect("alfresco");
   model.folder = loadDisplayInfo(connector, page.url.args.nodeRef);

   // Load rules
   result = connector.get("/api/node/" + page.url.args.nodeRef.replace("://", "/") + "/ruleset");
   if (result.status == 200)
   {
      var ruleset = eval('(' + result + ')').data;
      if (!ruleset)
      {
         ruleset = {};
      }
      model.ruleset = ruleset;

      var linkedToNodeRef = ruleset.linkedToRuleSet;
      if (linkedToNodeRef)
      {
         linkedToNodeRef = linkedToNodeRef.substring("/api/node/".length);
         linkedToNodeRef = linkedToNodeRef.substring(0, linkedToNodeRef.indexOf("/ruleset"));
         var tokens = linkedToNodeRef.split("/");
         linkedToNodeRef = tokens[0] + "://" + tokens[1] + "/" + tokens[2];
         model.linkedToFolder = loadDisplayInfo(connector, linkedToNodeRef);         
      }
   }
}

function loadDisplayInfo(connector, nodeRef)
{
   var result = connector.get("/slingshot/doclib/node/" + nodeRef.replace("://", "/"));
   if (result.status == 200)
   {
      var location = eval('(' + result + ')').item.location;
      return {
         nodeRef: nodeRef,
         site: location.site, 
         name: location.file,
         path: location.path
      };
   }
   return null;
}

main();
