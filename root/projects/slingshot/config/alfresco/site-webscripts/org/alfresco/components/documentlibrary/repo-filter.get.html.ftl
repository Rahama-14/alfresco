<#assign filterIds = "">
<div class="filter doclib-filter">
   <h2>${msg("header.documents")}</h2>
   <ul class="filterLink">
   <#list filters as filter>
      <#assign filterIds>${filterIds}"${filter.id}"<#if filter_has_next>,</#if></#assign>
      <li><span class="${filter.id}"><a class="filter-link" rel="${filter.data?html}" href="#">${msg(filter.label)}</a></span></li>
   </#list>
   </ul>
</div>
<script type="text/javascript">//<![CDATA[
   new Alfresco.component.BaseFilter("Alfresco.DocListFilter", "${args.htmlid}").setFilterIds([${filterIds}]);
//]]></script>