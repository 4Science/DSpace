--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Update the `text_value` column in the `metadatavalue` table
-- to remove the trailing "::UUID" pattern if the UUID exists in the `dspaceobject` table.
UPDATE metadatavalue mv
SET text_value = REGEXP_REPLACE(mv.text_value, '::([a-f0-9-]{36})$', '') -- Remove the trailing "::UUID" if present
    FROM metadatafieldregistry mf
JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
WHERE mv.metadata_field_id = mf.metadata_field_id
-- Ensure the `text_value` contains "::UUID" at the end
  AND mv.text_value ~ '::[a-f0-9-]{36}$'
-- Check if the extracted UUID exists in `dspaceobject`
  AND EXISTS (
    SELECT 1 FROM dspaceobject d
    WHERE d.uuid = CAST(SUBSTRING(mv.text_value FROM '::([a-f0-9-]{36})$') AS UUID)
    )
-- Apply the update only for virtual metadata fields in the "cris" schema
  AND mf.element = 'virtual'
  AND ms.short_id = 'cris';
