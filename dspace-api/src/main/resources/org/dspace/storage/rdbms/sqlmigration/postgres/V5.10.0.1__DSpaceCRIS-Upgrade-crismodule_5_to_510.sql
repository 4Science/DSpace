--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE imp_record_to_item DROP CONSTRAINT  imp_record_to_item_pkey ;
ALTER TABLE imp_record_to_item ADD PRIMARY KEY (imp_record_id, imp_sourceref);