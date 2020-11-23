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
			'ALTER TABLE cris_do_no ADD (preferred NUMBER(1,0))';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
	BEGIN
		EXECUTE IMMEDIATE
			'ALTER TABLE cris_ou_no ADD (preferred NUMBER(1,0))';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
	BEGIN
		EXECUTE IMMEDIATE
			'ALTER TABLE cris_pj_no ADD (preferred NUMBER(1,0))';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;
	BEGIN
		EXECUTE IMMEDIATE
			'ALTER TABLE cris_rp_no ADD (preferred NUMBER(1,0))';
		EXCEPTION
		WHEN OTHERS
		THEN
		   NULL;
	END;	
END;
