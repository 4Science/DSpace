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
			'ALTER TABLE imp_record_to_item ADD (imp_sourceref VARCHAR2(256))';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
	BEGIN
		EXECUTE IMMEDIATE
			'ALTER TABLE imp_record ADD (imp_sourceref VARCHAR2(256))';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
	BEGIN
		EXECUTE IMMEDIATE
			'ALTER TABLE imp_record MODIFY imp_record_id VARCHAR2(256)'; 
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
	BEGIN
		EXECUTE IMMEDIATE
			'ALTER TABLE imp_record_to_item MODIFY imp_record_id VARCHAR2(256)'; 
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
END;
