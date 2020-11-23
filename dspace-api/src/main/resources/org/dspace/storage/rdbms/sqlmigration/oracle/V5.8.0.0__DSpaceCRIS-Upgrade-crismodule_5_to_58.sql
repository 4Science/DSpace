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
	'DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-activities-update'')))';
    	EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR1: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
    BEGIN
	EXECUTE IMMEDIATE
	'DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-activities-update''))';
        	EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR2: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
    BEGIN
	EXECUTE IMMEDIATE
	'DELETE FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-activities-update''))';
        	EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR3: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
    BEGIN
	EXECUTE IMMEDIATE
	'DELETE FROM CRIS_RP_PDEF WHERE SHORTNAME IN (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-activities-update'')';
        	EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR4: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
    BEGIN
	EXECUTE IMMEDIATE	
	'DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''orcid-profile-pref-biography'',''orcid-profile-pref-email'',''orcid-profile-pref-fullName'',''orcid-profile-pref-preferredName'',''orcid-profile-pref-otheremails'')))';
        EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR5: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE	
	'DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''orcid-profile-pref-biography'',''orcid-profile-pref-email'',''orcid-profile-pref-fullName'',''orcid-profile-pref-preferredName'',''orcid-profile-pref-otheremails''))';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR6: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-token-activities-update'' WHERE SHORTNAME = ''system-orcid-token-orcid-works-update''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR7: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-token-person-update'' WHERE SHORTNAME = ''system-orcid-token-orcid-bio-update''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR8: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-token-read-limited'' WHERE SHORTNAME = ''system-orcid-token-orcid-profile-read-limited''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR9: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-biography'' WHERE SHORTNAME = ''orcid-profile-pref-biography''';
       EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR10: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
    
    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-email'' WHERE SHORTNAME = ''orcid-profile-pref-email''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR11: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-fullName'' WHERE SHORTNAME = ''orcid-profile-pref-fullName''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR12: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-preferredName'' WHERE SHORTNAME = ''orcid-profile-pref-preferredName''';
        EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR13: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
    
    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-otheremails'' WHERE SHORTNAME = ''orcid-profile-pref-otheremails''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR14: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_ORCID_HISTORY ADD (orcid varchar2(255))';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR15: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_ORCID_HISTORY DROP COLUMN entityid';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR16: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_OU_PDEF SET MANDATORY = 1 WHERE SHORTNAME = ''city''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR17: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'UPDATE CRIS_OU_PDEF SET MANDATORY = 1 WHERE SHORTNAME = ''iso-3166-country''';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR18: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
    EXECUTE IMMEDIATE
	'DELETE FROM CRIS_ORCID_HISTORY';
	EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
      DBMS_OUTPUT.put_line ('ERROR19: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
END;