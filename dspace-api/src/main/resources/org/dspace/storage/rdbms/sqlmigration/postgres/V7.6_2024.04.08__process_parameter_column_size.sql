--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

----------------------------------------------------
-- double the length of parameters column on process table
----------------------------------------------------

ALTER TABLE process ALTER COLUMN parameters TYPE character varying(1024);
