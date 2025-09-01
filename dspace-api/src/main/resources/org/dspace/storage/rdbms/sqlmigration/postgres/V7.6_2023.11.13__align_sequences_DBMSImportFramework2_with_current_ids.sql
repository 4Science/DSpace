--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create sequences for DBMS Import framework
-----------------------------------------------------------------------------------
do $$
begin

	SELECT pg_catalog.setval('imp_record_seq', (SELECT coalesce(MAX(imp_id),0) FROM imp_record)+1);
	
	SELECT pg_catalog.setval('imp_metadatavalue_seq', (SELECT coalesce(MAX(imp_metadatavalue_id),0) FROM imp_metadatavalue)+1);
	
	SELECT pg_catalog.setval('imp_bitstream_seq', (SELECT coalesce(MAX(imp_bitstream_id),0) FROM imp_bitstream)+1);
	
	SELECT pg_catalog.setval('imp_bitstream_metadatavalue_seq', (SELECT coalesce(MAX(imp_bitstream_metadatavalue_id),0) FROM imp_bitstream_metadatavalue)+1);

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Rollback --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';