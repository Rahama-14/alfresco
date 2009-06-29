<#if field.control.params.showTime?exists && field.control.params.showTime == "true"><#assign showTime=true><#else><#assign showTime=false></#if>
<#if showTime><#assign viewFormat>${msg("form.control.date-picker.view.time.format")}</#assign><#else><#assign viewFormat>${msg("form.control.date-picker.view.date.format")}</#assign></#if>

<div class="form-field">
   <#if form.mode == "view">
      <div class="viewmode-field">
         <#if field.mandatory && field.value == "">
            <span class="incomplete-warning"><img src="${url.context}/components/form/images/warning-16.png" title="${msg("form.field.incomplete")}" /><span>
         </#if>
         <span class="viewmode-label">${field.label?html}:</span>
         <span class="viewmode-value"><#if field.value != "">${field.value?datetime("yyyy-MM-dd'T'HH:mm:ss")?string(viewFormat)}</#if></span>
      </div>
   <#else>
      <#assign controlId = fieldHtmlId + "-cntrl">
      
      <script type="text/javascript">//<![CDATA[
      (function()
      {
         new Alfresco.DatePicker("${controlId}", "${fieldHtmlId}").setOptions(
         {
            <#if form.mode == "view" || field.disabled>disabled: true,</#if>
            currentValue: "${field.value}",
            showTime: ${showTime?string},
            mandatory: ${field.mandatory?string}
         }).setMessages(
            ${messages}
         );
      })();
      //]]></script>
   
      <input id="${fieldHtmlId}" type="hidden" name="${field.name}" value="${field.value}" />
   
      <label for="${controlId}-date">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
      <input id="${controlId}-date" name="-" type="text" class="date-entry" <#if field.description?exists>title="${field.description}"</#if> <#if field.disabled>disabled="true"</#if> />
   
      <#if field.disabled == false>
         <a id="${controlId}-icon"><img src="${url.context}/components/form/images/calendar.png" class="datepicker-icon"/></a>
      </#if>
   
      <div id="${controlId}" class="datepicker"></div>
   
      <#if showTime>
         <input id="${controlId}-time" name="-" type="text" class="time-entry" <#if field.description?exists>title="${field.description}"</#if> <#if field.disabled>disabled="true"</#if> />
      </#if>
   
      <div class="format-info">
      <span class="date-format">${msg("form.control.date-picker.display.date.format")}</span>
      <#if showTime><span class="time-format<#if field.disabled>-disabled</#if>">${msg("form.control.date-picker.display.time.format")}</span></#if>
      </div>
   </#if>
</div>