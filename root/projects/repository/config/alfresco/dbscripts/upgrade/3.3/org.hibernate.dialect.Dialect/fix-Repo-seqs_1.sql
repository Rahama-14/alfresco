--
-- Title:      Upgrade to V3.3 - Create repo sequences
-- Database:   Generic
-- Since:      V3.3 schema 4008
-- Author:     unknown
--
-- creates sequences for repo tables
--
-- Please contact support@alfresco.com if you need assistance with the upgrade.
--


--
-- Record script finish
--
DELETE FROM alf_applied_patch WHERE id = 'patch.db-V3.3-Fix-Repo-Seqs_1';
INSERT INTO alf_applied_patch
  (id, description, fixes_from_schema, fixes_to_schema, applied_to_schema, target_schema, applied_on_date, applied_to_server, was_executed, succeeded, report)
  VALUES
  (
    'patch.db-V3.3-Fix-Repo-Seqs_1', 'Manually executed script upgrade V3.3 to create repo sequences',
     0, 4007, -1, 4008, null, 'UNKOWN', ${TRUE}, ${TRUE}, 'Script completed'
   );
