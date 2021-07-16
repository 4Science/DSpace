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
	 		'ALTER TABLE imp_record_to_item DROP CONSTRAINT  imp_record_to_item_pkey';
		EXECUTE IMMEDIATE  
	 		'ALTER TABLE imp_record_to_item ADD PRIMARY KEY (imp_record_id, imp_sourceref)';
	    EXCEPTION
		WHEN OTHERS
	    THEN
	        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
	      DBMS_OUTPUT.put_line ('ERROR2: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
	      NULL;
    END;
END;