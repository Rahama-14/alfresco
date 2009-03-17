<#include "include/alfresco-template.ftl" />
<@templateHeader>
  <@link rel="stylesheet" type="text/css" href="${url.context}/templates/wiki/wiki.css" />
</@>

<@templateBody>
   <div id="alf-hd">
      <@region id="header" scope="global" protected=true />
      <@region id="title" scope="template" protected=true />
   </div>
   <div id="bd">
      <div class="yui-t1">
         <div id="yui-main">
            <@region id="search" scope="template" protected=true />
         </div>
      </div>
   </div>
</@>

<@templateFooter>
   <div id="alf-ft">
      <@region id="footer" scope="global" protected=true />
   </div>
</@>