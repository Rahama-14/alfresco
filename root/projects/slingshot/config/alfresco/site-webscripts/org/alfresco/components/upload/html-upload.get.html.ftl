<div id="${args.htmlid}-dialog" class="html-upload hidden">
   <div class="hd">
      <span id="${args.htmlid}-title-span"></span>
   </div>
   <div class="bd">
      <form id="${args.htmlid}-htmlupload-form" action=""
            method="POST" enctype="multipart/form-data" accept-charset="utf-8"
            action="${url.context}/proxy/alfresco/api/upload.html" >
         <input type="hidden" id="${args.htmlid}-siteId-hidden" name="siteId" value=""/>
         <input type="hidden" id="${args.htmlid}-containerId-hidden" name="containerId" value=""/>
         <input type="hidden" id="${args.htmlid}-username-hidden" name="username" value=""/>
         <input type="hidden" id="${args.htmlid}-updateNodeRef-hidden" name="updateNodeRef" value=""/>
         <input type="hidden" id="${args.htmlid}-uploadDirectory-hidden" name="uploadDirectory" value=""/>
         <input type="hidden" id="${args.htmlid}-overwrite-hidden" name="overwrite" value=""/>
         <input type="hidden" id="${args.htmlid}-thumbnails-hidden" name="thumbnails" value=""/>
         <input type="hidden" id="${args.htmlid}-successCallback-hidden" name="successCallback" value=""/>
         <input type="hidden" id="${args.htmlid}-successScope-hidden" name="successScope" value=""/>
         <input type="hidden" id="${args.htmlid}-failureCallback-hidden" name="failureCallback" value=""/>
         <input type="hidden" id="${args.htmlid}-failureScope-hidden" name="failureScope" value=""/>

         <p>
            <span id="${args.htmlid}-singleUploadTip-span">${msg("label.singleUploadTip")}</span>
            <span id="${args.htmlid}-singleUpdateTip-span">${msg("label.singleUpdateTip")}</span>
         </p>

         <div>
            <div class="yui-g">
               <h2>${msg("section.file")}</h2>
            </div>
            <div class="yui-gd <#if (contentTypes?size == 1)>hidden</#if>">
               <div class="yui-u first">
                  <label for="${args.htmlid}-contentType-select">${msg("label.contentType")}</label>
               </div>
               <div class="yui-u">
                  <select id="${args.htmlid}-contentType-select" name="contentType">
                     <#if (contentTypes?size > 0)>
                        <#list contentTypes as contentType>
                           <option value="${contentType.id}">${contentType.value}</option>
                        </#list>
                     </#if>
                  </select>
               </div>
            </div>
            <div class="yui-gd">
               <div class="yui-u first">
                  <label for="${args.htmlid}-filedata-file">${msg("label.file")}</label>
               </div>
               <div class="yui-u">
                  <input type="file" id="${args.htmlid}-filedata-file" name="filedata">
               </div>
            </div>
         </div>

         <div id="${args.htmlid}-versionSection-div">
            <div class="yui-g">
               <h2>${msg("section.version")}</h2>
            </div>
            <div class="yui-gd">
               <div class="yui-u first">
                  <label for="${args.htmlid}-minorVersion-radioButton">${msg("label.version")}</label>
               </div> 
               <div class="yui-u">
                  <input id="${args.htmlid}-minorVersion-radioButton" type="radio" name="majorVersion" checked="checked" value="false"/> ${msg("label.minorVersion")}
               </div>
            </div>
            <div class="yui-gd">
               <div class="yui-u first">&nbsp;
               </div>
               <div class="yui-u">
                  <input id="${args.htmlid}-majorVersion-radioButton" type="radio" name="majorVersion" value="true"/> ${msg("label.majorVersion")}
               </div>
            </div>
            <div class="yui-gd">
               <div class="yui-u first">
                  <label for="${args.htmlid}-description-textarea">${msg("label.comments")}</label>
               </div>
               <div class="yui-u">
                  <textarea id="${args.htmlid}-description-textarea" name="description" rows="4"></textarea>
               </div>
            </div>
         </div>

         <div class="bdft">
            <input id="${args.htmlid}-upload-button" type="button" value="${msg("button.upload")}" />
            <input id="${args.htmlid}-cancel-button" type="button" value="${msg("button.cancel")}" />
         </div>

      </form>

   </div>
</div>
<script type="text/javascript">//<![CDATA[
new Alfresco.HtmlUpload("${args.htmlid}").setMessages(
   ${messages}
);
//]]></script>
