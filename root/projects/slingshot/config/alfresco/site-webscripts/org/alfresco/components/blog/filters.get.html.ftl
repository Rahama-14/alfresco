<script type="text/javascript">//<![CDATA[
   new Alfresco.component.BaseFilter("Alfresco.BlogPostListFilter", "${args.htmlid}");
//]]></script>
<div class="filter blog-filter">
   <h2>${msg("header.browseposts")}</h2>
   <ul class="filterLink">
   <#list filters as filter>
      <li><span class="${filter.id}"><a class="filter-link" href="#">${msg(filter.label)}</a></span></li>
   </#list>
   </ul>
</div>