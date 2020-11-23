--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

begin
  for rec in (select a.constraint_name from ALL_CONS_COLUMNS a, user_constraints b
    where a.table_name = 'METADATAVALUE' and a.column_name = 'RESOURCE_ID' and b.constraint_type = 'R'
    and A.CONSTRAINT_NAME = b.CONSTRAINT_NAME) loop
      execute immediate 'alter table metadatavalue drop constraint '||rec.constraint_name;
  end loop;
  exception when others then
	null;
end;