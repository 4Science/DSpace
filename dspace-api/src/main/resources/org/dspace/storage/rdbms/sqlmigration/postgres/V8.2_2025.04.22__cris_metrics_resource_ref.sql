--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- 1. Safely recreate the foreign key constraint to include ON DELETE CASCADE
ALTER TABLE cris_metrics DROP CONSTRAINT IF EXISTS cris_metrics_resource_id_fkey;
ALTER TABLE cris_metrics ADD CONSTRAINT cris_metrics_resource_id_fkey FOREIGN KEY (resource_id) REFERENCES DSpaceObject(uuid) ON DELETE CASCADE;

-- 2. Add resource_type column safely using IF NOT EXISTS
ALTER TABLE cris_metrics ADD COLUMN IF NOT EXISTS resource_type INT;

-- 3. Populate the resourcetype column
-- These UPDATE statements are natively idempotent; they will just re-apply the values if run again.
UPDATE cris_metrics SET resource_type = 4 WHERE resource_id IN (SELECT uuid FROM community);
UPDATE cris_metrics SET resource_type = 3 WHERE resource_id IN (SELECT uuid FROM collection);
UPDATE cris_metrics SET resource_type = 2 WHERE resource_id IN (SELECT uuid FROM item);