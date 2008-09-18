
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
   <div id="${args.htmlid}-postlistBar" class="toolbar flat-button">
      <div id="${args.htmlid}-create-post-container" class="hidden">
         <div class="createPost hideable"><button id="${args.htmlid}-createPost-button" name="postlist-createPost-button">${msg("header.createPost")}</button></div>
         <div class="separator hideable">&nbsp;</div>
      </div>
      <div id="${args.htmlid}-configure-blog-container" class="hidden">
         <div class="configureBlog hideable"><button id="${args.htmlid}-configureBlog-button" name="postlist-configureBlog-button">${msg("header.configureBlog")}</button></div>
         <div class="separator hideable">&nbsp;</div>
      </div>
      <div class="rss-feed hideable"><button id="${args.htmlid}-rssFeed-button">${msg("header.rssFeed")}</button></div>
      <div class="simple-view"><button id="${args.htmlid}-simpleView-button" name="postlist-simpleView-button">${msg("header.simpleList")}</button></div>
   </div>


   <div class="postlist-infobar yui-gd">
      <div class="yui-u first">
         <div id="${args.htmlid}-listtitle" class="listTitle">
            ${msg("title.postlist")}
         </div>
      </div>
      <div class="yui-u align-right">
         <div id="${args.htmlid}-paginator" class="paginator"></div>
      </div>
   </div>
</div>

<div id="${args.htmlid}-postlist" class="blog-postlist">
</div>
