<#assign templateStylesheets = []>

<#macro link rel type href>
   <#assign templateStylesheets = templateStylesheets + [href]>
</#macro>

<#macro renderStylesheets>
   <style type="text/css" media="screen">
   <#list templateStylesheets as href>
      @import "${href}";
   </#list>
   </style>
</#macro>

<html xmlns="http://www.w3.org/1999/xhtml">
   <head>
      <title>Form Test Page</title>
      <meta http-equiv="X-UA-Compatible" content="Edge" />

      <@link rel="stylesheet" type="text/css" href="${url.context}/yui/reset-fonts-grids/reset-fonts-grids.css" />
      <@link rel="stylesheet" type="text/css" href="${url.context}/yui/assets/skins/default/skin.css" />

      <script type="text/javascript" src="${url.context}/js/log4javascript.v1.4.1.js"></script>
      <script type="text/javascript" src="${url.context}/yui/yahoo/yahoo-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/event/event-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/dom/dom-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/dragdrop/dragdrop-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/animation/animation-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/logger/logger-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/connection/connection-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/element/element-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/get/get-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/yuiloader/yuiloader-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/button/button-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/container/container-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/menu/menu-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/json/json-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/selector/selector-debug.js"></script>
      <script type="text/javascript" src="${url.context}/yui/yui-patch.js"></script>
      <script type="text/javascript" src="${url.context}/js/bubbling.v2.1.js"></script>
      <script type="text/javascript" src="${url.context}/service/messages.js?locale=en_US"></script>
      <script type="text/javascript" src="${url.context}/js/alfresco.js"></script>
      <script type="text/javascript" src="${url.context}/js/forms-runtime.js"></script>
      
      <script type="text/javascript">//<![CDATA[
         Alfresco.constants = Alfresco.constants || {};
         Alfresco.constants.DEBUG = true;
         Alfresco.constants.AUTOLOGGING = false;
         Alfresco.constants.PROXY_URI = window.location.protocol + "//" + window.location.host + "${url.context}/proxy/alfresco/";
         Alfresco.constants.PROXY_URI_RELATIVE = "${url.context}/proxy/alfresco/";
         Alfresco.constants.THEME = "default";
         Alfresco.constants.URL_CONTEXT = "${url.context}/";
         Alfresco.constants.URL_PAGECONTEXT = "${url.context}/page/";
         Alfresco.constants.URL_SERVICECONTEXT = "${url.context}/service/";
         Alfresco.constants.USERNAME = "admin";
         Alfresco.constants.HTML_EDITOR = "tinyMCE";
         Alfresco.constants.URI_TEMPLATES = {};
      //]]></script>
      
      <@renderStylesheets />
      
      ${head}
      
      <style type="text/css" media="screen">
         body
         {
            text-align: left;
         }
         
         .form-console
         {
            padding: 10px; 
            margin: 10px; 
            background-color: #8dc3e9;
            border: 1px dotted #c0c0c0;
         }
         
         .form-console h2
         {
            font-family: Helvetica,Arial,sans-serif;
            font-size: 146.5%;
         }
         
         .form-console fieldset
         {
            border: 1px solid black; 
            margin-top: 10px; 
            padding: 8px;
         }
         
         .form-console legend
         {
            color: black;
         }
         
         .form-console input
         {
            margin: 5px 5px 5px 0px;
         }
         
         .form-console .inline-label
         {
            margin-right: 5px;
            margin-left: 5px;
            font-weight: bold;
         }
         
         .form-console .button
         {
            margin-top: 15px;
         }
         
         .form-instance
         {
            margin-left: 1.4em; 
            margin-top: 1.4em;
         }
      </style>
      
   </head>
   <body class="yui-skin-default">
      <div id="bd">
         <div class="form-console">
            <h2>Form Test Page</h2>
            <#if url.args.mode?exists>
               <#assign mode="${url.args.mode}">
            <#else>
               <#assign mode="edit">
            </#if>
            <#if url.args.submitType?exists>
               <#assign submitType="${url.args.submitType}">
            <#else>
               <#assign submitType="multipart">
            </#if>
            <form method="get">
               <fieldset>
                  <legend>Item Details</legend>
                  <label for="itemKind">Kind:</label>
                  <input id="itemKind" type="text" name="itemKind" value="<#if url.args.itemKind?exists>${url.args.itemKind}<#else>node</#if>" size="5" />
                  <label for="itemId">Id:</label>
                  <input id="itemId" type="text" name="itemId" value="<#if url.args.itemId?exists>${url.args.itemId}</#if>" size="70" />
                  <br/>
                  <label for="destination">Destination:</label>
                  <input id="destination" type="text" name="destination" value="<#if url.args.destination?exists>${url.args.destination}</#if>" size="77" />
               </fieldset>
               <fieldset>
                  <legend>Form Details</legend>
                  <label for="formId">Id:</label>
                  <input id="formId" name="formId" value="<#if url.args.formId?exists>${url.args.formId}</#if>" />
                  <label class="inline-label">Mode:</label>
                  <input id="mode-view" type="radio" name="mode" value="view"<#if mode == "view"> checked</#if>>&nbsp;View&nbsp;
                  <input id="mode-edit" type="radio" name="mode" value="edit"<#if mode == "edit"> checked</#if>>&nbsp;Edit&nbsp;
                  <input id="mode-create" type="radio" name="mode" value="create"<#if mode == "create"> checked</#if>>&nbsp;Create&nbsp;
                  <label class="inline-label">Submit Type:</label>
                  <input id="submitType-multi" type="radio" name="submitType" value="multipart"<#if submitType == "multipart"> checked</#if>>&nbsp;Multipart&nbsp;
                  <input id="submitType-json" type="radio" name="submitType" value="json"<#if submitType == "json"> checked</#if>>&nbsp;JSON&nbsp;
                  <input id="submitType-url" type="radio" name="submitType" value="urlencoded"<#if submitType == "urlencoded"> checked</#if>>&nbsp;URL Encoded&nbsp;&nbsp;&nbsp;
                  <br/>
                  <label for="redirect">Redirect:</label>
                  <input id="redirect" type="text" name="redirect" value="<#if url.args.redirect?exists>${url.args.redirect}</#if>" size="80" />
               </fieldset>
               <input type="submit" value="Show Form" class="button" />
               <input type="button" value="Clear" class="button"
                      onclick="javascript:document.getElementById('itemKind').value='';document.getElementById('itemId').value='';document.getElementById('formId').value='';" />
            </form>
         </div>
         
         <div class="form-instance">
            <@region id="form-ui" scope="template" />
         </div>

      </div>

   </body>
</html>