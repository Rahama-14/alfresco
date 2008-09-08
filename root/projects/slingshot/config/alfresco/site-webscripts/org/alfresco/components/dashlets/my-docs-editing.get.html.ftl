<#macro doclibUrl doc>
   <a href="${url.context}/page/site/${doc.location.site}/documentlibrary?file=${doc.fileName?url}&amp;filter=editingMe">${doc.displayName?html}</a>
</#macro>
<div class="dashlet">
   <div class="title">${msg("header")}</div>
   <div class="body scrollableList">
      <#if docs.message?exists>
         <span class="error">${docs.message}</span>
      <#else>
         <#if docs.items?size == 0>
            <span>${msg("label.noItems")}</span>
         <#else>
            <#list docs.items?sort_by("modifiedOn") as doc>
               <#assign modifiedBy><a href="${url.context}/page/user/${doc.modifiedByUser?url}/profile">${doc.modifiedBy?html}</a></#assign>
               <div class="detail-list-item">
                  <div>
                     <div class="icon">
                        <img src="${url.context}/components/images/generic-file-32.png" alt="${doc.displayName?html}" />
                     </div>
                     <div class="details">
                        <h4><@doclibUrl doc /></h4>
                        <div>
                           ${msg("text.editing-since", doc.modifiedOn?datetime("dd MMM yyyy HH:mm:ss 'GMT'Z '('zzz')'")?string("dd MMM, yyyy HH:mm"))}
                        </div>
                     </div>
                  </div>
               </div>
            </#list>
         </#if>
      </#if>
   </div>
</div>