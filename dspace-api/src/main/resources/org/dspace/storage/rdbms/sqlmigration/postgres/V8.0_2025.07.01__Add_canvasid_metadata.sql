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
            pagesize
    INT := 100;
            rows_inserted
    INT := 0;
    BEGIN
        -- Step 0: Prepare UUID pairs
        CREATE TEMP TABLE temp_canvasid_targets
        (
            target_uuid UUID PRIMARY KEY,
            canvas_uuid UUID
        );

        INSERT INTO TEMP_CANVASID_TARGETS (TARGET_UUID, CANVAS_UUID)
        WITH
        -- retrieves dc.title
            DC_TITLE_FIELD AS (SELECT MF.METADATA_FIELD_ID
                               FROM METADATAFIELDREGISTRY MF
                                        JOIN METADATASCHEMAREGISTRY MS ON MF.METADATA_SCHEMA_ID = MS.METADATA_SCHEMA_ID
                               WHERE MS.SHORT_ID = 'dc'
                                 AND MF.ELEMENT = 'title'
                                 AND MF.QUALIFIER IS NULL
                                LIMIT 1)
                                ,
        -- retrieves bundle with IIIF-PDF in name
        BUNDLES_WITH_NAME AS (SELECT DISTINCT ON (MV.TEXT_VALUE) B.UUID AS BUNDLE_UUID,
                                                                 MV.TEXT_VALUE AS TITLE
                                FROM BUNDLE B
                                    JOIN METADATAVALUE MV ON MV.DSPACE_OBJECT_ID = B.UUID
                                    JOIN DC_TITLE_FIELD DTF ON MV.METADATA_FIELD_ID = DTF.METADATA_FIELD_ID
                                WHERE MV.TEXT_VALUE LIKE 'IIIF-PDF-%'
                                ORDER BY MV.TEXT_VALUE,
                                    MV.METADATA_VALUE_ID DESC) -- ensures we select the latest created bundle
                ,
        -- groups each bundle with the related primary bitstream ( the one with the lowest BITSREAM_ORDER value )
        -- due to the DISTINCT ON (BUNDLE_ID)
        PRIMARY_BITSTREAMS AS (SELECT DISTINCT
            ON (B2B.BUNDLE_ID) B2B.BUNDLE_ID,
                               BS.UUID AS CANVAS_UUID,
                               B2B.BITSTREAM_ORDER
                                FROM BUNDLE2BITSTREAM B2B
                                    JOIN BITSTREAM BS
                                        ON BS.UUID = B2B.BITSTREAM_ID
                                ORDER BY B2B.BUNDLE_ID,
                                         B2B.BITSTREAM_ORDER),
        -- retrieves all the bitstreams conained in the IIIF-PDF-{uuid} bundle name from the PRIMARY_BITSTREAMS table
        JOINED AS (SELECT DISTINCT
                    ON (REPLACE(B.TITLE, 'IIIF-PDF-', '')::UUID) REPLACE(B.TITLE, 'IIIF-PDF-', '')::UUID AS TARGET_UUID,
                        PB.CANVAS_UUID
                    FROM BUNDLES_WITH_NAME B
                        JOIN PRIMARY_BITSTREAMS PB
                            ON PB.BUNDLE_ID = B.BUNDLE_UUID
                    ORDER BY REPLACE(B.TITLE, 'IIIF-PDF-', '')::UUID),
        -- retrieves the metadata that we are going to fill out canvasid
        IIIF_FIELD_ID AS (SELECT MF.METADATA_FIELD_ID
                            FROM METADATAFIELDREGISTRY MF
                                JOIN METADATASCHEMAREGISTRY MS
                                    ON MF.METADATA_SCHEMA_ID = MS.METADATA_SCHEMA_ID
                            WHERE MS.SHORT_ID = 'bitstream'
                                AND MF.ELEMENT = 'iiif'
                                AND MF.QUALIFIER = 'canvasid'
                            LIMIT 1)
        -- finally retrieves all the bitstreams that are inside the IIIF-PDF-{uuid} bundle
        -- and all the primary bitstreams related to that specific bundle, without any canvasid metadata set.
        SELECT J.TARGET_UUID,
               J.CANVAS_UUID
        FROM JOINED J,
             IIIF_FIELD_ID ICF
        WHERE NOT EXISTS (SELECT 1
                          FROM METADATAVALUE MV
                          WHERE MV.DSPACE_OBJECT_ID = J.TARGET_UUID
                            AND MV.METADATA_FIELD_ID = ICF.METADATA_FIELD_ID);

        -- Step 1: Paginate insert
        LOOP
            WITH
                -- retrieves metadata field
                IIIF_CANVASID_FIELD AS (SELECT MF.METADATA_FIELD_ID
                                        FROM METADATAFIELDREGISTRY MF
                                                 JOIN METADATASCHEMAREGISTRY MS
                                                      ON MF.METADATA_SCHEMA_ID = MS.METADATA_SCHEMA_ID
                                        WHERE MS.SHORT_ID = 'bitstream'
                                          AND MF.ELEMENT = 'iiif'
                                          AND MF.QUALIFIER = 'canvasid'
                                        LIMIT 1),
                -- computes the bitstreams to be inserter with paginated query
                TO_INSERT AS (SELECT TARGET_UUID,
                                     CANVAS_UUID
                              FROM TEMP_CANVASID_TARGETS
                              ORDER BY TARGET_UUID
                              LIMIT PAGESIZE OFFSET PAGE * PAGESIZE),
                -- inserts metadata.
                INSERTED AS (
                    INSERT INTO
                        METADATAVALUE (
                                       METADATA_FIELD_ID,
                                       TEXT_VALUE,
                                       PLACE,
                                       AUTHORITY,
                                       CONFIDENCE,
                                       DSPACE_OBJECT_ID,
                                       TEXT_LANG,
                                       SECURITY_LEVEL
                            )
                        SELECT ICF.METADATA_FIELD_ID,
                               T.CANVAS_UUID::TEXT,
                               0,
                               NULL,
                               -1,
                               T.TARGET_UUID,
                               NULL,
                               NULL
                        FROM TO_INSERT T
                                 JOIN IIIF_CANVASID_FIELD ICF ON TRUE
                        RETURNING
                            1)
            -- counts the rows inserted
            SELECT COUNT(*)
            INTO ROWS_INSERTED
            FROM INSERTED;

            RAISE
                NOTICE 'Inserted % rows on page %',
                ROWS_INSERTED,
                PAGE;
            EXIT
                WHEN ROWS_INSERTED = 0;

            PAGE
                := PAGE + 1;

        END LOOP;

            -- Cleanup
            DROP TABLE TEMP_CANVASID_TARGETS;
    END
$$;

-- Set the bitstream.iiif.canvasid on ORIGINAL RAW bitstreams if missing
DO
$$
  DECLARE
PAGE INT := 0;
    PAGESIZE
INT := 100;
    ROWS_INSERTED
INT := 0;
BEGIN LOOP
WITH
    -- checks the canvasid from the metadatafield
    CANVASID_FIELD AS (
      SELECT
        MF.METADATA_FIELD_ID
      FROM
        METADATAFIELDREGISTRY MF
        JOIN METADATASCHEMAREGISTRY MS ON MF.METADATA_SCHEMA_ID = MS.METADATA_SCHEMA_ID
      WHERE
        MS.SHORT_ID = 'bitstream'
        AND MF.ELEMENT = 'iiif'
        AND MF.QUALIFIER = 'canvasid'
      LIMIT
        1
    ),
    -- loads the master field metadata
    MASTER_FIELD AS (
      SELECT
        MF.METADATA_FIELD_ID
      FROM
        METADATAFIELDREGISTRY MF
        JOIN METADATASCHEMAREGISTRY MS ON MF.METADATA_SCHEMA_ID = MS.METADATA_SCHEMA_ID
      WHERE
        MS.SHORT_ID = 'bitstream'
        AND MF.ELEMENT = 'master'
        AND MF.QUALIFIER IS NULL
      LIMIT
        1
    ),
    -- loads the dc title metadata
    DC_TITLE_FIELD AS (
      SELECT
        MF.METADATA_FIELD_ID
      FROM
        METADATAFIELDREGISTRY MF
        JOIN METADATASCHEMAREGISTRY MS ON MF.METADATA_SCHEMA_ID = MS.METADATA_SCHEMA_ID
      WHERE
        MS.SHORT_ID = 'dc'
        AND MF.ELEMENT = 'title'
        AND MF.QUALIFIER IS NULL
      LIMIT
        1
    ),
    -- Loads all the bundles named 'IIIF-RAW-ACCESS'
    IIIF_RAW_ACCESS_BUNDLES AS (
      SELECT
        B.UUID AS BUNDLE_UUID
      FROM
        BUNDLE B
        JOIN METADATAVALUE MV ON MV.DSPACE_OBJECT_ID = B.UUID
        JOIN DC_TITLE_FIELD DTF ON MV.METADATA_FIELD_ID = DTF.METADATA_FIELD_ID
      WHERE
        MV.TEXT_VALUE = 'IIIF-RAW-ACCESS'
    ),
    -- Access bitstreams with a bitstream.master metadata (pointing to original)
    ACCESS_WITH_MASTER AS (
      SELECT
        BS.UUID AS ACCESS_UUID,
        MV.TEXT_VALUE AS ORIGINAL_UUID
      FROM
        IIIF_RAW_ACCESS_BUNDLES BNDL
        JOIN BUNDLE2BITSTREAM B2B ON B2B.BUNDLE_ID = BNDL.BUNDLE_UUID
        JOIN BITSTREAM BS ON BS.UUID = B2B.BITSTREAM_ID
        JOIN METADATAVALUE MV ON MV.DSPACE_OBJECT_ID = BS.UUID
        JOIN MASTER_FIELD MF ON MV.METADATA_FIELD_ID = MF.METADATA_FIELD_ID
    ),
    -- Filters only original bitstreams that don't already have iiif.canvasid
    ORIGINALS_TO_UPDATE AS (
      SELECT DISTINCT
        AWM.ORIGINAL_UUID::UUID AS ORIGINAL_UUID,
        AWM.ACCESS_UUID
      FROM
        ACCESS_WITH_MASTER AWM
        JOIN CANVASID_FIELD CF ON TRUE
      WHERE
        NOT EXISTS (
          SELECT
            1
          FROM
            METADATAVALUE MV
          WHERE
            MV.DSPACE_OBJECT_ID = AWM.ORIGINAL_UUID::UUID
            AND MV.METADATA_FIELD_ID = CF.METADATA_FIELD_ID
        )
      ORDER BY
        AWM.ORIGINAL_UUID::UUID
      LIMIT
        PAGESIZE
      OFFSET
        (PAGE * PAGESIZE)
    ),
    INSERTED AS (
      INSERT INTO
        METADATAVALUE (
          METADATA_FIELD_ID,
          TEXT_VALUE,
          PLACE,
          AUTHORITY,
          CONFIDENCE,
          DSPACE_OBJECT_ID,
          TEXT_LANG,
          SECURITY_LEVEL
        )
      SELECT
        CF.METADATA_FIELD_ID,
        O2U.ACCESS_UUID::TEXT,
        0,
        NULL,
        -1,
        O2U.ORIGINAL_UUID,
        NULL,
        NULL
      FROM
        ORIGINALS_TO_UPDATE O2U
        JOIN CANVASID_FIELD CF ON TRUE
      RETURNING 1
    )
SELECT COUNT(*)
INTO rows_inserted
FROM inserted;

RAISE
NOTICE 'Inserted % rows on page %', rows_inserted, page;

            EXIT
WHEN rows_inserted = 0;
            page
:= page + 1;
END LOOP;
END
$$;

