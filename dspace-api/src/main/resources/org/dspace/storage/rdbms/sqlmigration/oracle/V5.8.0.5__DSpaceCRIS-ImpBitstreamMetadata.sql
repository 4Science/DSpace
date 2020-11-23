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
	'CREATE TABLE IMP_BITSTREAM_MV
	(
	  imp_bitstream_mv_id NUMBER(*,0) NOT NULL,
	  imp_bitstream_id NUMBER(*,0) NOT NULL,
	  imp_schema VARCHAR2(128 BYTE) NOT NULL,
	  imp_element VARCHAR2(128 BYTE) NOT NULL,
	  imp_qualifier VARCHAR2(128 BYTE),
	  imp_value CLOB NOT NULL,
	  imp_authority VARCHAR2(256 BYTE),
	  imp_confidence NUMBER(*,0),
	  imp_share NUMBER(38,0),
	  metadata_order NUMBER(*,0) NOT NULL,
	  text_lang VARCHAR2(32 BYTE),
	  CONSTRAINT imp_bitstream_mv_id_pkey PRIMARY KEY (imp_bitstream_mv_id),      
	  CONSTRAINT imp_bitstream_id_mv_fkey FOREIGN KEY (imp_bitstream_id) REFERENCES imp_bitstream (imp_bitstream_id) ENABLE
	)';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR1: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

	BEGIN
	EXECUTE IMMEDIATE
		'CREATE INDEX imp_bitstream_mv_idx_impid  ON imp_bitstream_mv(imp_bitstream_id)';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR2: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;
  	
    BEGIN
	EXECUTE IMMEDIATE  
 		'CREATE SEQUENCE  imp_bitstream_mv_seq  MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20';
    EXCEPTION
	WHEN OTHERS
    THEN
        -- to see output you have enable DBMS OUTPUT on client (only for debug mode)      
      DBMS_OUTPUT.put_line ('ERROR3: ' || DBMS_UTILITY.FORMAT_ERROR_STACK);
      NULL;
    END;

END;