--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Alter table imp_record with additional uuid for predefined uuid to set
-----------------------------------------------------------------------------------

ALTER TABLE imp_record ADD COLUMN if NOT EXISTS uuid UUID;
