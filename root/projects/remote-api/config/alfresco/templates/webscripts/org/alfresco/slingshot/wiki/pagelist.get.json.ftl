<#escape x as jsonUtils.encodeJSONString(x)>
{
   "totalPages" : ${pageList.pages?size},
   "pages":
   [
   <#list pageList.pages?sort_by(['modified'])?reverse as p>
      <#assign page = p.page>
      {
         "name" : "${page.name}",
         "editable" : "<#if page.hasPermission("Write")>true<#else>false</#if>",
         "title" : "<#if page.properties.title?exists>${page.properties.title}<#else>${page.name?replace("_", " ")}</#if>",
         <#-- Strip out any HTML tags -->
         "text" : "${page.content}",
         "tags" : [
             <#list p.tags as tag>
                "${tag}"<#if tag_has_next>,</#if>
             </#list>  
           ],
         "createdOn": "${page.properties.created?string("MMM dd yyyy, HH:mm:ss")}",
         <#if p.createdBy??>
            <#assign createdBy = (p.createdBy.properties.firstName + " " + p.createdBy.properties.lastName)?trim>
            <#assign createdByUser = p.createdBy.properties.userName>
         <#else>
            <#assign createdBy="">
            <#assign createdByUser="">
         </#if>
         "createdBy": "${createdBy}",
         "createdByUser": "${createdByUser}",
         "modifiedOn": "${page.properties.modified?string("MMM dd yyyy, HH:mm:ss")}",
         <#if p.modifiedBy??>
            <#assign modifiedBy = (p.modifiedBy.properties.firstName + " " + p.modifiedBy.properties.lastName)?trim>
            <#assign modifiedByUser = p.modifiedBy.properties.userName>
         <#else>
            <#assign modifiedBy="">
            <#assign modifiedByUser="">
         </#if>
         "modifiedBy": "${modifiedBy}",
         "modifiedByUser": "${modifiedByUser}"
      }<#if p_has_next>,</#if>
   </#list>
   ]
}
</#escape>