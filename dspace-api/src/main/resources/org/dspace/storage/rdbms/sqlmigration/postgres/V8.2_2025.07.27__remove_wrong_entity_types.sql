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

DO
$$
    DECLARE
        metadata_field_id_variable INTEGER;
    BEGIN
        --         Create temporary table to store id and label of old entity types
        CREATE TEMPORARY TABLE IF NOT EXISTS old_entity_types AS
        SELECT id, label FROM entity_type WHERE label IN ('Fond', 'JournalFond', 'JournalIssue', 'ArchivalMaterials');

--         Delete relationship types that has reference to old entity types
        DELETE
        FROM relationship_type rt
        WHERE rt.left_type in (SELECT id FROM old_entity_types) OR rt.right_type in (SELECT id FROM old_entity_types);

        SELECT metadata_field_id
        INTO metadata_field_id_variable
        FROM metadatafieldregistry
        WHERE element = 'entity' AND qualifier = 'type'
          AND metadata_schema_id =
              (SELECT metadataschemaregistry.metadata_schema_id FROM metadataschemaregistry WHERE short_id = 'dspace');

--         Update old text_value field with new for metadata dspace.entity.type
        UPDATE metadatavalue
        SET text_value = 'Fonds'
        WHERE text_value = 'Fond' AND metadata_field_id = metadata_field_id_variable;

        UPDATE metadatavalue
        SET text_value = 'JournalFonds'
        WHERE text_value = 'JournalFond' AND metadata_field_id = metadata_field_id_variable;

        UPDATE metadatavalue
        SET text_value = 'JournalFile'
        WHERE text_value = 'JournalIssue' AND metadata_field_id = metadata_field_id_variable;

        UPDATE metadatavalue
        SET text_value = 'ArchivalMaterial'
        WHERE text_value = 'ArchivalMaterials' AND metadata_field_id = metadata_field_id_variable;

--         Delete old entity types from entity_type table
        DELETE
        FROM entity_type
        WHERE label IN (SELECT label FROM old_entity_types);

    END;
$$;
