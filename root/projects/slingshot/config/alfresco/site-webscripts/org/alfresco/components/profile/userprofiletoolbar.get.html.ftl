<#if activeUserProfile>
   <#assign activePage = page.url.templateArgs.pageid?lower_case!"">
   <div id="${args.htmlid}-body" class="toolbar userprofile">
      <div class="link"><a href="profile" <#if activePage=="profile">class="activePage"</#if>>${msg("link.viewmyprofile")}</a></div>
      <div class="separator">&nbsp;</div>
      <div class="link"><a href="change-password" <#if activePage=="change-password">class="activePage"</#if>>${msg("link.changepassword")}</a></div>
      <!--<div class="separator">&nbsp;</div>
      <div class="link"><a href="#settings" <#if activePage=="settings">class="activePage"</#if>>${msg("link.settings")}</a></div>-->
   </div>
</#if>