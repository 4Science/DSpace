--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- update glam.mis* in scientific-material form
-----------------------------------------------------------------------------------
DO $$
DECLARE
    old_elements text[] := ARRAY [
        'misn', 'misp', 'misd', 'miss', 'misg', 'misc', 'misw', 'misb', 'misi'
        ];

    old_schema_id       integer;
    new_schema_id       integer;
    old_element         text;
    new_element         text := 'format';
    new_qualifier       text;
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
        -- Iterate through each old element
        FOR i IN 1..array_length(old_elements, 1)
        LOOP

            old_element = old_elements[i];
            new_qualifier = old_elements[i];

            -- Get old schema id
            SELECT metadata_schema_id INTO old_schema_id
            FROM metadataschemaregistry
            WHERE short_id = 'glam';

            -- Get new schema id
            SELECT metadata_schema_id INTO new_schema_id
            FROM metadataschemaregistry
            WHERE short_id = 'dc';

            -- update metadata field registry
            UPDATE metadatafieldregistry
            SET metadata_schema_id = new_schema_id,
            element = new_element,
            qualifier = new_qualifier
            WHERE metadata_schema_id = old_schema_id
            AND element = old_element AND (qualifier = '' OR qualifier IS NULL);

        END LOOP;
    END IF;
END $$;