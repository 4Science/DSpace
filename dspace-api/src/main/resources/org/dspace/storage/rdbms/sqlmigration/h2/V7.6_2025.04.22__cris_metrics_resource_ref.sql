--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- replace the cris_metrics_resource_id_fkey to include ON DELETE CASCADE to automatically
-- remove metrics when their associated resource is deleted
ALTER TABLE IF EXISTS cris_metrics DROP CONSTRAINT IF EXISTS cris_metrics_resource_id_fkey;
ALTER TABLE cris_metrics ADD CONSTRAINT cris_metrics_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES DSpaceObject(uuid) ON DELETE CASCADE;

-- add resourcetype column
ALTER TABLE cris_metrics ADD COLUMN resource_type INT;

-- populate the resourcetype column
UPDATE cris_metrics set resource_type = 4 where resource_id in (SELECT uuid FROM community);
UPDATE cris_metrics set resource_type = 3 where resource_id in (SELECT uuid FROM collection);
UPDATE cris_metrics set resource_type = 2 where resource_id in (SELECT uuid FROM item);