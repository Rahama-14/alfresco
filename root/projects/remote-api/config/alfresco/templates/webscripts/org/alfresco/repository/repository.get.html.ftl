<#import "/org/alfresco/webscripts.lib.html.ftl" as wsLib/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <@wsLib.head>Alfresco Draft CMIS Implementation v${cmisVersion}</@wsLib.head>
  <body>
    <div>
    <@wsLib.header>Alfresco Draft CMIS Implementation v${cmisVersion}</@wsLib.header>
    <br/>
    <span class="mainSubTitle">Introduction</span>
    <table>
        <tr>
           <td>The Content Management Interoperability Services (CMIS) specification defines a domain
               model and set of API bindings, such as Web Services and REST/Atom that can be used
               by applications to work with one or more Content Management repositories/systems.</td>
        </tr>
        <tr><td><ul><li><a href="http://wiki.alfresco.com/wiki/CMIS">More Info...</a></li></ul></td></tr>
    </table>
    <span class="mainSubTitle">REST API Binding</span>
    <table>
        <tr><td>Alfresco's Content Repository provides the CMIS REST v${cmisVersion} binding, an extension of the Atom Publishing Protocol. 
                All <a href="${url.serviceContext}/index/family/CMIS">CMIS REST services</a> are implemented as <a href="http://wiki.alfresco.com/wiki/Web Scripts">Alfresco Web Scripts</a>, 
                therefore support all Web Script capabilities such as authentication, content negotiation, tunelling etc.</td>
        </tr>
        <tr><td><ul><li><a href="${url.serviceContext}/index/family/CMIS">CMIS REST API Reference</a></li></ul></td></tr>
    </table>
    <span class="mainSubTitle">REST API Binding Test Harness</span>
    <table>
        <tr><td>The following test harness exercises the CMIS REST API binding.  By default, its parameters are setup to this
                repository, although any CMIS REST provider may be tested.</td></tr>
        <form action="${url.serviceContext}/api/cmis/test" method="post">
        <tr><td></td></tr>
        <tr><td>Service URI: <input name="url" size="50" value="http://localhost:8080/alfresco/service/api/cmis"></td></tr>
        <tr><td>Username/Password: <input name="user" value="">  ** enter in the form of username/password</td></tr>
        <tr><td>Validate Responses: <input type="checkbox" name="validate" value="true" checked="checked"></td></tr>
        <tr><td>Trace Request/Responses: <input type="checkbox" name="trace" value="true"></td></tr>
        <tr><td>Tests (use * for wildcard in name): <input name="tests" value="*"></td></tr>
        <tr><td>** available tests: <#list tests as test>${test}<#if test_has_next>, </#if></#list></td></tr>
        <tr><td><input type="submit" value="Test"></td></tr>
        </form>
    </table>    
    <br>
    <span class="mainSubTitle">Web Services API Binding</span>
    <table>
        <tr><td>Alfresco's Content Repository provides the CMIS Web Services v${cmisVersion} binding as defined by the following
                <a href="${url.serviceContext}/alfresco/cmis">WSDL</a>.</td></tr>
        <tr><td><ul><li><a href="${url.context}/cmis">CMIS Web Service WSDL</a></li></ul></td></tr>
    </table>
    <span class="mainSubTitle">Alfresco Repository Information</span>
    <table>
        <tr><td>(also available as an <a href="${url.serviceContext}/api/repository">CMIS/APP Service Document</a>)</td></tr>
    </table>
    <br>
    <table>
        <tr><td>Repository Id:</td><td>${server.id}</td></tr>
        <tr><td>Repository Name:</td><td>${server.name}</td></tr>
        <tr><td>Repository Relationship:</td><td>Self</td></tr>
        <tr><td>Repository Description:</td><td>[none]</td></tr>
        <tr><td>Vendor Name:</td><td>Alfresco</td></tr>
        <tr><td>Product Name:</td><td>Alfresco Repository (${server.edition})</td></tr>
        <tr><td>Product Version:</td><td>${server.version}</td></tr>
        <tr><td>Root Folder Id:</td><td>${absurl(url.serviceContext)}/api/path/${encodeuri(defaultRootFolderPath)}/children</td></tr> 
        <tr><td>Multifiling:</td><td>true</td></tr>
        <tr><td>Unfiling:</td><td>false</td></tr>
        <tr><td>VersionSpecificFiling:</td><td>false</td></tr>        
        <tr><td>PWCUpdateable:</td><td>true</td></tr>
        <tr><td>PWCSearchable:</td><td>${pwcSearchable?string}</td></tr>
        <tr><td>AllVersionsSearchable:</td><td>${allVersionsSearchable?string}</td></tr>
        <tr><td>Query:</td><td>${querySupport}</td></tr>
        <tr><td>Join:</td><td>${joinSupport}</td></tr>
        <tr><td>VersionSupported:</td><td>${cmisVersion}</td></tr>
        <tr><td>repositorySpecificInformation:</td><td>[none]</td></tr>
    </table>
    </div>
  </body>    
</html>