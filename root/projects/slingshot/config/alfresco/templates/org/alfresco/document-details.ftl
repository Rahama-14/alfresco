<#include "include/alfresco-template.ftl" />
<@templateHeader>
   <@link rel="stylesheet" type="text/css" href="${url.context}/components/blog/postlist.css" />
   <@link rel="stylesheet" type="text/css" href="${url.context}/components/blog/postview.css" />
   <@link rel="stylesheet" type="text/css" href="${url.context}/templates/document-details/document-details.css" />
   <@script type="text/javascript" src="${page.url.context}/components/blog/blogdiscussions-common.js"></@script>
   <@script type="text/javascript" src="${page.url.context}/components/blog/blog-common.js"></@script>
   <@script type="text/javascript" src="${url.context}/modules/documentlibrary/doclib-actions.js"></@script>
   <@script type="text/javascript" src="${page.url.context}/templates/document-details/document-details.js"></@script>
   <#if doclibType != ""><@script type="text/javascript" src="${page.url.context}/templates/document-details/${doclibType}document-details.js"></@script></#if>
   <@templateHtmlEditorAssets />
</@>

<@templateBody>
   <div id="alf-hd">
      <@region id="header" scope="global" protected=true />
      <@region id="title" scope="template" protected=true />
      <@region id="navigation" scope="template" protected=true />
   </div>
   <div id="bd">
      <@region id=doclibType + "path" scope="template" protected=true />

      <div class="yui-g">
         <div class="yui-g first">
            <@region id=doclibType + "web-preview" scope="template" protected=true />
         <#if doclibType?starts_with("dod5015")>
            <@region id=doclibType + "events" scope="template" protected=true />
         <#else>
            <div class="document-details-comments">
               <@region id=doclibType + "comments" scope="template" protected=true />
               <@region id=doclibType + "createcomment" scope="template" protected=true />
            </div>
         </#if>
         </div>
         <div class="yui-g"> 
            <div class="yui-u first">
               <@region id=doclibType + "document-metadata-header" scope="template" protected=true />
               <@region id=doclibType + "document-metadata" scope="template" protected=true />
               <@region id=doclibType + "document-info" scope="template" protected=true />
               <@region id=doclibType + "document-versions" scope="template" protected=true />
            </div>
            <div class="yui-u">
               <@region id=doclibType + "document-actions" scope="template" protected=true />
               <@region id=doclibType + "document-links" scope="template" protected=true />
            </div>
         </div>
      </div>
   </div>
   
   <script type="text/javascript">//<![CDATA[
   new ${jsType}().setOptions(
   {
      nodeRef: "${url.args.nodeRef}",
      siteId: "${page.url.templateArgs.site!""}"
   });
   //]]></script>

</@>

<@templateFooter>
   <div id="alf-ft">
      <@region id="footer" scope="global" protected=true />
   </div>
</@>
