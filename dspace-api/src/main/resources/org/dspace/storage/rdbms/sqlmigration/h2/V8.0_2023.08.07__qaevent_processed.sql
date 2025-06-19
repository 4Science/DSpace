--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create QA Event Processed table.
-----------------------------------------------------------------------------------
CREATE TABLE qaevent_processed (
  qaevent_id VARCHAR(255) NOT NULL,
  qaevent_timestamp TIMESTAMP NULL,
  eperson_uuid UUID NULL REFERENCES eperson(uuid),
  item_uuid uuid NOT NULL REFERENCES item(uuid)
);
DROP INDEX IF EXISTS item_uuid_idx;
CREATE INDEX item_uuid_idx ON qaevent_processed(item_uuid);
