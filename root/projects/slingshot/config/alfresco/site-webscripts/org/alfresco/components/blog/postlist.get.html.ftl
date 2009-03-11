<script type="text/javascript">//<![CDATA[
   new Alfresco.BlogPostList("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!''}",
      containerId: "${args.container!'blog'}",
      initialFilter:
      {
         filterId: "${page.url.args.filterId!'new'}",
         filterOwner: "${page.url.args.filterOwner!'Alfresco.BlogPostListFilter'}",
         filterData: <#if page.url.args.filterData??>"${page.url.args.filterData}"<#else>null</#if>
      }
   }).setMessages(
      ${messages}
   );
//]]></script>

<div class="postlist-header">
   <div class="postlist-infobar yui-gb theme-bg-color-4">
      <div class="yui-u first">
         <div id="${args.htmlid}-listtitle" class="listTitle">
            ${msg("title.postlist")}
         </div>
      </div>
      <div class="yui-u">
         <div id="${args.htmlid}-paginator" class="paginator"></div>
      </div>
      <div class="yui-u flat-button">
         <div class="simple-view"><button id="${args.htmlid}-simpleView-button" name="postlist-simpleView-button">${msg("header.simpleList")}</button></div>
      </div>
   </div>
</div>

<div id="${args.htmlid}-postlist" class="blog-postlist">
</div>
