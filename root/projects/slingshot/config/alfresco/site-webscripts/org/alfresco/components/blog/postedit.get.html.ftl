<script type="text/javascript">//<![CDATA[
   new Alfresco.BlogPostEdit("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site}",
      editorConfig : 
      {
         <#--
         //YUI
         //             height: this.options.height + 'px',
         //             width: this.options.width + 'px',
         //             dompath: false, //Turns on the bar at the bottom
         //             animate: false, //Animates the opening, closing and moving of Editor windows
         //             markup: "xhtml",
         //             toolbar:  Alfresco.util.editor.getTextOnlyToolbarConfig(this._msg),
         -->
         //Tiny MCE
         theme:'advanced',
         theme_advanced_buttons1 : "bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,formatselect,fontselect,fontsizeselect,forecolor,backcolor",         
         theme_advanced_buttons2 :"bullist,numlist,|,outdent,indent,blockquote,|,undo,redo,|,link,unlink,anchor,image,cleanup,help,code,removeformat",
         theme_advanced_toolbar_location : "top",
         theme_advanced_toolbar_align : "left",
         theme_advanced_statusbar_location : "bottom",
         theme_advanced_resizing : true,
         theme_advanced_buttons3 : null,
         language:'${locale?substring(0, 2)}'         
      },
      containerId: "blog",
      <#if page.url.args.postId??>
         editMode: true,
         postId: "${page.url.args.postId?html}"
      <#else>
         editMode: false,
         postId: ""
      </#if>
   }).setMessages(
      ${messages}
   );
//]]></script>

<div class="editBlogPostForm">
<#if page.url.args.postId??>
   <h1>${msg("editPost")}</h1>
<#else>
   <h1>${msg("createPost")}</h1>
</#if>
<hr/>
</div>
<div id="${args.htmlid}-div" class="editBlogPostForm hidden">
   <form id="${args.htmlid}-form" method="post" action="">
      <div>
         <input type="hidden" id="${args.htmlid}-site" name="site" value="" />
         <input type="hidden" id="${args.htmlid}-container" name="container" value="" />
         <input type="hidden" id="${args.htmlid}-page" name="page" value="blog-postview" />
         <input type="hidden" id="${args.htmlid}-draft" name="draft" value=""/>
               
         <!-- title -->
         <label for="${args.htmlid}-title">${msg("title")}:</label>
         <input type="text" id="${args.htmlid}-title" name="title" value="" />

         <!-- content -->
         <label for="${args.htmlid}-content">${msg("text")}:</label>
         <textarea rows="8" id="${args.htmlid}-content" name="content" cols="180" class="yuieditor"></textarea> 
      
         <!-- tags -->
         <label for="${htmlid}-tag-input-field">${msg("tags")}:</label>
         <#import "/org/alfresco/modules/taglibrary/taglibrary.lib.ftl" as taglibraryLib/>
         <@taglibraryLib.renderTagLibraryHTML htmlid=args.htmlid />
         <!-- end tags -->

      </div>
      <div class="nodeFormAction">
         <input type="submit" id="${args.htmlid}-save-button" value="" />         
         <input type="button" id="${args.htmlid}-publish-button" value="${msg('action.publish')}" class="hidden" />
         <input type="button" id="${args.htmlid}-publishexternal-button" value="" />
         <input type="reset" id="${args.htmlid}-cancel-button" value="${msg('action.cancel')}" />
      </div>
   </form>
</div>
