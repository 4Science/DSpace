--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

-------------------------------------------------------------------------------------------------------
-- Remove wrong entity types
-------------------------------------------------------------------------------------------------------

-- Remove wrong entity types in relationship_type, metadatavalue and entity_type tables

-- Delete relationship types that has reference to old entity types
DELETE
FROM relationship_type rt
WHERE rt.left_type in
    (SELECT id FROM entity_type WHERE label IN ('Fond', 'JournalFond', 'JournalIssue', 'ArchivalMaterials'))
   OR rt.right_type in
    (SELECT id FROM entity_type WHERE label IN ('Fond', 'JournalFond', 'JournalIssue', 'ArchivalMaterials'));

-- Update old text_value field with new for metadata dspace.entity.type
UPDATE metadatavalue
SET text_value = 'JournalFonds'
WHERE text_value = 'JournalFond'
  AND metadata_field_id IN (SELECT metadata_field_id
                            FROM metadatafieldregistry mfr
                                     LEFT JOIN metadataschemaregistry msr
                                               ON mfr.metadata_schema_id = msr.metadata_schema_id
                            WHERE msr.short_id = 'dspace'
                              AND mfr.element = 'entity'
                              AND mfr.qualifier = 'type');

UPDATE metadatavalue
SET text_value = 'Fonds'
WHERE text_value = 'Fond'
  AND metadata_field_id IN (SELECT metadata_field_id
                            FROM metadatafieldregistry mfr
                                     LEFT JOIN metadataschemaregistry msr
                                               ON mfr.metadata_schema_id = msr.metadata_schema_id
                            WHERE msr.short_id = 'dspace'
                              AND mfr.element = 'entity'
                              AND mfr.qualifier = 'type');

UPDATE metadatavalue
SET text_value = 'JournalFile'
WHERE text_value = 'JournalIssue'
  AND metadata_field_id IN (SELECT metadata_field_id
                            FROM metadatafieldregistry mfr
                                     LEFT JOIN metadataschemaregistry msr
                                               ON mfr.metadata_schema_id = msr.metadata_schema_id
                            WHERE msr.short_id = 'dspace'
                              AND mfr.element = 'entity'
                              AND mfr.qualifier = 'type');

UPDATE metadatavalue
SET text_value = 'ArchivalMaterial'
WHERE text_value = 'ArchivalMaterials'
  AND metadata_field_id IN (SELECT metadata_field_id
                            FROM metadatafieldregistry mfr
                                     LEFT JOIN metadataschemaregistry msr
                                               ON mfr.metadata_schema_id = msr.metadata_schema_id
                            WHERE msr.short_id = 'dspace'
                              AND mfr.element = 'entity'
                              AND mfr.qualifier = 'type');

-- Delete old entity types from entity_type table
DELETE
FROM entity_type
WHERE label IN
      (SELECT label FROM entity_type WHERE label IN ('Fond', 'JournalFond', 'JournalIssue', 'ArchivalMaterials'))
