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

DO
$$
DECLARE
page INT := 0;
    pagesize INT := 100;
    rows_inserted INT := 0;
BEGIN
    -- Step 0: Prepare UUID pairs
    CREATE TEMP TABLE temp_canvasid_targets (
        target_uuid UUID PRIMARY KEY,
        canvas_uuid UUID
    );

INSERT INTO temp_canvasid_targets (target_uuid, canvas_uuid)
WITH
    dc_title_field AS (
        SELECT mf.metadata_field_id
        FROM metadatafieldregistry mf
                 JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
        WHERE ms.short_id = 'dc'
          AND mf.element = 'title'
          AND mf.qualifier IS NULL
    LIMIT 1
    ),
    bundles_with_name AS (
SELECT b.uuid AS bundle_uuid, mv.text_value AS title
FROM bundle b
    JOIN metadatavalue mv ON mv.dspace_object_id = b.uuid
    JOIN dc_title_field dtf ON mv.metadata_field_id = dtf.metadata_field_id
WHERE mv.text_value LIKE 'IIIF-PDF-%'
    ),
    primary_bitstreams AS (
SELECT DISTINCT ON (b2b.bundle_id)
    b2b.bundle_id,
    bs.uuid AS canvas_uuid
FROM bundle2bitstream b2b
    JOIN bitstream bs ON bs.uuid = b2b.bitstream_id
ORDER BY b2b.bundle_id, b2b.bitstream_order
    ),
    joined AS (
SELECT
    REPLACE(b.title, 'IIIF-PDF-', '')::uuid AS target_uuid,
    pb.canvas_uuid
FROM bundles_with_name b
    JOIN primary_bitstreams pb ON pb.bundle_id = b.bundle_uuid
    ),
    iiif_field_id AS (
SELECT mf.metadata_field_id
FROM metadatafieldregistry mf
    JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
WHERE ms.short_id = 'bitstream'
  AND mf.element = 'iiif'
  AND mf.qualifier = 'canvasid'
    LIMIT 1
    )
SELECT
    j.target_uuid,
    j.canvas_uuid
FROM joined j, iiif_field_id icf
WHERE NOT EXISTS (
    SELECT 1 FROM metadatavalue mv
    WHERE mv.dspace_object_id = j.target_uuid
      AND mv.metadata_field_id = icf.metadata_field_id
);

-- Step 1: Paginate insert
LOOP
WITH iiif_canvasid_field AS (
            SELECT mf.metadata_field_id
            FROM metadatafieldregistry mf
            JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
            WHERE ms.short_id = 'bitstream'
              AND mf.element = 'iiif'
              AND mf.qualifier = 'canvasid'
            LIMIT 1
        ),
        to_insert AS (
            SELECT target_uuid, canvas_uuid
            FROM temp_canvasid_targets
            ORDER BY target_uuid
            LIMIT pagesize OFFSET page * pagesize
        ),
        inserted AS (
            INSERT INTO metadatavalue (
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
                icf.metadata_field_id,
                t.canvas_uuid::text,
                0,
                NULL,
                -1,
                t.target_uuid,
                NULL,
                NULL
            FROM to_insert t
            JOIN iiif_canvasid_field icf ON TRUE
            RETURNING 1
        )
SELECT COUNT(*) INTO rows_inserted FROM inserted;

RAISE NOTICE 'Inserted % rows on page %', rows_inserted, page;

        EXIT WHEN rows_inserted = 0;

        page := page + 1;
END LOOP;

    -- Cleanup
DROP TABLE temp_canvasid_targets;
END
$$;



-- Set the bitstream.iiif.canvasid on ORIGINAL RAW bitstreams if missing

DO
$$
DECLARE
page INT := 0;
    pagesize INT := 100;
    rows_inserted INT := 0;
BEGIN

LOOP
WITH
        canvasid_field AS (
            SELECT mf.metadata_field_id
            FROM metadatafieldregistry mf
            JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
            WHERE ms.short_id = 'bitstream'
              AND mf.element = 'iiif'
              AND mf.qualifier = 'canvasid'
            LIMIT 1
        ),
        master_field AS (
            SELECT mf.metadata_field_id
            FROM metadatafieldregistry mf
            JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
            WHERE ms.short_id = 'bitstream'
              AND mf.element = 'master'
              AND mf.qualifier IS NULL
            LIMIT 1
        ),
        dc_title_field AS (
            SELECT mf.metadata_field_id
            FROM metadatafieldregistry mf
            JOIN metadataschemaregistry ms ON mf.metadata_schema_id = ms.metadata_schema_id
            WHERE ms.short_id = 'dc'
              AND mf.element = 'title'
              AND mf.qualifier IS NULL
            LIMIT 1
        ),
        -- Bundles named 'IIIF-RAW-ACCESS'
        iiif_raw_access_bundles AS (
            SELECT b.uuid AS bundle_uuid
            FROM bundle b
            JOIN metadatavalue mv ON mv.dspace_object_id = b.uuid
            JOIN dc_title_field dtf ON mv.metadata_field_id = dtf.metadata_field_id
            WHERE mv.text_value = 'IIIF-RAW-ACCESS'
        ),
        -- Access bitstreams with a bitstream.master metadata (pointing to original)
        access_with_master AS (
            SELECT bs.uuid AS access_uuid, mv.text_value AS original_uuid
            FROM iiif_raw_access_bundles bndl
            JOIN bundle2bitstream b2b ON b2b.bundle_id = bndl.bundle_uuid
            JOIN bitstream bs ON bs.uuid = b2b.bitstream_id
            JOIN metadatavalue mv ON mv.dspace_object_id = bs.uuid
            JOIN master_field mf ON mv.metadata_field_id = mf.metadata_field_id
        ),
        -- Filter only original bitstreams that don't already have iiif.canvasid
       originals_to_update AS (
            SELECT DISTINCT
                awm.original_uuid::uuid AS original_uuid,
                awm.access_uuid
            FROM access_with_master awm
            JOIN canvasid_field cf ON TRUE
            WHERE NOT EXISTS (
                SELECT 1 FROM metadatavalue mv
                WHERE mv.dspace_object_id = awm.original_uuid::uuid
                  AND mv.metadata_field_id = cf.metadata_field_id
            )
            ORDER BY awm.original_uuid::uuid
            LIMIT pagesize OFFSET (page * pagesize)
        ),
        inserted AS (
            INSERT INTO metadatavalue (
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
                cf.metadata_field_id,
                o2u.access_uuid::text,
                0,
                NULL,
                -1,
                o2u.original_uuid,
                NULL,
                NULL
            FROM originals_to_update o2u
            JOIN canvasid_field cf ON TRUE
            RETURNING 1
        )
SELECT COUNT(*) INTO rows_inserted FROM inserted;

RAISE NOTICE 'Inserted % rows on page %', rows_inserted, page;

        EXIT WHEN rows_inserted = 0;
        page := page + 1;
END LOOP;
END
$$;

