--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-------------------------------------------------------------------------------------
---- UPDATE table metadatavalue
-------------------------------------------------------------------------------------

-- Add 'glamfonds.index' field to metadataschemaregistry (if missing)
INSERT INTO metadatafieldregistry (metadata_schema_id, element)
SELECT msr.metadata_schema_id, 'index'
FROM metadataschemaregistry msr
WHERE EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glamfonds')
   AND msr.short_id = 'glamfonds' AND NOT EXISTS (
    SELECT 1
    FROM metadatafieldregistry mfr
    WHERE mfr.metadata_schema_id = msr.metadata_schema_id
      AND mfr.element = 'index'
);

-- REPLACE dc.identifier.archivalunit with glamfonds.index for publication, picture, archival_material
update metadatavalue mv
set metadata_field_id = (select mfr.metadata_field_id
                         from metadatafieldregistry mfr
                                  inner join metadataschemaregistry msr
                                             on mfr.metadata_schema_id = msr.metadata_schema_id
                         where msr.short_id = 'glamfonds'
                           and mfr.element = 'index'
                           and mfr.qualifier is null)
where EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glamfonds')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'dc')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'cris')
  and mv.metadata_field_id in (select mfr.metadata_field_id
                               from metadatafieldregistry mfr
                                        inner join metadataschemaregistry msr
                                                   on mfr.metadata_schema_id = msr.metadata_schema_id
                               where msr.short_id = 'dc'
                                 and mfr.element = 'identifier'
                                 and mfr.qualifier = 'archivalunit')
  and mv.dspace_object_id in (select distinct c2i.item_id
                              from collection2item c2i
                                       inner join metadatavalue mv on mv.dspace_object_id = c2i.collection_id
                                       inner join metadatafieldregistry mfr
                                                  on mfr.metadata_field_id = mv.metadata_field_id
                                       inner join metadataschemaregistry msr
                                                  on mfr.metadata_schema_id = msr.metadata_schema_id
                              where msr.short_id = 'cris'
                                and mfr.element = 'submission'
                                and mfr.qualifier = 'definition'
                                and (mv.text_value = 'picture' or mv.text_value = 'publication' or
                                     mv.text_value = 'archival_material'));

-- REPLACE glam.index with glamfonds.index for fonds
update metadatavalue mv
set metadata_field_id = (select mfr.metadata_field_id
                         from metadatafieldregistry mfr
                                  inner join metadataschemaregistry msr
                                             on mfr.metadata_schema_id = msr.metadata_schema_id
                         where msr.short_id = 'glamfonds'
                           and mfr.element = 'index'
                           and mfr.qualifier is null)
where EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glamfonds')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glam')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'cris')
  and mv.metadata_field_id in (select mfr.metadata_field_id
                               from metadatafieldregistry mfr
                                        inner join metadataschemaregistry msr
                                                   on mfr.metadata_schema_id = msr.metadata_schema_id
                               where msr.short_id = 'glam'
                                 and mfr.element = 'index'
                                 and mfr.qualifier is null)
  and mv.dspace_object_id in (select distinct c2i.item_id
                              from collection2item c2i
                                       inner join metadatavalue mv on mv.dspace_object_id = c2i.collection_id
                                       inner join metadatafieldregistry mfr
                                                  on mfr.metadata_field_id = mv.metadata_field_id
                                       inner join metadataschemaregistry msr
                                                  on mfr.metadata_schema_id = msr.metadata_schema_id
                              where msr.short_id = 'cris'
                                and mfr.element = 'submission'
                                and mfr.qualifier = 'definition'
                                and mv.text_value = 'fonds');

-- Add 'glamjournalfonds.index' field to registry (if missing)
INSERT INTO metadatafieldregistry (metadata_schema_id, element)
SELECT msr.metadata_schema_id, 'index'
FROM metadataschemaregistry msr
WHERE EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glamjournalfonds')
  and msr.short_id = 'glamjournalfonds' AND NOT EXISTS (
    SELECT 1
    FROM metadatafieldregistry mfr
    WHERE mfr.metadata_schema_id = msr.metadata_schema_id
      AND mfr.element = 'index'
);

-- REPLACE dc.identifier.archivalunit with glamjournalfonds.index for journal_file
update metadatavalue mv
set metadata_field_id = (select mfr.metadata_field_id
                         from metadatafieldregistry mfr
                                  inner join metadataschemaregistry msr
                                             on mfr.metadata_schema_id = msr.metadata_schema_id
                         where msr.short_id = 'glamjournalfonds'
                           and mfr.element = 'index'
                           and mfr.qualifier is null)
where EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glamjournalfonds')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'dc')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'cris')
  and mv.metadata_field_id in (select mfr.metadata_field_id
                               from metadatafieldregistry mfr
                                        inner join metadataschemaregistry msr
                                                   on mfr.metadata_schema_id = msr.metadata_schema_id
                               where msr.short_id = 'dc'
                                 and mfr.element = 'identifier'
                                 and mfr.qualifier = 'archivalunit')
  and mv.dspace_object_id in (select distinct c2i.item_id
                              from collection2item c2i
                                       inner join metadatavalue mv on mv.dspace_object_id = c2i.collection_id
                                       inner join metadatafieldregistry mfr
                                                  on mfr.metadata_field_id = mv.metadata_field_id
                                       inner join metadataschemaregistry msr
                                                  on mfr.metadata_schema_id = msr.metadata_schema_id
                              where msr.short_id = 'cris'
                                and mfr.element = 'submission'
                                and mfr.qualifier = 'definition'
                                and mv.text_value = 'journal_file');

-- REPLACE glam.index with glamjournalfonds.index for journal_fonds
update metadatavalue mv
set metadata_field_id = (select mfr.metadata_field_id
                         from metadatafieldregistry mfr
                                  inner join metadataschemaregistry msr
                                             on mfr.metadata_schema_id = msr.metadata_schema_id
                         where msr.short_id = 'glamjournalfonds'
                           and mfr.element = 'index'
                           and mfr.qualifier is null)
where EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glamjournalfonds')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'glam')
  and EXISTS(SELECT 1
             FROM metadataschemaregistry
             WHERE short_id = 'cris')
  and mv.metadata_field_id in (select mfr.metadata_field_id
                               from metadatafieldregistry mfr
                                        inner join metadataschemaregistry msr
                                                   on mfr.metadata_schema_id = msr.metadata_schema_id
                               where msr.short_id = 'glam'
                                 and mfr.element = 'index'
                                 and mfr.qualifier is null)
  and mv.dspace_object_id in (select distinct c2i.item_id
                              from collection2item c2i
                                       inner join metadatavalue mv on mv.dspace_object_id = c2i.collection_id
                                       inner join metadatafieldregistry mfr
                                                  on mfr.metadata_field_id = mv.metadata_field_id
                                       inner join metadataschemaregistry msr
                                                  on mfr.metadata_schema_id = msr.metadata_schema_id
                              where msr.short_id = 'cris'
                                and mfr.element = 'submission'
                                and mfr.qualifier = 'definition'
                                and mv.text_value = 'journal_fonds');

-- DELETE glam.index from aggregation
delete
from metadatavalue mv
where EXISTS(SELECT 1
    FROM metadataschemaregistry
    WHERE short_id = 'glam')
  and EXISTS(SELECT 1
    FROM metadataschemaregistry
    WHERE short_id = 'cris')
  and mv.metadata_field_id in (select mfr.metadata_field_id
                               from metadatafieldregistry mfr
                                        inner join metadataschemaregistry msr
                                                   on mfr.metadata_schema_id = msr.metadata_schema_id
                               where msr.short_id = 'glam'
                                 and mfr.element = 'index'
                                 and mfr.qualifier is null)
  and mv.dspace_object_id in (select distinct c2i.item_id
                              from collection2item c2i
                                       inner join metadatavalue mv on mv.dspace_object_id = c2i.collection_id
                                       inner join metadatafieldregistry mfr
                                                  on mfr.metadata_field_id = mv.metadata_field_id
                                       inner join metadataschemaregistry msr
                                                  on mfr.metadata_schema_id = msr.metadata_schema_id
                              where msr.short_id = 'cris'
                                and mfr.element = 'submission'
                                and mfr.qualifier = 'definition'
                                and mv.text_value = 'aggregation');