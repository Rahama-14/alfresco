<script type="text/javascript">//<![CDATA[
   new Alfresco.DiscussionsTopicList("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!''}",
      containerId: "${args.container!'blog'}",
      initialFilter:
      {
         filterId: "${page.url.args.filterId!'new'}",
         filterOwner: "${page.url.args.filterOwner!'Alfresco.TopicListFilter'}",
         filterData: <#if page.url.args.filterData??>"${page.url.args.filterData}"<#else>null</#if>
      }
   }).setMessages(
      ${messages}
   );
//]]></script>

<div class="topiclist-header flat-button">
   <div id="${args.htmlid}-topiclistBar" class="yui-g toolbar">
      <div class="yui-u first">
         <div id="${args.htmlid}-create-topic-container" class="hidden">
            <div class="createTopic hideable"><button id="${args.htmlid}-createTopic-button" name="postlist-createTopic-button">${msg("header.createTopic")}</button></div>
            <div class="separator hideable">&nbsp;</div>
         </div>
         <div class="rss-feed hideable"><a id="${args.htmlid}-rssFeed-button" href="#">${msg("header.rssFeed")}</a></div>
      </div>
      <div class="yui-u align-right">
         <button id="${args.htmlid}-simpleView-button" name="topiclist-simpleView-button">${msg("header.simpleList")}</button>
      </div>
   </div>

   <div class="topiclist-infobar yui-gd">
      <div class="yui-u first">
         <div id="${args.htmlid}-listtitle" class="listTitle">
            ${msg("title.generic")}
         </div>
      </div>
      <div class="yui-u align-right">
         <div id="${args.htmlid}-paginator" class="paginator"></div>
      </div>
   </div>
</div>

<div id="${args.htmlid}-topiclist" class="topiclist">
</div>
