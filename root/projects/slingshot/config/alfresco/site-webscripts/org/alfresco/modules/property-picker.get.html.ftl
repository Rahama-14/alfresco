<#assign el=args.htmlid>
<div id="${el}-dialog" class="property-picker">
   <div class="hd">
      <span id="${el}-title">${msg("header")}</span></div>
   <div class="bd">
      <div id="${el}-tabs"></div>
   </div>
   <div class="bdft">
      <input type="button" id="${el}-ok-button" value="${msg("button.ok")}" tabindex="0"/>
      <input type="button" id="${el}-cancel-button" value="${msg("button.cancel")}" tabindex="0"/>
   </div>
</div>
<script type="text/javascript">//<![CDATA[
Alfresco.util.addMessages(${messages}, "Alfresco.module.PropertyPicker");
<#if transientContentProperties??>
Alfresco.util.ComponentManager.get("${el}").setOptions(
{
   transientProperties: {
      "d:content" : ${transientContentProperties}
   }
});
</#if>
//]]></script>

