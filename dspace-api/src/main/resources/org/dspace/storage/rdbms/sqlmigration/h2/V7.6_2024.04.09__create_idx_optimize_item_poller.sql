--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create INDEXES to optimize exact query over the metadatavalue
-----------------------------------------------------------------------------------

-- we cannot create the idx related to the text_value substring in H2 as index on
-- expression are not supported, see https://github.com/h2database/h2database/issues/3535
-- CREATE INDEX idx_text_value_hash ON metadatavalue (SUBSTRING(text_value,1,36));
CREATE INDEX idx_authority_hash ON metadatavalue (authority);