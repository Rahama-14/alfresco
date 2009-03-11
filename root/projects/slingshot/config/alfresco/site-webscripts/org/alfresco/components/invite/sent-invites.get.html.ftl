<script type="text/javascript">//<![CDATA[
   new Alfresco.SentInvites("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!""}"
   }).setMessages(
      ${messages}
   );
//]]></script>

<div id="${args.htmlid}-sentinvites" class="sent-invites">

   <div class="title">${msg("sentinvites.title")}</div>

   <div id="${args.htmlid}-wrapper" class="sent-invites-wrapper">
      <div class="search-bar theme-bg-color-3">
         <div class="search-label"><label for="${args.htmlid}-search-text">${msg("label.search")}</label></div>
         <div class="search-text"><input type="text" id="${args.htmlid}-search-text" name="-" value="" /></div>
         <div class="search-button"><button id="${args.htmlid}-search-button">${msg("button.search")}</button></div>
      </div>
      
      <#--
      <div class="tool-bar yui-gc">
         <div id="${args.htmlid}-paginator" class="paginator yui-b first">
         </div>
         <div class="tools yui-b">
            <button name="select-clear-button">Clear All...</button>
            <button name="clear-pending-button">Clear All Pending</button>
         </div>
      </div>
      -->
      <div id="${args.htmlid}-results" class="results"></div>
   </div>
</div>