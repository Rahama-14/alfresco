<#macro renderFormsRuntime formId>
   <script type="text/javascript">//<![CDATA[
      new Alfresco.FormUI("${formId}", "${args.htmlid}").setOptions(
      {
         mode: "${form.mode}",
         <#if form.mode == "view">
         arguments:
         {
            itemKind: "${form.arguments.itemKind!""}",
            itemId: "${form.arguments.itemId!""}",
            formId: "${form.arguments.formId!""}"
         }
         <#else>
         enctype: "${form.enctype}",
         fieldConstraints: 
         [
            <#list form.constraints as constraint>
            {
               fieldId : "${args.htmlid}_${constraint.fieldId}", 
               handler : ${constraint.validationHandler}, 
               params : ${constraint.params}, 
               event : "${constraint.event}",
               message : <#if constraint.message?exists>"${constraint.message?js_string}"<#else>null</#if>
            }
            <#if constraint_has_next>,</#if>
            </#list>
         ]
         </#if>
      }).setMessages(
         ${messages}
      );
   //]]></script>
</#macro> 

<#macro renderFormContainer formId>
   <div id="${formId}-container" class="form-container">
      <#if form.showCaption?exists && form.showCaption>
         <div id="${formId}-caption" class="caption"><span class="mandatory-indicator">*</span>${msg("form.required.fields")}</div>
      </#if>
         
      <#if form.mode != "view">
         <form id="${formId}" method="${form.method}" accept-charset="utf-8" enctype="${form.enctype}" action="${form.submissionUrl}">
      </#if>
      
      <#if form.mode == "create" && form.destination?? && form.destination?length &gt; 0>
         <input id="${formId}-destination" name="alf_destination" type="hidden" value="${form.destination}" />
      </#if>
      
      <#if form.mode != "view" && form.redirect?? && form.redirect?length &gt; 0>
         <input id="${formId}-redirect" name="alf_redirect" type="hidden" value="${form.redirect}" />
      </#if>
      
      <div id="${formId}-fields" class="form-fields">
         <#nested>
      </div>
         
      <#if form.mode != "view">
         <@renderFormButtons formId=formId />
         </form>
      </#if>
   </div>
</#macro>

<#macro renderFormButtons formId>         
   <div id="${formId}-buttons" class="form-buttons">
      <input id="${formId}-submit" type="submit" value="${msg("form.button.submit.label")}" />
      <#if form.showResetButton?exists && form.showResetButton>
         &nbsp;<input id="${formId}-reset" type="reset" value="${msg("form.button.reset.label")}" />
      </#if>
      <#if form.showCancelButton?exists && form.showCancelButton>
         &nbsp;<input id="${formId}-cancel" type="button" value="${msg("form.button.cancel.label")}" />
      </#if>
   </div>
</#macro>   

<#macro renderField field>
   <#if field.control.template?exists>
      <#assign fieldHtmlId=args.htmlid + "_" + field.id >
      <#include "${field.control.template}" />
   </#if>
</#macro>

<#macro renderSet set>
   <#if set.appearance?exists>
      <#if set.appearance == "fieldset">
         <fieldset><legend>${set.label}</legend>
      <#elseif set.appearance == "bordered-panel">
         <div class="set-bordered-panel">
            <div class="set-bordered-panel-heading">${set.label}</div>
            <div class="set-bordered-panel-body">
      <#elseif set.appearance == "panel">
         <div class="set-panel">
            <div class="set-panel-heading">${set.label}</div>
            <div class="set-panel-body">
      <#elseif set.appearance == "title">
         <div class="set-title">${set.label}</div>
      <#elseif set.appearance == "whitespace">
         <div class="set-whitespace"></div>
      </#if>
   </#if>
   
   <#if set.template??>
      <#include "${set.template}" />
   <#else>
      <#list set.children as item>
         <#if item.kind == "set">
            <@renderSet set=item />
         <#else>
            <@renderField field=form.fields[item.id] />
         </#if>
      </#list>
   </#if>
   
   <#if set.appearance?exists>
      <#if set.appearance == "fieldset">
         </fieldset>
      <#elseif set.appearance == "panel" || set.appearance == "bordered-panel">
            </div>
         </div>
      </#if>
   </#if>
</#macro>

<#macro renderFieldHelp field>
   <#if field.help?? && field.help?length &gt; 0>
      <span class="help-icon">
         <img src="${url.context}/components/form/images/help.png" title="${msg("form.field.help")}" 
              onclick="javascript:Alfresco.util.toggleHelpText('${fieldHtmlId}-help');" />
      </span>
      <div class="help-text" id="${fieldHtmlId}-help">${field.help?html}</div>
   </#if>
</#macro>
