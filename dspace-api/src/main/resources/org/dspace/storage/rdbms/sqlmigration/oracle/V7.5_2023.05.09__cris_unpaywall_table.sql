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
	id number(10,0) not null,
	doi varchar2(255 char),
	status varchar2(64 char),
	item_id RAW(16) not null unique,
	json_record clob,
	timestamp_created TIMESTAMP(6),
	timestamp_last_modified TIMESTAMP(6),
	PRIMARY KEY (id)
);
