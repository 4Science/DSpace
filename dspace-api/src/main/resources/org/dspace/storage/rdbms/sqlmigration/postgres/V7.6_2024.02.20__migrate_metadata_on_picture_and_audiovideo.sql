--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Migrate old metadata fields
-----------------------------------------------------------------------------------
DO $$
DECLARE
    old_metadata_fields text[] := ARRAY [
        'dc.subject', 'dc.subject.keywords'
        ];
    new_metadata_fields text[] := ARRAY [
        'glam.subject', 'dc.subject'
        ];
    old_field_id        integer;
    new_field_id        integer;
    old_schema_id       integer;
    new_schema_id       integer;
    new_short_id        text;
    old_element         text;
    new_element         text;
    old_qualifier       text;
    new_qualifier       text;
    updated_rows        integer;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM metadataschemaregistry
        WHERE short_id = 'dc'
    ) AND EXISTS (
        SELECT 1
        FROM metadataschemaregistry
        WHERE short_id = 'glam'
    ) THEN
        -- Iterate through each old metadata field
        FOR i IN 1..array_length(old_metadata_fields, 1)
        LOOP

            old_element = CASE WHEN split_part(old_metadata_fields[i], '.', 2) = '' THEN null ELSE split_part(old_metadata_fields[i], '.', 2) END;
            new_element = CASE WHEN split_part(new_metadata_fields[i], '.', 2) = '' THEN null ELSE split_part(new_metadata_fields[i], '.', 2) END;

            old_qualifier = CASE WHEN split_part(old_metadata_fields[i], '.', 3) = '' THEN null ELSE split_part(old_metadata_fields[i], '.', 3) END;
            new_qualifier = CASE WHEN split_part(new_metadata_fields[i], '.', 3) = '' THEN null ELSE split_part(new_metadata_fields[i], '.', 3) END;

            -- Get old schema id
            SELECT metadata_schema_id INTO old_schema_id
            FROM metadataschemaregistry
            WHERE short_id = split_part(old_metadata_fields[i], '.', 1);

            -- Get new schema id
            SELECT metadata_schema_id, short_id INTO new_schema_id, new_short_id
            FROM metadataschemaregistry
            WHERE short_id = split_part(new_metadata_fields[i], '.', 1);

            -- Add metadata field to registry (if missing)
            INSERT INTO metadatafieldregistry (metadata_schema_id, element, qualifier)
            SELECT (SELECT metadata_schema_id FROM metadataschemaregistry
                                              WHERE short_id = new_short_id), new_element, new_qualifier
            WHERE NOT EXISTS
                      (SELECT metadata_field_id,element FROM metadatafieldregistry
                       WHERE metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id = new_short_id)
                         AND element = new_element AND CASE WHEN new_qualifier IS NULL THEN new_qualifier IS NULL ELSE new_qualifier = '' END);

            -- Get the IDs of both old and new metadata fields
            SELECT
                mf1.metadata_field_id,
                mf2.metadata_field_id
            INTO
                old_field_id,
                new_field_id
            FROM
                metadatafieldregistry mf1
                    JOIN
                metadataschemaregistry ms ON mf1.metadata_schema_id = ms.metadata_schema_id
                    LEFT JOIN
                metadatafieldregistry mf2 ON mf2.metadata_schema_id = new_schema_id
            WHERE
                    ms.metadata_schema_id = old_schema_id
              AND mf1.element = old_element
              AND ((old_qualifier is NULL AND mf1.qualifier is NULL) OR ((old_qualifier <> '' OR old_qualifier IS NOT NULL) AND mf1.qualifier =  old_qualifier))
              AND mf2.element = new_element
              AND ((new_qualifier is NULL AND mf2.qualifier is NULL) OR ((new_qualifier <> '' OR new_qualifier IS NOT NULL) AND mf2.qualifier =  new_qualifier));

            -- If both old and new fields exist, update the metadatavalue table
            -- in case of Picture or AudioVideo items
            IF old_field_id IS NOT NULL AND new_field_id IS NOT NULL THEN
                UPDATE metadatavalue
                SET metadata_field_id = new_field_id
                WHERE metadata_field_id = old_field_id
                    -- custom filter to check if the item is Picture or AudioVideo
                    AND dspace_object_id in (select distinct mv.dspace_object_id
                        from metadatavalue mv
                        inner join metadatafieldregistry mfr on mv.metadata_field_id = mfr.metadata_field_id
                        inner join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id
                        where msr.short_id = 'dspace'
                            and mfr.element = 'entity'
                            and mfr.qualifier = 'type'
                            and (mv.text_value = 'Picture' or mv.text_value = 'AudioVideo'));

                RAISE INFO 'Replacing Old Metadata: % with %', old_metadata_fields[i], new_metadata_fields[i];

                GET DIAGNOSTICS updated_rows = ROW_COUNT;
                RAISE INFO 'Updated % rows.', updated_rows;
                RAISE INFO '------------------------------------------------------------------------------------------';

            END IF;
        END LOOP;
    END IF;
END $$;