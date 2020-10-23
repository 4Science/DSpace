--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

UPDATE metadatavalue set text_value = 'CC BY' where text_value = 'ccby' and metadata_field_id in (select metadata_field_id from metadatafieldregistry where element = 'rights' and qualifier = 'license');
UPDATE metadatavalue set text_value = 'CC BY-SA' where text_value = 'ccbysa' and metadata_field_id in (select metadata_field_id from metadatafieldregistry where element = 'rights' and qualifier = 'license');
UPDATE metadatavalue set text_value = 'CC BY-ND' where text_value = 'ccbynd' and metadata_field_id in (select metadata_field_id from metadatafieldregistry where element = 'rights' and qualifier = 'license');
UPDATE metadatavalue set text_value = 'CC BY-NC' where text_value = 'ccbync' and metadata_field_id in (select metadata_field_id from metadatafieldregistry where element = 'rights' and qualifier = 'license');
UPDATE metadatavalue set text_value = 'CC BY-NC-SA' where text_value = 'ccbyncsa' and metadata_field_id in (select metadata_field_id from metadatafieldregistry where element = 'rights' and qualifier = 'license');
UPDATE metadatavalue set text_value = 'CC BY-NC-ND' where text_value = 'ccbyncnd' and metadata_field_id in (select metadata_field_id from metadatafieldregistry where element = 'rights' and qualifier = 'license');