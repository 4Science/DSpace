--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--
 
BEGIN
	-- need mandatory to send affiliation (employment and education) to Orcid Registry
	EXECUTE IMMEDIATE	
	'UPDATE CRIS_OU_PDEF SET MANDATORY = 1 WHERE SHORTNAME = ''city''';
	EXECUTE IMMEDIATE
	'UPDATE CRIS_OU_PDEF SET MANDATORY = 1 WHERE SHORTNAME = ''iso-3166-country''';
EXCEPTION
	WHEN OTHERS
    THEN
	    -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      	DBMS_OUTPUT.put_line ('ERROR: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
    	NULL;
END;
