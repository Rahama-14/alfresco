<div id="${args.htmlid}-dialog" class="create-event">
<#if (!edit)>
   <div class="hd">${msg("title.addEvent")}</div>
<#else>
    <div class="hd">${msg("title.editEvent")}</div>
</#if>
   <div class="bd">

      <form id="${args.htmlid}-form" action="${url.context}/proxy/alfresco/calendar/create" method="POST">
         <input type="hidden" name="site" value="${args.site!""}" />
         <input type="hidden" name="page" value="calendar" />
         <input type="hidden" id="${args.htmlid}-from" name="from" value="${event.from!""}" />
         <input type="hidden" id="${args.htmlid}-to" name="to" value="${event.to!""}" />
         <div class="yui-g">
            <h2>${msg("section.details")}</h2>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-title">${msg("label.what")}:</label></div>
            <div class="yui-u"><input id="${args.htmlid}-title" type="text" name="what" value="${event.what!""}" tabindex="1" class="wide"/> * </div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-location">${msg("label.where")}:</label></div>
            <div class="yui-u"><input id="${args.htmlid}-location" type="text" name="where" value="${event.location!""}" tabindex="2" class="wide"/></div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-description">${msg("label.description")}:</label></div>
            <div class="yui-u"><textarea id="${args.htmlid}-description" name="desc" rows="3" cols="20" class="wide" tabindex="3">${event.description!""}</textarea></div>
         </div>
         <div class="yui-g">
            <h2>${msg("section.time")}</h2>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="${args.htmlid}-allday">${msg("label.allday")}:</label></div>
            <#if event.allday?exists>
            <div class="yui-u"><input id="${args.htmlid}-allday" type="checkbox" name="allday" tabindex="4" checked="checked"/></div>
            <#else>
            <div class="yui-u"><input id="${args.htmlid}-allday" type="checkbox" name="allday" tabindex="4"/></div>
            </#if>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="fd">${msg("label.startdate")}:</label></div>
            <div class="yui-u overflow"><span id="${args.htmlid}-startdate"><input id="fd" type="text" name="fromdate" readonly="readonly"  value="<#if event.from?exists>${event.from?date("MM/dd/yyy")?string("EEEE, MMMM dd yyyy")}</#if>"/></span><span id="${args.htmlid}-starttime" class="eventTime">&nbsp;<label for="${args.htmlid}-start">${msg("label.at")}&nbsp;</label><input id="${args.htmlid}-start" name="start" value="${event.start!"12:00"}" type="text" size="10" tabindex="6" /></span></div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first"><label for="td">${msg("label.enddate")}:</label></div>
            <div class="yui-u overflow"><span id="${args.htmlid}-enddate"><input id="td" type="text" name="todate" readonly="readonly"  value="<#if event.to?exists>${event.to?date("MM/dd/yyy")?string("EEEE, MMMM dd yyyy")}</#if>"/></span><span id="${args.htmlid}-endtime" class="eventTime"><label for="${args.htmlid}-end">${msg("label.at")}&nbsp;</label><input id="${args.htmlid}-end" name="end" value="${event.end!"13:00"}" type="text" size="10" tabindex="8" /></span></div>
         </div>
         <!-- tags -->
         <div class="yui-gd">
            <div class="yui-u first"><span class="label">${msg("label.tags")}:</span></div>
            <div class="yui-u overflow">
              <#import "/org/alfresco/modules/taglibrary/taglibrary.lib.ftl" as taglibraryLib/>
               <div class="taglibrary">
                  <div class="top_taglist tags_box">
                     <ul id="${args.htmlid}-current-tags">
                     </ul>
                  </div>
                  <div class="title">${msg("taglibrary.typetag")}&nbsp;</div>
                  <#assign tags = ''>
                  <#if event.tags?? && event.tags?size &gt; 0>
                     <#list event.tags as tag>
                        <#assign tags = tags + tag>
                        <#if tag_has_next><#assign tags = tags + ' '></#if>
                     </#list>
                  </#if>
                  <input type="text" size="30" class="rel_left" id="${args.htmlid}-tag-input-field" value="${tags}"/>
                  <input type="button" id="${args.htmlid}-add-tag-button" value="Add" />
                  <div class="bottom_taglist tags_box">
                     <a href="#" id="${args.htmlid}-load-popular-tags-link">${msg("taglibrary.populartagslink")}</a>
                     <ul id="${args.htmlid}-popular-tags">
                     </ul>
                  </div>
               </div>
               <!-- end tags -->                    
            </div>
         </div>
         <div class="bdft">
            <input type="submit" id="${args.htmlid}-ok" value="${msg("button.ok")}" tabindex="10" />
            <input type="submit" id="${args.htmlid}-cancel" value="${msg("button.cancel")}" tabindex="11" />
         </div>

      </form>

   </div>
</div>
