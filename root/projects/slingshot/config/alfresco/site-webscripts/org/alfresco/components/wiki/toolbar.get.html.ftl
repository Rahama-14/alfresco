<script type="text/javascript">//<![CDATA[
   new Alfresco.WikiToolbar("${args.htmlid}").setSiteId(
      "${page.url.templateArgs["site"]!""}"
   ).setTitle("${page.url.args["title"]!""}").setMessages(
      ${messages}
   );
//]]></script>
<div id="${args.htmlid}-body" class="toolbar">
   <div class="header">
      <div class="new-page"><a href="${page.url.context}/page/site/${page.url.templateArgs["site"]}/wiki-create" id="${args.htmlid}-create-button">${msg("button.create")}</a></div>
      <div class="separator">|</div>
      <div class="delete-page"><button id="${args.htmlid}-delete-button">${msg("button.delete")}</button></div>
      <div class="separator">|</div>
      <div class="rename-page"><button id="${args.htmlid}-rename-button">${msg("button.rename")}</button></div>
   </div>

   <div id="${args.htmlid}-rssFeed" class="rss-feed"><a href="${url.context}/proxy/alfresco/slingshot/wiki/pages/${page.url.templateArgs["site"]}?format=rss">RSS feed</a></div>
   
   <div id="${args.htmlid}-renamepanel">
      <div class="hd">${msg("panel.rename.title")}</div>
         <div class="bd">
            <form id="${args.htmlid}-renamePageForm" method="post" action="${url.context}/proxy/alfresco/slingshot/wiki/page/${page.url.templateArgs["site"]}/${page.url.args["title"]!""}">
               <div class="yui-ge">
                  <div class="yui-u first">
                     <input type="text" id="${args.htmlid}-renameTo" name="name" value="" size="30" tabindex="1" />
                  </div>
                  <div class="yui-u">
                     <input type="submit" id="${args.htmlid}-rename-save-button" value="${msg("button.save")}" tabindex="2" />
                  </div>
               </div>
            </form>
            <div class="bdft">${msg("panel.rename.footer")}</div>
         </div>
      </div>
   </div>   
</div>