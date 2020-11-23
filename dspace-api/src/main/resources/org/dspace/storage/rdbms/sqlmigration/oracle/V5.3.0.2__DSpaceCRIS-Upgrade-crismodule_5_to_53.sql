--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

DECLARE
  VCOUNT NUMBER (10);
BEGIN
  BEGIN
    SELECT COUNT(*)
    INTO VCOUNT
    FROM USER_TABLES
    WHERE LOWER(TABLE_NAME) = 'cris_orcid_history';
    IF (VCOUNT=0) THEN
      EXECUTE IMMEDIATE 'create table cris_orcid_history (id number(10,0) not null, owner varchar2(255 CHAR), entityId number(10,0), typeId number(10,0), responseMessage clob, lastAttempt timestamp, lastSuccess timestamp, primary key (id))';
    END IF;
  END;
  
  BEGIN
    SELECT COUNT(*)
    INTO VCOUNT
    FROM USER_TABLES
    WHERE LOWER(TABLE_NAME) = 'cris_orcid_queue';
    IF (VCOUNT=0) THEN
      EXECUTE IMMEDIATE 'create table cris_orcid_queue (id number(10,0) not null, owner varchar2(255 CHAR), entityId number(10,0), typeId number(10,0), "MODE" varchar2(255 CHAR), fastlookupobjectname clob, fastlookupuuid varchar2(255 CHAR), primary key (id))';
    END IF;
  END;
  
  BEGIN
    SELECT COUNT(*)
    INTO VCOUNT
    FROM USER_TABLES
    WHERE LOWER(TABLE_NAME) = 'jdyna_widget_checkradio';
    IF (VCOUNT              =0) THEN
      EXECUTE IMMEDIATE 'create table jdyna_widget_checkradio (id number(10,0) not null, option4row number(10,0), staticValues clob, dropdown number(1,0), primary key (id))';
    END IF;
  END;
  
  BEGIN
    EXECUTE IMMEDIATE 'create sequence CRIS_ORCIDHISTORY_SEQ';
  EXCEPTION
  WHEN OTHERS THEN
    NULL;
  END;
  
  BEGIN
    EXECUTE IMMEDIATE 'create sequence CRIS_ORCIDQUEUE_SEQ';
  EXCEPTION
  WHEN OTHERS THEN
    NULL;
  END;
  
END;