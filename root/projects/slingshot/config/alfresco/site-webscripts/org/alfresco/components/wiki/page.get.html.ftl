<#-- Tags -->
<#if result.tags?? && result.tags?size &gt; 0>
   <#assign tags=result.tags />
<#else>
   <#assign tags=[] />
</#if>
   
<script type="text/javascript">//<![CDATA[
	new Alfresco.Wiki("${args.htmlid}").setOptions(
	{
	   siteId: "${page.url.templateArgs.site}",
      pageTitle: "${page.url.args["title"]!""}",
	   mode: "${page.url.args["action"]!"view"}",
	   tags: [<#list tags as tag>"${tag}"<#if tag_has_next>,</#if></#list>],
      pages: [<#if pageList.pages?size &gt; 0><#list pageList.pages as p>"${p.name}"<#if p_has_next>, </#if></#list></#if>] 
	}).setMessages(
      ${messages}
   );    
//]]></script>
<#-- Note, since result.pagetext has already been stripped by the page.get.js script -->
<div class="yui-g wikipage-bar">

   <div class="title-bar">
      <div id="${args.htmlid}-viewButtons" class="yui-u first pageTitle">
         ${page.url.args["title"]?replace("_", " ")}
      </div>
      <div class="yui-u align-right">
<#assign action = page.url.args["action"]!"view"> 
<#assign tabs =
[
   { 
      "label": msg("tab.view"),
      "action": "view"
   },
   {
      "label": msg("tab.edit"),
      "action": "edit"
   },
   {
      "label": msg("tab.details"),
      "action": "details"
   }
]>
<#list tabs as tab>
   <#if tab.action == action>
         <span class="tabSelected">${tab.label}</span>
   <#else>
         <a href="?title=${page.url.args["title"]!""}&amp;action=${tab.action}" class="tabLabel">${tab.label}</a>
   </#if>
   <#if tab_has_next>
         <span class="separator">|</span>
   </#if>
</#list>
      </div>
   </div>
</div>  
<div id="${args.htmlid}-wikipage" class="yui-navset">       
	    <div class="yui-content" style="background: #FFFFFF;"> 
<#if action == "view">	    
	        <div id="${args.htmlid}-page" class="rich-content"><#if result.pagetext??>${result.pagetext}<#elseif result.error??>${result.error}</#if></div> 
<#elseif action == "edit">	        
	        <div>
	            <form id="${args.htmlid}-form" action="${page.url.context}/proxy/alfresco/slingshot/wiki/page/${page.url.templateArgs.site}/${page.url.args["title"]}" method="post">
	               <fieldset>
	            <#assign pageContext = page.url.context + "/page/site/" + page.url.templateArgs.site + "/wiki-page?title=" + page.url.args["title"]>
   	            <input type="hidden" name="context" value="${pageContext?html}" />
   	            <input type="hidden" name="page" value="wiki-page" />
                  <textarea name="pagecontent" id="${args.htmlid}-pagecontent" cols="50" rows="10"><#if result.pagetext??>${result.pagetext}</#if></textarea>
              
               <#import "/org/alfresco/modules/taglibrary/taglibrary.lib.ftl" as taglibraryLib/>
               
               <!-- Render the tag inputs -->
               <@taglibraryLib.renderTagLibraryHTML htmlid=args.htmlid />
               <!-- end tags -->
      			      <div>
      	               <input type="submit" id="${args.htmlid}-save-button" value="${msg("button.save")}" />
      				      <input type="submit" id="${args.htmlid}-cancel-button" value="${msg("button.cancel")}" />
      	            </div>
                  </fieldset>
	            </form>
			</div> 
<#elseif action == "details">	    		
			<div>
   			<div style="border: 3px solid #CCC; margin-bottom:15px; width:100%; height:400px; overflow-y:auto">
   			<div class="yui-g" style="background: #CCC;">
			   <div class="yui-u first"><h2>${result.title!""}</h2></div>
			   <div class="yui-u">
			   	<#if versionhistory??>
			      <div style="float:right">
				      <select id="${args.htmlid}-selectVersion">
				      <#list result.versionhistory as version>
				         <option value="${version.versionId}">${version.version} <#if version_index = 0>(Latest)</#if></option>
				      </#list>
				      </select>
			      </div>
			      </#if>
			   </div>
			</div>
			<div id="${args.htmlid}-page">
			   <#-- PAGE CONTENT GOES HERE -->
			   <#if result.pagetext??>${result.pagetext}</#if>
			</div>
			<div id="${args.htmlid}-pagecontent" style="display:none;"><#if result.pagetext??>${result.pagetext}</#if></div>		
			</div>
			 <div style="display:none; margin-bottom: 5px;" id="${args.htmlid}-revertPanel"><button id="${args.htmlid}-revert-button">${msg("button.revert")}</button></div>
			<div class="yui-gb">
			   <div class="yui-u first">
			      <div class="columnHeader">${msg("label.versionHistory")}</div>
			      <#if result.versionhistory??>
			      <#list result.versionhistory as version>
			      <table class="versionHistory">
			         <tr><td colspan="2" class="pageTitle">${version.name}</td></tr>
			         <tr><td class="attributeLabel">${msg("label.version")}:</td><td class="attribute">${version.version}</td></tr>
			         <tr><td class="attributeLabel">${msg("label.modifier")}:</td><td class="attribute">${version.author}</td></tr>
			         <tr><td class="attributeLabel">${msg("label.modifiedOn")}:</td><td class="attribute">${version.date}</td></tr>
			      </table>
			      </#list>
			      </#if>
			   </div>
			   <div class="yui-u">
			      <div class="columnHeader">${msg("label.tags")}</div>
			      <#if result.tags?? && result.tags?size &gt; 0>
			         <#list result.tags as tag>
			            <div><span class="tagDetails">${tag}</span></div>
			         </#list>
               <#else>
                  ${msg("label.none")}
               </#if>
			   </div>
			   <div class="yui-u">
			      <div class="columnHeader">${msg("label.linkedPages")}</div>
			      <#if result.links??>
			         <#list result.links as link>
			            <div><span><a href="${page.url.context}/page/site/${page.url.templateArgs.site}/wiki-page?title=${link?replace(" ", "_")}">${link}</a></span></div>
			         </#list>
			      </#if>
			   </div>
			</div><#-- end of yui-gb -->
			</div>
</#if>
</div> 	    
</div>
