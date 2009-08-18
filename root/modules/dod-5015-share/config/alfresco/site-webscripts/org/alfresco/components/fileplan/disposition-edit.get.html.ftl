<script type="text/javascript">//<![CDATA[
new Alfresco.DispositionEdit("${args.htmlid}").setMessages(
   ${messages}
).setOptions({
   nodeRef: "${page.url.args.nodeRef}",
   siteId: "${page.url.templateArgs.site!""}",
   events: {
      <#list events as event>
      ${event.value}: { label: "${event.label}", automatic: ${event.automatic?string} }<#if (event_has_next)>, </#if>
      </#list>
   }
});
//]]></script>
<#assign el=args.htmlid>
<div class="disposition-edit">
   <div class="disposition-form">
      <div>
         <div class="header">${msg("header.action")}</div>
         <hr />
         <ol id="${el}-actionList" class="action-list">
            <li id="${el}-action-template-dummy"></li>
         </ol>
      </div>
   </div>

   <div id="${el}-flowButtons">
      <hr />
      <span id="${el}-createaction-button" class="yui-button createaction">
          <span class="first-child">
              <button type="button">${msg("button.createaction")}</button>
          </span>
      </span>
   </div>

   <div class="main-buttons">
      <hr />
      <span id="${el}-done-button" class="yui-button done">
          <span class="first-child">
              <button type="button">${msg("button.done")}</button>
          </span>
      </span>
   </div>

   <!-- Event and Action Templates -->
   <div style="display:none">
      <ul>
         <li id="${el}-event-template" class="event">
            <div class="action-event-relation">
               <span class="or">${msg("label.or")}</span>
               <span class="and">${msg("label.and")}</span>
            </div>
            <div class="action-event-name">
               <input type="hidden" name="events[]" class="action-event-name-value">
               <span class="action-event-name-label"></span>
            </div>
            <div class="action-event-completion"></div>
            <div class="action-event-buttons">
               <span class="delete" title="${msg("icon.deleteevent")}">&nbsp;</span>
            </div>
         </li>
      </ul>

      <ol>
         <li id="${el}-action-template" class="action collapsed">
            <div class="header">
               <div class="no"></div>
               <div class="buttons">
                  <span class="edit" title="${msg("icon.editaction")}">&nbsp;</span>
                  <span class="delete" title="${msg("icon.deleteaction")}">&nbsp;</span>
               </div>
               <div class="title"></div>
            </div>
            <div class="details" style="display:none;">
               <form class="action-form" method="" action="">
                  <input type="hidden" name="period" value="" class="period" />
                  <input type="hidden" name="id" value="" class="id" />
                  <div class="section">
                     ${msg("label.action")}
                     <select name="name" class="action-type">
                        <#list dispositionActions as action>
                        <option value="${action.value}">${action.label}</option>
                        </#list>
                     </select>
                  </div>
                  <div class="section">
                     <input type="checkbox" class="period-enabled" checked="true" />
                     ${msg("label.after")}
                     <input type="text" class="period-amount" />
                     <select class="period-unit">
                        <#list periodTypes as periodType>
                        <option value="${periodType.value}">${periodType.label}</option>
                        </#list>
                     </select>
                     ${msg("label.from")}
                     <select name="periodProperty" class="period-action">
                        <#list periodProperties as periodProperty>
                        <option value="${periodProperty.value}">${periodProperty.label}</option>
                        </#list>
                     </select>
                     <span class="or-relation">${msg("label.or")}</span>
                     <span class="and-relation">${msg("label.and")}</span>
                  </div>
                  <div class="section">
                     <input type="checkbox" class="events-enabled" checked="true" />
                     ${msg("label.when")}
                  </div>
                  <div class="section events">
                     <div class="events-header">
                        <div class="event-name-header">${msg("header.event")}</div>
                        <div class="event-completion-header">${msg("header.completion")}</div>
                        <hr/>
                     </div>
                     <ul class="events-list">
                        <li id="${el}-event-template-dummy"></li>
                     </ul>
                     <div class="events-header">
                        <input type="button" class="addevent-button" name="addevent-button" value="${msg("button.addevent")}">
                        <select class="addevent-menu" name="addevent-menu">
                           <#list events as event>
                           <option value="${event.value}">${event.label}</option>
                           </#list>                           
                        </select>
                        <hr />
                     </div>
                     <select name="eligibleOnFirstCompleteEvent" class="relation">
                        <option value="false">${msg("relation.option.and")}</option>
                        <option value="true">${msg("relation.option.or")}</option>
                     </select>
                  </div>
                  <div class="section">
                     <hr />
                     ${msg("label.description")}:<br />
                     <textarea name="description" class="description"></textarea>
                  </div>
                  <div class="buttons">
                     <span class="yui-button saveaction">
                        <span class="first-child">
                           <button type="button">${msg("button.save")}</button>
                        </span>
                     </span>
                     <span class="yui-button cancel">
                        <span class="first-child">
                           <button type="button">${msg("button.cancel")}</button>
                        </span>
                     </span>
                  </div>
               </form>
            </div>
         </li>
      </ol>
   </div>

</div>
