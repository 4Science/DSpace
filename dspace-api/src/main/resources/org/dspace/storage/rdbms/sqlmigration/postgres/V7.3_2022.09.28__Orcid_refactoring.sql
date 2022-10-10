--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- DELETE ORCID boxes
DELETE FROM cris_layout_box box where box.type in ('ORCID_SYNC_SETTINGS', 'ORCID_AUTHORIZATIONS', 'ORCID_SYNC_QUEUE');

-- DELETE empty cells
DELETE FROM cris_layout_cell cell 
where NOT EXISTS (SELECT 1 from cris_layout_box box where box.cell = cell.id);

-- DELETE empty rows
DELETE FROM cris_layout_row l_row 
where NOT EXISTS (SELECT 1 from cris_layout_cell cell where cell.row = l_row.id);

-- DELETE empty tabs
DELETE FROM cris_layout_tab tab
where NOT EXISTS (SELECT 1 from cris_layout_row l_row where l_row.tab = tab.id);

-- REPLACE cris.owner with dspace.object.owner
UPDATE metadatafieldregistry 
SET element = 'object',
    qualifier ='owner',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'owner' 
AND qualifier is null
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris'); 

-- REPLACE cris.orcid.scope with dspace.orcid.scope
UPDATE metadatafieldregistry 
SET element = 'orcid',
    qualifier = 'scope',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'orcid' 
AND qualifier = 'scope'
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris'); 

-- REPLACE cris.orcid.sync-mode with dspace.orcid.sync-mode
UPDATE metadatafieldregistry 
SET element = 'orcid',
    qualifier = 'sync-mode',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'orcid' 
AND qualifier = 'sync-mode'
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris'); 

-- REPLACE cris.orcid.sync-publications with dspace.orcid.sync-publications
UPDATE metadatafieldregistry 
SET element = 'orcid',
    qualifier = 'sync-publications',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'orcid' 
AND qualifier = 'sync-publications'
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris'); 

-- REPLACE cris.orcid.sync-fundings with dspace.orcid.sync-fundings
UPDATE metadatafieldregistry 
SET element = 'orcid',
    qualifier = 'sync-fundings',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'orcid' 
AND qualifier = 'sync-fundings'
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris'); 

-- REPLACE cris.orcid.sync-profile with dspace.orcid.sync-profile
UPDATE metadatafieldregistry 
SET element = 'orcid',
    qualifier = 'sync-profile',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'orcid' 
AND qualifier = 'sync-profile'
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris'); 

-- REPLACE cris.orcid.authenticated with dspace.orcid.authenticated
UPDATE metadatafieldregistry 
SET element = 'orcid',
    qualifier = 'authenticated',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'orcid' 
AND qualifier = 'authenticated'
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris');

-- REPLACE cris.orcid.webhook with dspace.orcid.webhook
UPDATE metadatafieldregistry 
SET element = 'orcid',
    qualifier = 'webhook',
	metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
WHERE element = 'orcid' 
AND qualifier = 'webhook'
AND metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris');

-- Migrate ORCID access tokens
INSERT INTO orcid_token 
SELECT 
	nextval('orcid_token_id_seq'),
	owner_metadata_value.authority::uuid,
	token_metadata_value.dspace_object_id,
	token_metadata_value.text_value
FROM metadatavalue token_metadata_value, metadatavalue owner_metadata_value, item item
WHERE owner_metadata_value.dspace_object_id = token_metadata_value.dspace_object_id
AND item.uuid = owner_metadata_value.dspace_object_id
AND item.in_archive = true
AND owner_metadata_value.metadata_field_id = (
	SELECT mf.metadata_field_id FROM metadatafieldregistry mf 
	WHERE mf.element = 'object' AND mf.qualifier = 'owner'
	AND mf.metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'dspace')
) 
AND token_metadata_value.metadata_field_id = (
	SELECT mf.metadata_field_id FROM metadatafieldregistry mf 
	WHERE mf.element = 'orcid' AND mf.qualifier = 'access-token'
	AND mf.metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'cris')
);

INSERT INTO orcid_token 
SELECT 
	nextval('orcid_token_id_seq'),
	token_metadata_value.dspace_object_id,
	null,
	token_metadata_value.text_value
FROM metadatavalue token_metadata_value
WHERE NOT EXISTS (SELECT 1 FROM orcid_token ot where ot.eperson_id = token_metadata_value.dspace_object_id)
AND token_metadata_value.metadata_field_id = (
	SELECT mf.metadata_field_id FROM metadatafieldregistry mf 
	WHERE mf.element = 'orcid' AND mf.qualifier = 'access-token'
	AND mf.metadata_schema_id = (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id = 'eperson')
);

-- DELETE access token and refresh token metadata values
DELETE FROM metadatavalue mv 
WHERE mv.metadata_field_id  IN (
SELECT mf.metadata_field_id FROM metadatafieldregistry mf 
	WHERE mf.element = 'orcid' AND mf.qualifier IN ('refresh-token', 'access-token')
	AND mf.metadata_schema_id IN (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id IN ('eperson', 'cris'))
);

DELETE FROM metadatafieldregistry mf 
WHERE mf.element = 'orcid' AND mf.qualifier IN ('refresh-token', 'access-token')
AND mf.metadata_schema_id IN (SELECT ms.metadata_schema_id from metadataschemaregistry ms where ms.short_id IN ('eperson', 'cris'));
