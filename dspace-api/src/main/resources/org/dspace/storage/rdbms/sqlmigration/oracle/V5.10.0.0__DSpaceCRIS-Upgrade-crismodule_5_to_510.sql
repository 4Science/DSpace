--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	BEGIN
		EXECUTE IMMEDIATE
			'UPDATE cris_rp_pdef set shortname = ''iso-country'' where shortname = ''iso-3166-country''';
		EXECUTE IMMEDIATE			
			'UPDATE cris_ou_pdef set shortname = ''iso-country'' where shortname = ''iso-3166-country''';
		EXECUTE IMMEDIATE			
			'UPDATE cris_rp_pdef set shortname = ''orcid-profile-pref-iso-country'' where shortname = ''orcid-profile-pref-iso-3166-country''';
		EXCEPTION
		WHEN OTHERS
		THEN
		    -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      		DBMS_OUTPUT.put_line ('ERROR: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      		NULL;
	END;
END;	