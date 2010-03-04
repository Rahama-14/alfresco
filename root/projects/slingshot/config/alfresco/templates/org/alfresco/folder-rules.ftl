<#include "include/alfresco-template.ftl" />
<@templateHeader>
   <@link rel="stylesheet" type="text/css" href="${url.context}/templates/rules/folder-rules.css" />
   <@script type="text/javascript" src="${page.url.context}/templates/rules/folder-rules.js"></@script>
</@>

<@templateBody>
   <div id="alf-hd">
      <@region id="header" scope="global" protected=true />
      <@region id=doclibType + "title" scope="template" protected=true />
      <@region id=doclibType + "navigation" scope="template" protected=true />
   </div>
   <div id="bd">
      <@region id=doclibType + "path" scope="template" protected=true />
      <@region id="rules-header" scope="template" protected=true />
      <div class="clear"></div>

      <#if ruleset.linkedToRuleSet??>
         <@region id="rules-linked" scope="template" protected=true />
      <#elseif ruleset.rules??>
         <div class="yui-g">
            <div class="yui-g first">
               <div id="inherited-rules-container" class="hidden">
               <@region id="inherited-rules" scope="template" protected=true />
               </div>
               <@region id="folder-rules" scope="template" protected=true />
            </div>
            <div class="yui-g">
               <@region id="rule-details" scope="template" protected=true />
            </div>
         </div>
      <#else>
         <@region id="rules-none" scope="template" protected=true />
      </#if>
   </div>

   <script type="text/javascript">//<![CDATA[
   new Alfresco.FolderRules().setOptions(
   {
      nodeRef: new Alfresco.util.NodeRef("${url.args.nodeRef}"),
      siteId: "${page.url.templateArgs.site!""}",
      folder:
      {
         nodeRef: "${folder.nodeRef}",
         site: "${folder.site}", 
         name: "${folder.name}",
         path: "${folder.path}"
      },
      ruleset:
      {
         rules: <#if ruleset.rules??>[<#list ruleset.rules as rule>
            {
               id: "${rule.id}",
               title: "${rule.title}",
               description: "${rule.description}",
               url: "${rule.url}",
               disabled: ${rule.disabled?string},
               owningNode:
               {
                  nodeRef : "${rule.owningNode.nodeRef}",
                  name : "${rule.owningNode.name}"
               }
            }<#if rule_has_next>,</#if></#list>
         ]<#else>null</#if>,
         inheritedRules: <#if ruleset.inheritedRules??>[<#list ruleset.inheritedRules as rule>
            {
               id: "${rule.id}",
               title: "${rule.title}",
               description: "${rule.description}",
               url: "${rule.url}",
               disabled: ${rule.disabled?string},
               owningNode:
               {
                  nodeRef : "${rule.owningNode.nodeRef}",
                  name : "${rule.owningNode.name}"
               }
            }<#if rule_has_next>,</#if></#list>
         ]<#else>null</#if>,
         linkedFromRuleSets: <#if ruleset.linkedFromRuleSets??>[<#list ruleset.linkedFromRuleSets as link>
            "${link}"<#if link_has_next>,</#if></#list>
         ]<#else>null</#if>,
         linkedToRuleSet: <#if ruleset.linkedToRuleSet??>"${ruleset.linkedToRuleSet}"<#else>null</#if>
      },
      linkedToFolder: <#if linkedToFolder??>
      {
         nodeRef: "${linkedToFolder.nodeRef}",
         site: "${linkedToFolder.site}",
         name: "${linkedToFolder.name}",
         path: "${linkedToFolder.path}"
      }<#else>null</#if>         
   });
   //]]></script>

</@>

<@templateFooter>
   <div id="alf-ft">
      <@region id="footer" scope="global" protected=true />
   </div>
</@>
