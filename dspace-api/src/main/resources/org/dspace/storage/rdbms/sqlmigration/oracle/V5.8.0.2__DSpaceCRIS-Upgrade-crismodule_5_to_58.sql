--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
	-- delete scopes
	EXECUTE IMMEDIATE
	'DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-orcid-works-update'')))';
	EXECUTE IMMEDIATE
	'DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-orcid-works-update''))';
	EXECUTE IMMEDIATE
    'DELETE FROM CRIS_RP_BOX2CON WHERE jdyna_containable_id  IN (SELECT ID FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-orcid-works-update'')))';
    EXECUTE IMMEDIATE
	'DELETE FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-orcid-works-update''))';
	EXECUTE IMMEDIATE
	'DELETE FROM CRIS_RP_PDEF WHERE SHORTNAME IN (''system-orcid-token-orcid-works-create'',''system-orcid-token-funding-create'',''system-orcid-token-funding-update'',''system-orcid-token-orcid-works-update'')';
	-- delete metadata configuration
	EXECUTE IMMEDIATE	
	'DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''orcid-profile-pref-biography'',''orcid-profile-pref-email'',''orcid-profile-pref-fullName'',''orcid-profile-pref-preferredName'')))';
	EXECUTE IMMEDIATE
	'DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in (''orcid-profile-pref-biography'',''orcid-profile-pref-email'',''orcid-profile-pref-fullName'',''orcid-profile-pref-preferredName''))';
	
	-- upgrade scopes
	EXECUTE IMMEDIATE	
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-token-activities-update'' WHERE SHORTNAME = ''system-orcid-token-orcid-works-update''';
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-token-person-update'' WHERE SHORTNAME = ''system-orcid-token-orcid-bio-update''';
	EXECUTE IMMEDIATE	
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-token-read-limited'' WHERE SHORTNAME = ''system-orcid-token-orcid-profile-read-limited''';
	
	-- upgrade metadata not editable
	EXECUTE IMMEDIATE	
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-biography'' WHERE SHORTNAME = ''orcid-profile-pref-biography''';
	EXECUTE IMMEDIATE	
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-email'' WHERE SHORTNAME = ''orcid-profile-pref-email''';
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-fullName'' WHERE SHORTNAME = ''orcid-profile-pref-fullName''';
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-preferredName'' WHERE SHORTNAME = ''orcid-profile-pref-preferredName''';
	EXECUTE IMMEDIATE
	'UPDATE CRIS_RP_PDEF SET SHORTNAME = ''system-orcid-profile-pref-otheremails'' WHERE SHORTNAME = ''orcid-profile-pref-otheremails''';
	
EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      	DBMS_OUTPUT.put_line ('ERROR: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
		DELETE FROM JDYNA_VALUES WHERE id IN (SELECT value_id FROM CRIS_RP_PROP WHERE typo_id in (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited')));
		DELETE FROM CRIS_RP_PROP WHERE typo_id IN (SELECT id from CRIS_RP_PDEF where SHORTNAME in ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited'));
	    DELETE FROM CRIS_RP_BOX2CON WHERE jdyna_containable_id  IN (SELECT ID FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited')));
		DELETE FROM JDYNA_CONTAINABLE WHERE cris_rp_pdef_fk IN (SELECT id FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited'));
		DELETE FROM CRIS_RP_PDEF WHERE SHORTNAME IN ('orcid-profile-pref-biography','orcid-profile-pref-email','orcid-profile-pref-fullName','orcid-profile-pref-preferredName','orcid-profile-pref-otheremails','system-orcid-token-orcid-works-update','system-orcid-token-orcid-bio-update','system-orcid-token-orcid-profile-read-limited');
END;
