<script type="text/javascript">//<![CDATA[
new Alfresco.SiteLinks("${args.htmlid}").setOptions({
   siteId: "${page.url.templateArgs.site!''}",
   links: [
   <#if links??>
	   <#list links as link>
      {
         id: '${link.name?js_string}',
         title: '${link.title?js_string}',
         url: '${link.url?js_string}',
         description: '${link.description?js_string}'
      }<#if (link_has_next)>,</#if>
	   </#list>
   </#if>
   ]
});
//]]></script>

<#assign site=page.url.templateArgs.site>

<div class="dashlet site-links">
   <div class="title">${msg("header.links")}</div>
   <div class="toolbar">
      <a id="${args.htmlid}-createLink-button" class="create-link" >${msg("link.createLink")}</a>
   </div>
   <div class="body scrollableList">
<#if links??>
   <#list links as link>
      <#assign linkUrl=link.url >
      <div id="${args.htmlid}-link-div-${link.name}" class="detail-list-item <#if link_index = 0>first-item<#elseif !link_has_next>last-item</#if>">
         <div>
            <div class="link">
               <a <#if !link.internal>target="_blank"</#if> href="<#if link.url?substring(0,1) == "/" || link.url?index_of("://") == -1>http://</#if>${link.url}">${link.title?html}</a>
            </div>
            <div class="actions">
               <a id="${args.htmlid}-details-span-${link_index}" href="${url.context}/page/site/${site}/links-view?linkId=${link.name}" class="details" title="${msg("link.details")}">&nbsp;</a>
            </div>
         </div>
      </div>
   </#list>
<#else>
      <div class="detail-list-item first-item last-item">
         <span>${msg("label.noLinks")}</span>
      </div>
</#if>
   </div>
</div>