<#include "include/alfresco-template.ftl" />
<@templateHeader>
   <@link rel="stylesheet" type="text/css" href="${url.context}/templates/rules/rule-edit.css" />
   <@script type="text/javascript" src="${page.url.context}/templates/rules/rule-edit.js"></@script>
</@>

<@templateBody>
   <div id="alf-hd">
      <@region id="header" scope="global" protected=true />
      <@region id=doclibType + "title" scope="template" protected=true />
      <@region id=doclibType + "navigation" scope="template" protected=true />
   </div>
   <div id="bd">
      <@region id="rule-edit" scope="template" protected=true />
   </div>
</@>

<@templateFooter>
   <div id="alf-ft">
      <@region id="footer" scope="global" protected=true />
   </div>
</@>
