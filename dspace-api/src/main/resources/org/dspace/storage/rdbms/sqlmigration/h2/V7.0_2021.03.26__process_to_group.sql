--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================
-------------------------------------------------------------------------------
-- Sequences for Process within Group feature
-------------------------------------------------------------------------------

CREATE TABLE Process2Group
(
  process_id INTEGER REFERENCES Process(process_id),
  group_id UUID REFERENCES epersongroup (uuid) ON DELETE CASCADE
);
-----------------------------------------------------------------------------------
-- Drop the 'history_seq' sequence (related table deleted at Dspace-1.5)
-----------------------------------------------------------------------------------

DROP SEQUENCE history_seq;
