do $$
begin

	UPDATE metadatavalue AS mv
	SET authority = temprow.dspace_object_id
	FROM (
	    SELECT m.text_value, m.dspace_object_id
	    FROM metadatavalue AS m
	    JOIN metadatafieldregistry AS mf ON m.metadata_field_id = mf.metadata_field_id
	    WHERE mf."element" = 'legacyId'
	) AS temprow
	WHERE mv.authority  = temprow.text_value;
	
	update metadatavalue as mtdval set authority = subquery.resource_id 
	from (select handle.resource_id,metadatavalue.metadata_value_id from metadatavalue  
	 		join handle on handle.handle = metadatavalue.authority) as subquery 
	where EXISTS (select 1 from handle where handle.handle=mtdval.authority) 
	and subquery.metadata_value_id=mtdval.metadata_value_id;

	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Rollback --> % %', SQLERRM, SQLSTATE;
end; $$