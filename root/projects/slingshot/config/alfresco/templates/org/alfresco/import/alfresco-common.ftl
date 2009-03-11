<#macro uriTemplate>
   <#assign uriConfig = config.scoped["UriTemplate"]["uri-templates"]>
   <script type="text/javascript">//<![CDATA[
      Alfresco.constants.URI_TEMPLATES =
      {
   <#list uriConfig.childrenMap["uri-template"] as t>
         ${t.attributes["id"]}: "${t.value}"<#if t_has_next>,</#if>
   </#list>
      }
   //]]></script>
</#macro>

<#macro htmlEditor htmlEditor="YAHOO.widget.SimpleEditor">
   <script type="text/javascript">//<![CDATA[
      Alfresco.constants.HTML_EDITOR = '${htmlEditor}';
   //]]></script>
</#macro>
