--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- --
-- Copy raws from nbevent_processed table to qaevent_processed table
-- --
INSERT INTO qaevent_processed (qaevent_id, qaevent_timestamp, eperson_uuid, item_uuid)
SELECT nbevent_id, nbevent_timestamp, eperson_uuid, item_uuid UUID
FROM nbevent_processed as nb WHERE NOT EXISTS(
	SELECT 1 FROM qaevent_processed as qa WHERE qa.qaevent_id = nb.nbevent_id
);

-- --
-- Drop nbevent_processed table
-- --
DELETE FROM nbevent_processed;
DROP TABLE nbevent_processed;