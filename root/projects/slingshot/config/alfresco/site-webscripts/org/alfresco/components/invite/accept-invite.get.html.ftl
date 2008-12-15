<div class="page-title">
   <div class="title">
      <h1><span>${msg("header.title")}</span></h1>
   </div>
</div>

<div class="accept-invite-body">
<#if (!doRedirect)>
   <h1>${msg("error.acceptfailed.title")}</h1>
   <p>${msg("error.acceptfailed.text")}</p>
<#else>
<script type="text/javascript">//<![CDATA[
   window.location = "${page.url.context}/page/site/${siteShortName}/dashboard";
//]]></script>
   <h1>${msg("acceptregistered.title")}</h1>
   <p>${msg("acceptregistered.text")}</p>
   <br />
   <a href="${page.url.context}/page/site/${siteShortName}/dashboard">${page.url.context}/page/site/${siteShortName}/dashboard</a>
</#if>
</div>
