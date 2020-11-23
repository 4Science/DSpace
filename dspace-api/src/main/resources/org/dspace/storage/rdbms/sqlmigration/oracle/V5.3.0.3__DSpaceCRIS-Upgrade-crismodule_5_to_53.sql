--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

BEGIN
  BEGIN
    EXECUTE IMMEDIATE 'alter table cris_orcid_queue add (fastlookupobjectname clob)';
  EXCEPTION
  WHEN OTHERS THEN
    NULL;
  END;
  BEGIN
    EXECUTE IMMEDIATE 'alter table cris_orcid_queue add (fastlookupuuid varchar2(255 CHAR))';
  EXCEPTION
  WHEN OTHERS THEN
    NULL;
  END;
END;