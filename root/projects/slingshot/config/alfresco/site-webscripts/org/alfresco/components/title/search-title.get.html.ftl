<div class="page-title search-title theme-bg-color-1">
	<h1 class="theme-color-3"><span>${msg("header.searchresults")}</span></h1>	
	<#if page.url.templateArgs.site??>
      <div>
         <span class="navigation-item backLink">
            <a href="${url.context}/page/site/${page.url.templateArgs.site}/dashboard">${msg("header.backlink", profile.title?html)}</a>
         </span>
      </div>
   </#if>
   <div class="clear"></div>
</div>