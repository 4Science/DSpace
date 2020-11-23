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
	'ALTER TABLE CRIS_RP_PROP DROP CONSTRAINT FKC8A841F5E52079D7F40BFC5E';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR1: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
    
    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_RP_PROP ADD CONSTRAINT FKC8A841F5E52079D7F40BFC5E FOREIGN KEY (value_id) REFERENCES JDYNA_VALUES (id) DEFERRABLE INITIALLY DEFERRED ENABLE';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR2: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_DO_PROP DROP CONSTRAINT FKC8A841F5E52079D7DBFE631';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR3: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_DO_PROP ADD CONSTRAINT FKC8A841F5E52079D7DBFE631 FOREIGN KEY (value_id) REFERENCES jdyna_values (id) DEFERRABLE INITIALLY DEFERRED ENABLE';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR4: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_OU_PROP DROP CONSTRAINT FKC8A841F5E52079D75DE185B6';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR5: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_OU_PROP ADD CONSTRAINT FKC8A841F5E52079D75DE185B6 FOREIGN KEY (value_id) REFERENCES jdyna_values (id) DEFERRABLE INITIALLY DEFERRED ENABLE';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR6: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_PJ_PROP DROP CONSTRAINT FKC8A841F5E52079D780027222';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR7: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

    BEGIN
	EXECUTE IMMEDIATE
	'ALTER TABLE CRIS_PJ_PROP ADD CONSTRAINT FKC8A841F5E52079D780027222 FOREIGN KEY (value_id) REFERENCES jdyna_values (id) DEFERRABLE INITIALLY DEFERRED ENABLE';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR8: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
END;
