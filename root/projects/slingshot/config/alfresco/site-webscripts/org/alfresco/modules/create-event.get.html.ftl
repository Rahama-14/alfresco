<div id="${args.htmlid}-dialog" class="create-event">
   <div class="hd">Add event</div>
   <div class="bd">

      <form id="${args.htmlid}-addEvent-form" action="${url.context}/proxy/alfresco/calendar/create" method="POST">
         <input type="hidden" name="site" value="${args["site"]!""}" />
         <input type="hidden" name="page" value="calendar" />
         <input type="hidden" id="${args.htmlid}-from" name="from" value="${event.from!""}" />
         <input type="hidden" id="${args.htmlid}-to" name="to" value="${event.to!""}" />

         <div class="yui-g">
            <h2>Details</h2>
         </div>
         <div class="yui-gd">
            <div class="yui-u first">${msg("label.what")}: *</div>
            <div class="yui-u"><input id="${args.htmlid}-title" type="text" name="what" value="${event.what!""}" tabindex="1" /></div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first">${msg("label.where")}:</div>
            <div class="yui-u"><input id="${args.htmlid}-location" type="text" name="where" value="${event.location!""}" tabindex="2" /></div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first">${msg("label.description")}:</div>
            <div class="yui-u"><textarea id="${args.htmlid}-description" name="desc" rows="3" cols="20" tabindex="3">${event.description!""}</textarea></div>
         </div>
         <div class="yui-g">
            <h2>Time</h2>
         </div>
         <div class="yui-gd">
            <div class="yui-u first">${msg("label.allday")}:</div>
            <div class="yui-u"><input id="${args.htmlid}-allday" type="checkbox" name="allday" tabindex="4" /></div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first">${msg("label.startdate")}:</div>
            <div class="yui-u"><span id="${args.htmlid}-startdate"><input id="fd" type="text" name="datefrom" readonly="readonly" disabled value="<#if event.from?exists>${event.from?date("MM/dd/yyy")?string("EEEE, MMMM dd yyyy")}</#if>"/></span><span id="${args.htmlid}-starttime" class="eventTime">&nbsp;at&nbsp;<input id="${args.htmlid}-start" name="start" value="${event.start!"12:00"}" type="text" size="10" tabindex="6" /></span></div>
         </div>
         <div class="yui-gd">
            <div class="yui-u first">${msg("label.enddate")}:</div>
            <div class="yui-u"><span id="${args.htmlid}-enddate"><input id="td" type="text" name="dateto" readonly="readonly" disabled value="<#if event.to?exists>${event.to?date("MM/dd/yyy")?string("EEEE, MMMM dd yyyy")}</#if>"/></span><span id="${args.htmlid}-endtime" class="eventTime">&nbsp;at&nbsp;<input id="${args.htmlid}-end" name="end" value="${event.end!"13:00"}" type="text" size="10" tabindex="8" /></span></div>
         </div>
         <div class="yui-gd"> 
               <div class="yui-u first">${msg("label.tags")}:</div>
               <div class="yui-u"><input type="text" id="${args.htmlid}-tags" name="tags" value="<#if event.tags??><#list event.tags as tag>${tag}<#if tag_has_next>&nbsp;</#if></#list></#if>" tabindex="9" /> space separated</div>
         </div>
         <div class="bdft">
            <input type="submit" id="${args.htmlid}-ok-button" value="${msg("button.ok")}" tabindex="10" />
            <input type="submit" id="${args.htmlid}-cancel-button" value="${msg("button.cancel")}" tabindex="11" />
         </div>

      </form>

   </div>
</div>
