--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- DELETE glam.index metadata for items belonging to collections with submission definition 'aggregation'
DELETE FROM metadatavalue mv
WHERE EXISTS (
    SELECT 1
    FROM metadataschemaregistry
    WHERE short_id = 'glam'
)
  AND EXISTS (
    SELECT 1
    FROM metadataschemaregistry
    WHERE short_id = 'cris'
)
  AND mv.metadata_field_id IN (
    SELECT mfr.metadata_field_id
    FROM metadatafieldregistry mfr
             INNER JOIN metadataschemaregistry msr
                        ON mfr.metadata_schema_id = msr.metadata_schema_id
    WHERE msr.short_id = 'glam'
      AND mfr.element = 'index'
      AND mfr.qualifier IS NULL
)
  AND mv.dspace_object_id IN (
    SELECT DISTINCT c2i.item_id
    FROM collection2item c2i
             INNER JOIN metadatavalue mv2 ON mv2.dspace_object_id = c2i.collection_id
             INNER JOIN metadatafieldregistry mfr ON mfr.metadata_field_id = mv2.metadata_field_id
             INNER JOIN metadataschemaregistry msr ON mfr.metadata_schema_id = msr.metadata_schema_id
    WHERE msr.short_id = 'cris'
      AND mfr.element = 'submission'
      AND mfr.qualifier = 'definition'
      AND mv2.text_value = 'aggregation'
);

-- UPDATE "dc.identifier.archivalunit" to "glamfonds.index" for items in collections with submission definition "archival_material".
UPDATE metadatavalue mv
SET metadata_field_id = (
    SELECT mfr.metadata_field_id
    FROM metadatafieldregistry mfr
             INNER JOIN metadataschemaregistry msr ON mfr.metadata_schema_id = msr.metadata_schema_id
    WHERE msr.short_id = 'glamfonds'
      AND mfr.element = 'index'
      AND mfr.qualifier IS NULL
)
WHERE EXISTS (
    SELECT 1 FROM metadataschemaregistry WHERE short_id = 'glamfonds'
)
  AND EXISTS (
    SELECT 1 FROM metadataschemaregistry WHERE short_id = 'dc'
)
  AND EXISTS (
    SELECT 1 FROM metadataschemaregistry WHERE short_id = 'cris'
)
  AND mv.metadata_field_id IN (
    SELECT mfr.metadata_field_id
    FROM metadatafieldregistry mfr
             INNER JOIN metadataschemaregistry msr ON mfr.metadata_schema_id = msr.metadata_schema_id
    WHERE msr.short_id = 'dc'
      AND mfr.element = 'identifier'
      AND mfr.qualifier = 'archivalunit'
)
  AND mv.dspace_object_id IN (
    SELECT DISTINCT c2i.item_id
    FROM collection2item c2i
             INNER JOIN metadatavalue mv2 ON mv2.dspace_object_id = c2i.collection_id
             INNER JOIN metadatafieldregistry mfr ON mfr.metadata_field_id = mv2.metadata_field_id
             INNER JOIN metadataschemaregistry msr ON mfr.metadata_schema_id = msr.metadata_schema_id
    WHERE msr.short_id = 'cris'
      AND mfr.element = 'submission'
      AND mfr.qualifier = 'definition'
      AND mv2.text_value = 'archival_material'
);