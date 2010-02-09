<#include "/org/alfresco/components/form/controls/common/utils.inc.ftl" />
<div class="form-field">
   <#if form.mode == "view">
      <div class="viewmode-field">
         <#if field.mandatory && field.value == "">
            <span class="incomplete-warning"><img src="${url.context}/components/form/images/warning-16.png" title="${msg("form.field.incomplete")}" /><span>
         </#if>
         <span class="viewmode-label">${field.label?html}:</span>
         <span class="viewmode-value">${field.value?html}</span>
      </div>
   <#else>
      <label for="${fieldHtmlId}">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
      <#if field.control.params.options?exists && field.control.params.options != "">
         <select id="${fieldHtmlId}" name="${field.name}" tabindex="0"
               <#if field.description?exists>title="${field.description}"</#if>
               <#if field.control.params.size?exists>size="${field.control.params.size}"</#if> 
               <#if field.control.params.styleClass?exists>class="${field.control.params.styleClass}"</#if>
               <#if field.disabled>disabled="true"</#if>>
               <#list field.control.params.options?split(",") as nameValue>
                  <#if nameValue?index_of("|") == -1>
                     <option value="${nameValue?html}"<#if nameValue == field.value?string> selected="selected"</#if>>${nameValue?html}</option>
                  <#else>
                     <#assign choice=nameValue?split("|")>
                     <option value="${choice[0]?html}"<#if choice[0] == field.value?string> selected="selected"</#if>>${msgValue(choice[1])?html}</option>
                  </#if>
               </#list>
         </select>
      <#else>
         <div id="${fieldHtmlId}" class="missing-options">${msg("form.control.selectone.missing-options")}</div>
      </#if>
   </#if>
</div>