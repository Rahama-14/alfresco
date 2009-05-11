<#include "common/picker.inc.ftl" />

<#assign controlId = fieldHtmlId + "-cntrl">

<script type="text/javascript">//<![CDATA[
(function()
{
   <@renderPickerJS field "picker" />
   picker.setOptions(
   {
      itemType: "cm:category",
      multipleSelectMode: true,
      parentNodeRef: "alfresco://category/root",
      itemFamily: "category",
      maintainAddedRemovedItems: false,
      params: "${field.control.params.params!""}"
   });
})();
//]]></script>

<#if form.mode == "view">
<div id="${controlId}" class="viewmode-field">
   <#if field.mandatory && field.value == "">
      <span class="incomplete-warning"><img src="${url.context}/components/form/images/warning-16.png" title="${msg("form.incomplete.field")}" /><span>
   </#if>
   <span class="viewmode-label">${field.label?html}:</span>
   <span id="${controlId}-currentValueDisplay" class="viewmode-value current-values"></span>
</div>
<#else>

<label for="${controlId}">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>

<div id="${controlId}" class="object-finder">
   
   <div id="${controlId}-currentValueDisplay" class="current-values"></div>
   
   <#if form.mode != "view" && field.disabled == false>
   <input type="hidden" id="${fieldHtmlId}" name="${field.name}" value="${field.value}" />
   <div class="show-picker">
      <button id="${controlId}-showPicker-button">${msg("button.select")}</button>
   </div>

   <@renderPickerHTML controlId />
   </#if>
</div>
</#if>
