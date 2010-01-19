<div id="${args.htmlid}-dialog" class="create-site">
   <div class="hd">${msg("header.createSite")}</div>
   <div class="bd">
      <form id="${args.htmlid}-form" method="POST" action="">
         <input type="hidden" id="${args.htmlid}-visibility" name="visibility" value="PUBLIC"/>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-title">${msg("label.name")}:</label></div>
            <div class="yui-u"><input id="${args.htmlid}-title" type="text" name="title" tabindex="0" maxlength="255" />&nbsp;*</div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-shortName">${msg("label.shortName")}:</label></div>
            <div class="yui-u">
               <input id="${args.htmlid}-shortName" type="text" name="shortName" tabindex="0" maxlength="255" />&nbsp;*<br>
               <span class="help">${msg("label.shortNameHelp")}</span>
            </div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-description">${msg("label.description")}:</label></div>
            <div class="yui-u"><textarea id="${args.htmlid}-description" name="description" rows="3" tabindex="0"></textarea></div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-sitePreset">${msg("label.type")}:</label></div>
            <div class="yui-u">
               <select id="${args.htmlid}-sitePreset" name="sitePreset" tabindex="0">
                  <#list sitePresets as sitePreset>
                     <option value="${sitePreset.id}">${sitePreset.name}</option>
                  </#list>
               </select>
            </div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-isPublic">${msg("label.access")}:</label></div>
            <div class="yui-u">
               <input id="${args.htmlid}-isPublic" type="radio" checked="checked" tabindex="0" name="-" /> ${msg("label.isPublic")}<br />
               <div class="moderated">
                  <input id="${args.htmlid}-isModerated" type="checkbox" tabindex="0" name="-"/> ${msg("label.isModerated")}<br />
                  <span class="help"><label for="${args.htmlid}-isModerated">${msg("label.moderatedHelp")}</label></span>
               </div>
            </div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first">&nbsp;</div>
            <div class="yui-u">
               <input id="${args.htmlid}-isPrivate" type="radio" tabindex="0" name="-" /> ${msg("label.isPrivate")}
            </div>
         </div>
         <div class="bdft">
            <input type="submit" id="${args.htmlid}-ok-button" value="${msg("button.ok")}" tabindex="0"/>
            <input type="button" id="${args.htmlid}-cancel-button" value="${msg("button.cancel")}" tabindex="0"/>
         </div>
      </form>
   </div>
</div>

<script type="text/javascript">//<![CDATA[
Alfresco.util.addMessages(${messages}, "Alfresco.module.CreateSite");
//]]></script>
