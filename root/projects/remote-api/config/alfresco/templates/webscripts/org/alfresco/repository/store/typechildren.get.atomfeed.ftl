[#ftl]
[#import "/org/alfresco/cmis/ns.lib.atom.ftl" as nsLib/]
[#import "/org/alfresco/cmis/atomfeed.lib.atom.ftl" as feedLib/]
[#import "/org/alfresco/cmis/atomentry.lib.atom.ftl" as entryLib/]
[#import "/org/alfresco/paging.lib.atom.ftl" as pagingLib/]
[#compress]

<?xml version="1.0" encoding="UTF-8"?>
<feed [@nsLib.feedNS/]>

[@feedLib.generic "urn:uuid:type-${typedef.objectTypeId}-children" "Child types of ${typedef.objectTypeId}" "${person.properties.userName}"]
  [@pagingLib.links cursor/]
[/@feedLib.generic]

[#list results as child]
[@entryLib.typedef child returnPropertyDefinitions/]
[/#list]

[@feedLib.hasMore cursor/]
[@pagingLib.opensearch cursor/]

</feed>

[/#compress]