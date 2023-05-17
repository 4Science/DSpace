--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create table for unpaywall api
-----------------------------------------------------------------------------------

CREATE SEQUENCE cris_unpaywall_seq;

CREATE TABLE cris_unpaywall
(
	id uuid not null,
	doi varchar(255),
	status varchar(64),
	item_id int4 unique,
	json_record text,
	timestamp_created timestamp,
	timestamp_last_modified timestamp,
	primary key (id)
);
