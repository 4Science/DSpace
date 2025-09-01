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

-------------------------------------------------------------
-- This will create add bitstream.iiif.canvasid metadata
-------------------------------------------------------------


-- Set the bitstream.iiif.canvasid on ORIGINAL PDF bitstreams if missing.
-- Step 1: Create temporary table for target UUIDs and their corresponding canvas bitstreams
CREATE TEMPORARY TABLE temp_canvasid_targets (
    target_uuid UUID PRIMARY KEY,  -- UUID parsed from bundle title
    canvas_uuid UUID               -- UUID of bitstream in bundle
);

-- Step 2: Populate temp table with bitstreams that need canvasid metadata
INSERT INTO temp_canvasid_targets (target_uuid, canvas_uuid)
WITH
-- Locate metadata_field_id for dc.title
    dc_title_field AS (
        SELECT mf.metadata_field_id
        FROM metadatafieldregistry mf
                 JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
        WHERE ms.short_id = 'dc'
          AND mf.element = 'title'
          AND mf.qualifier IS NULL
    LIMIT 1
    ),

-- Locate metadata_field_id for bitstream.iiif.canvasid
    iiif_field_id AS (
SELECT mf.metadata_field_id
FROM metadatafieldregistry mf
    JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
WHERE ms.short_id = 'bitstream'
  AND mf.element = 'iiif'
  AND mf.qualifier = 'canvasid'
    LIMIT 1
    ),

-- All bundles with a dc.title starting with 'IIIF-PDF-'
    bundles_with_title AS (
SELECT b.uuid AS bundle_uuid, mv.text_value AS title
FROM bundle b
    JOIN metadatavalue mv ON mv.dspace_object_id = b.uuid
    JOIN dc_title_field dtf ON mv.metadata_field_id = dtf.metadata_field_id
WHERE mv.text_value LIKE 'IIIF-PDF-%'
    ),

-- First bitstream (bitstream_order = 0) in each bundle
    primary_bitstreams AS (
SELECT b2b.bundle_id, bs.uuid AS canvas_uuid
FROM bundle2bitstream b2b
    JOIN bitstream bs ON bs.uuid = b2b.bitstream_id
WHERE b2b.bitstream_order = 0
    ),

-- Join bundles to their primary bitstream
    joined AS (
SELECT
    CAST(REPLACE(b.title, 'IIIF-PDF-', '') AS UUID) AS target_uuid,
    pb.canvas_uuid
FROM bundles_with_title b
    JOIN primary_bitstreams pb ON pb.bundle_id = b.bundle_uuid
    )

-- Only insert entries that do NOT already have iiif.canvasid metadata
SELECT
    j.target_uuid,
    j.canvas_uuid
FROM joined j
WHERE NOT EXISTS (
    SELECT 1
    FROM metadatavalue mv
             JOIN iiif_field_id icf ON mv.metadata_field_id = icf.metadata_field_id
    WHERE mv.dspace_object_id = j.target_uuid
);

-- Step 3: Insert canvas UUIDs into target bitstreams as iiif.canvasid metadata
INSERT INTO metadatavalue (
    metadata_value_id,
    metadata_field_id,
    text_value,
    place,
    authority,
    confidence,
    dspace_object_id,
    text_lang,
    security_level
)
SELECT
    NEXTVAL('metadatavalue_seq'),
    (SELECT mf.metadata_field_id
     FROM metadatafieldregistry mf
              JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
     WHERE ms.short_id = 'bitstream'
       AND mf.element = 'iiif'
       AND mf.qualifier = 'canvasid'
        LIMIT 1),
    t.canvas_uuid::text,
    0,
    NULL,
    -1,
    t.target_uuid,
    NULL,
    NULL
FROM temp_canvasid_targets t;

-- Step 4: Cleanup
DROP TABLE temp_canvasid_targets;


-- Set the bitstream.iiif.canvasid on ORIGINAL RAW bitstreams if missing

-- Create temp table for metadata field IDs
CREATE TEMP TABLE IF NOT EXISTS temp_field_ids (
    canvasid_field_id INT,
    master_field_id INT,
    dc_title_field_id INT
);

DELETE FROM temp_field_ids;

INSERT INTO temp_field_ids (canvasid_field_id, master_field_id, dc_title_field_id)
SELECT
    (SELECT mf.metadata_field_id
     FROM metadatafieldregistry mf
              JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
     WHERE ms.short_id = 'bitstream'
       AND mf.element = 'iiif'
       AND mf.qualifier = 'canvasid'
        LIMIT 1),

    (SELECT mf.metadata_field_id
     FROM metadatafieldregistry mf
     JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
     WHERE ms.short_id = 'bitstream'
       AND mf.element = 'master'
       AND mf.qualifier IS NULL
     LIMIT 1),

    (SELECT mf.metadata_field_id
     FROM metadatafieldregistry mf
     JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
     WHERE ms.short_id = 'dc'
       AND mf.element = 'title'
       AND mf.qualifier IS NULL
     LIMIT 1);

-- Identify bitstreams to update (originals that don't have canvasid)
CREATE TEMP TABLE IF NOT EXISTS temp_canvas_inserts (
    original_uuid UUID,
    access_uuid UUID
);

DELETE FROM temp_canvas_inserts;

INSERT INTO temp_canvas_inserts (original_uuid, access_uuid)
SELECT DISTINCT
    CAST(CAST(mvm.text_value AS VARCHAR) AS UUID),
    bs.uuid
FROM bundle b
         JOIN metadatavalue mv ON mv.dspace_object_id = b.uuid
         JOIN temp_field_ids tf ON mv.metadata_field_id = tf.dc_title_field_id
         JOIN bundle2bitstream b2b ON b2b.bundle_id = b.uuid
         JOIN bitstream bs ON bs.uuid = b2b.bitstream_id
         JOIN metadatavalue mvm ON mvm.dspace_object_id = bs.uuid
WHERE mv.text_value = 'IIIF-RAW-ACCESS'
  AND mvm.metadata_field_id = tf.master_field_id
  AND NOT EXISTS (
    SELECT 1 FROM metadatavalue mv2
    WHERE mv2.dspace_object_id = CAST(CAST(mvm.text_value AS VARCHAR) AS UUID)
      AND mv2.metadata_field_id = tf.canvasid_field_id
);

-- Insert canvasid metadata into original bitstreams
INSERT INTO metadatavalue (
    metadata_value_id,
    metadata_field_id,
    text_value,
    place,
    authority,
    confidence,
    dspace_object_id,
    text_lang,
    security_level
)
SELECT
    NEXT VALUE FOR metadatavalue_seq,
    tf.canvasid_field_id,
    t.access_uuid,
    0,
    NULL,
    -1,
    t.original_uuid,
    NULL,
    NULL
FROM temp_canvas_inserts t
    JOIN temp_field_ids tf ON 1=1;

-- Cleanup
DROP TABLE temp_canvas_inserts;
DROP TABLE temp_field_ids;

