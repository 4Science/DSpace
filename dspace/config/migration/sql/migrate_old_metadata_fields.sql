DO $$
DECLARE
    old_metadata_fields text[] := ARRAY[
    'dc.contributor.type', 'dc.contributor.affiliation', 'dc.contributor.orcid', 'rd.contributor.primarycontact',
    'rd.contributor.primarycontactaffiliation', 'dc.date.embargoend', 'dc.alternateurl.url', 'rd.relation.project',
    'rd.relation.funding', 'dc.relation.externaltype', 'dc.relation.externaldoi', 'dc.relation.externalurl',
    'dc.relation.externalurn', 'dc.relation.publication', 'rd.relation.publication'
    ];

    new_metadata_fields text[] := ARRAY[
    'unibe.contributor.role', 'oairecerif.author.affiliation', 'unibe.contributor.orcid',
    'unibe.contributor.primarycontact', 'unibe.primarycontact.affiliation',
    'unibe.date.embargoend', 'dc.relation.url', 'dc.relation.project',
    'dc.relation.funding', 'rd.relation.externaltype', 'dc.coverage.doi',
    'dc.coverage.url', 'dc.coverage.urn', 'rd.relation.publication', 'dc.relation.publication'
    ];

    old_field_id integer;
    new_field_id integer;
    old_schema_id integer;
    new_schema_id integer;
    old_element text;
    new_element text;
    old_qualifier text;
    new_qualifier text;
    updated_rows integer;
BEGIN
    -- Iterate through each old metadata field
    FOR i IN 1..array_length(old_metadata_fields, 1)
    LOOP

        old_element = split_part(old_metadata_fields[i], '.', 2);
        new_element = split_part(new_metadata_fields[i], '.', 2);

        old_qualifier = split_part(old_metadata_fields[i], '.', 3);
        new_qualifier = split_part(new_metadata_fields[i], '.', 3);

        -- Get old schema id
        SELECT metadata_schema_id INTO old_schema_id
        FROM metadataschemaregistry
        WHERE short_id = split_part(old_metadata_fields[i], '.', 1);

        -- Get new schema id
        SELECT metadata_schema_id INTO new_schema_id
        FROM metadataschemaregistry
        WHERE short_id = split_part(new_metadata_fields[i], '.', 1);

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
            AND ((old_qualifier = '' AND mf1.qualifier is NULL) OR (old_qualifier <> '' AND mf1.qualifier =  old_qualifier))
            AND mf2.element = new_element
            AND ((new_qualifier = '' AND mf2.qualifier is NULL) OR (new_qualifier <> '' AND mf2.qualifier =  new_qualifier));

            -- If both old and new fields exist, update the metadatavalue table
            IF old_field_id IS NOT NULL AND new_field_id IS NOT NULL THEN
                UPDATE metadatavalue
                SET metadata_field_id = new_field_id
                WHERE metadata_field_id = old_field_id;

                RAISE INFO 'Replacing Old Metadata: % with %', old_metadata_fields[i], new_metadata_fields[i];

                GET DIAGNOSTICS updated_rows = ROW_COUNT;
                RAISE INFO 'Updated % rows.', updated_rows;
                RAISE INFO '------------------------------------------------------------------------------------------';

            END IF;
    END LOOP;
END $$;
