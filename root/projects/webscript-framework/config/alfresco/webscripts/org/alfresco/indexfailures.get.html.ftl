<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head> 
    <title>Index of Failed Web Scripts</title> 
    <link rel="stylesheet" href="${url.context}/css/base.css" TYPE="text/css">
  </head>
  <body>
   <table>
     <tr>
        <td><img src="${url.context}/images/logo/AlfrescoLogo32.png" alt="Alfresco" /></td>
        <td><nobr><span class="title">Index of Failed Web Scripts</span></nobr></td>
     </tr>
     <tr><td><td>${failures?size} Failed Web Scripts
    </table>
    <br>
    <table>
      <tr><td><a href="${url.serviceContext}/index">Back to Web Scripts Home</a>
    </table>
    <br>
    <table>
      <tr><td>Path</td><td>Failure</td></tr>
      <#list failures?keys as path>
        <tr><td>${path}</td><td>${failures[path]}</td></tr>
      </#list>
    </table>
  </body>
</html>