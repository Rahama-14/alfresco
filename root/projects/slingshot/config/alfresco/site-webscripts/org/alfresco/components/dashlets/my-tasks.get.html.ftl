<#macro doclibUrl doc>
   <#if ((doc.location.site?exists) && (doc.location.site != ""))>
   <a href="${url.context}/page/site/${doc.location.site}/documentlibrary?file=${doc.fileName?url}#path=${doc.location.path?url}">${doc.displayName?html}</a>
   <#else>
   ${doc.displayName?html}
   </#if>
</#macro>
<script type="text/javascript">//<![CDATA[
   new Alfresco.MyTasks("${args.htmlid}").setMessages(
      ${messages}
   );
//]]></script>

<div class="dashlet my-tasks">
   <div class="title">${msg("header")}</div>
   <div class="toolbar">
      <input id="${args.htmlid}-all" type="checkbox" name="all" value="${msg("filter.all")}" checked />
      <input id="${args.htmlid}-dueOn" type="button" name="dueOn" value="${msg("filter.today")}" />
      <select id="${args.htmlid}-dueOn-menu">
         <option value="today">${msg("filter.today")}</option>
         <option value="tomorrow">${msg("filter.tomorrow")}</option>
         <option value="this-week">${msg("filter.this-week")}</option>                
         <option value="next-week">${msg("filter.next-week")}</option>
         <option value="overdue">${msg("filter.overdue")}</option>
         <option value="no-due-date">${msg("filter.no-due-date")}</option>                
      </select>
   </div>
   <div id="${args.htmlid}-taskList" class="body scrollableList">
   </div>
</div>