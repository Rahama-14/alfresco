<import resource="classpath:alfresco/site-webscripts/org/alfresco/components/console/rm-console.lib.js">

function main()
{
   var conn = remote.connect("alfresco");
   
   // test user capabilities - can they access Email Mappings?
   model.hasAccess = hasCapability(conn, "MapEmailMetadata");
}

main();