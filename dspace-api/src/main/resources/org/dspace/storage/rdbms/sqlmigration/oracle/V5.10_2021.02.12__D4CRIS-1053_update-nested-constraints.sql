--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

DECLARE
 str    varchar2(1024);
 retval   VARCHAR2 (255);

BEGIN
    BEGIN
    str := 'select distinct all_cols.constraint_name
        from all_cons_columns col
        join all_cons_columns all_cols
            on col.owner = all_cols.owner and col.constraint_name = all_cols.constraint_name
        join all_constraints cons
            on col.owner = cons.owner and col.constraint_name = cons.constraint_name
        where col.table_name = ''CRIS_RP_NO''
            and (col.column_name = ''TYPO_ID'' or col.column_name = ''POSITIONDEF'' or col.column_name = ''ID'')
            and cons.constraint_type in (''U'')';
    EXECUTE IMMEDIATE str INTO retval;
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_RP_NO DROP CONSTRAINT '|| retval;
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR1: ' || DBMS_UTILITY.FORMAT_ERROR_STACK || ' CONSTRAINT:' || retval);
        NULL;
    END;

    BEGIN
    str := 'select distinct all_cols.constraint_name
        from all_cons_columns col
        join all_cons_columns all_cols
            on col.owner = all_cols.owner and col.constraint_name = all_cols.constraint_name
        join all_constraints cons
            on col.owner = cons.owner and col.constraint_name = cons.constraint_name
        where col.table_name = ''CRIS_OU_NO''
            and (col.column_name = ''TYPO_ID'' or col.column_name = ''POSITIONDEF'' or col.column_name = ''ID'')
            and cons.constraint_type in (''U'')';
    EXECUTE IMMEDIATE str INTO retval;
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_OU_NO DROP CONSTRAINT '|| retval;
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR2: ' || DBMS_UTILITY.FORMAT_ERROR_STACK || ' CONSTRAINT:' || retval);
        NULL;
    END;

    BEGIN
    str := 'select distinct all_cols.constraint_name
        from all_cons_columns col
        join all_cons_columns all_cols
            on col.owner = all_cols.owner and col.constraint_name = all_cols.constraint_name
        join all_constraints cons
            on col.owner = cons.owner and col.constraint_name = cons.constraint_name
        where col.table_name = ''CRIS_PJ_NO''
            and (col.column_name = ''TYPO_ID'' or col.column_name = ''POSITIONDEF'' or col.column_name = ''ID'')
            and cons.constraint_type in (''U'')';
    EXECUTE IMMEDIATE str INTO retval;
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_PJ_NO DROP CONSTRAINT '|| retval;
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR3: ' || DBMS_UTILITY.FORMAT_ERROR_STACK || ' CONSTRAINT:' || retval);
        NULL;
    END;

    BEGIN
    str := 'select distinct all_cols.constraint_name
        from all_cons_columns col
        join all_cons_columns all_cols
            on col.owner = all_cols.owner and col.constraint_name = all_cols.constraint_name
        join all_constraints cons
            on col.owner = cons.owner and col.constraint_name = cons.constraint_name
        where col.table_name = ''CRIS_DO_NO''
            and (col.column_name = ''TYPO_ID'' or col.column_name = ''POSITIONDEF'' or col.column_name = ''ID'')
            and cons.constraint_type in (''U'')';
    EXECUTE IMMEDIATE str INTO retval;
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_DO_NO DROP CONSTRAINT '|| retval;
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR4: ' || DBMS_UTILITY.FORMAT_ERROR_STACK || ' CONSTRAINT:' || retval);
        NULL;
    END;


    -- we need these constraints to be deferred to allow hibernate to sort INSERT/DELETE batch queries
    BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_RP_NO
        ADD CONSTRAINT CRIS_RP_NO_POSITIONDEF_TYPO_ID_PARENT_ID_KEY UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE ENABLE';
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR5: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
        NULL;
    END;

    BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_OU_NO
        ADD CONSTRAINT CRIS_OU_NO_POSITIONDEF_TYPO_ID_PARENT_ID_KEY UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE ENABLE';
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR6: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
        NULL;
    END;

    BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_PJ_NO
        ADD CONSTRAINT CRIS_PJ_NO_POSITIONDEF_TYPO_ID_PARENT_ID_KEY UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE ENABLE';
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR7: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
        NULL;
    END;

    BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE CRIS_DO_NO
        ADD CONSTRAINT CRIS_DO_NO_POSITIONDEF_TYPO_ID_PARENT_ID_KEY UNIQUE (positiondef, typo_id, parent_id) INITIALLY DEFERRED DEFERRABLE ENABLE';
    EXCEPTION
    WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)
        DBMS_OUTPUT.put_line ('ERROR8: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
        NULL;
    END;

END;