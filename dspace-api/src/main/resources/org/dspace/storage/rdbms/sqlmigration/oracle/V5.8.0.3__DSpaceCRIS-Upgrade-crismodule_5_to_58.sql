--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	EXECUTE IMMEDIATE	
	'ALTER TABLE CRIS_ORCID_HISTORY ADD orcid varchar(255)';
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_ORCID_HISTORY DROP COLUMN entityid';
	EXECUTE IMMEDIATE
	'DELETE FROM CRIS_ORCID_HISTORY';
EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      	DBMS_OUTPUT.put_line ('ERROR: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
    	DELETE FROM CRIS_ORCID_HISTORY;
END;
