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
    EXECUTE IMMEDIATE 'ALTER TABLE jdyna_values ADD booleanValue number(1,0)';
  END;
  
  BEGIN
    SELECT COUNT(*)
    INTO VCOUNT
    FROM USER_TABLES
    WHERE UPPER(TABLE_NAME) = 'JDYNA_WIDGET_BOOLEAN';
	
    IF (VCOUNT=0) THEN
      EXECUTE IMMEDIATE 'CREATE TABLE JDYNA_WIDGET_BOOLEAN (ID NUMBER(10,0) NOT NULL, SHOWASTYPE VARCHAR2(255 CHAR), CHECKED NUMBER(1,0), HIDEWHENFALSE NUMBER(1,0), PRIMARY KEY (ID))';
    END IF;
  END;
  
  -- Table to mantain the potential match between item and rp --
  BEGIN
    SELECT COUNT(*)
    INTO VCOUNT
    FROM USER_TABLES
    WHERE UPPER(TABLE_NAME) = 'POTENTIALMATCHES';
	
    IF (VCOUNT=0) THEN
      EXECUTE IMMEDIATE 'CREATE TABLE POTENTIALMATCHES(POTENTIALMATCHES_ID INTEGER, ITEM_ID INTEGER, RP VARCHAR2(20 byte), PENDING NUMBER(1), PRIMARY KEY (POTENTIALMATCHES_ID))';
    END IF;
  END;
  
  BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE potentialmatches_seq';
  END;
  BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX rp_idx ON potentialmatches (rp)';
  END;
  BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX pending_idx ON potentialmatches (pending)';
  END;
  -- END potential matches --
  
  BEGIN
    EXECUTE IMMEDIATE 'create table cris_orcid_history (id number(10,0) not null, itemId number(10,0), projectId number(10,0), researcherId number(10,0), responseMessage clob, lastAttempt timestamp, lastSuccess timestamp, primary key 
(id))';
  END;
  
  BEGIN
    EXECUTE IMMEDIATE 'create table cris_orcid_queue (id number(10,0) not null, itemId number(10,0), "MODE" varchar2(255 CHAR), projectId number(10,0), researcherId number(10,0), send number(1,0) not null, primary key (id))';
  END;
  
  BEGIN
    EXECUTE IMMEDIATE 'create sequence CRIS_ORCIDHISTORY_SEQ';
  END;
  
  BEGIN
    EXECUTE IMMEDIATE 'create sequence CRIS_ORCIDQUEUE_SEQ';
  END;
END;
