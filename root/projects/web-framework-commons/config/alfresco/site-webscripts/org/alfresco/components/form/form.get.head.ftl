<#include "../component.head.inc">
<!-- Form Assets -->
<link rel="stylesheet" type="text/css" href="${page.url.context}/yui/calendar/assets/calendar.css" />
<@link rel="stylesheet" type="text/css" href="${page.url.context}/components/object-finder/object-finder.css" />
<@link rel="stylesheet" type="text/css" href="${page.url.context}/components/form/form.css" />

<#if config.global.forms?exists && config.global.forms.dependencies?exists && config.global.forms.dependencies.css?exists>
<#list config.global.forms.dependencies.css as cssFile>
<link rel="stylesheet" type="text/css" href="${page.url.context}${cssFile}" />
</#list>
</#if>

<@script type="text/javascript" src="${page.url.context}/components/form/form.js"></@script>
<@script type="text/javascript" src="${page.url.context}/components/form/date.js"></@script>
<@script type="text/javascript" src="${page.url.context}/components/form/period.js"></@script>
<@script type="text/javascript" src="${page.url.context}/components/object-finder/object-finder.js"></@script>
<script type="text/javascript" src="${page.url.context}/yui/calendar/calendar-min.js"></script>

<#if config.global.forms?exists && config.global.forms.dependencies?exists && config.global.forms.dependencies.js?exists>
<#list config.global.forms.dependencies.js as jsFile>
<script type="text/javascript" src="${page.url.context}${jsFile}"></script>
</#list>
</#if>