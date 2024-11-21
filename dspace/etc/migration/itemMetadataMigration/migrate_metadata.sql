do $$
begin

exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Rollback --> % %', SQLERRM, SQLSTATE;
end; $$
