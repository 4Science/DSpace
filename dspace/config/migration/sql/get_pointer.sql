SELECT *
FROM  (SELECT 'project'        AS pointer_table,
              entity.id        AS id,
              values.textvalue AS metadata_value,
              crisid
       FROM   old_cris_project entity,
              old_cris_pj_prop properties,
              old_cris_pj_pdef property_def,
              old_jdyna_values values
       WHERE  property_def.id = properties.typo_id
              AND values.id = properties.value_id
              AND properties.parent_id = entity.id
              AND property_def.shortname = 'title'
       UNION ALL
       SELECT 'ou'        AS pointer_table,
              entity.id        AS id,
              values.textvalue AS metadata_value,
              crisid
       FROM   old_cris_orgunit entity,
              old_cris_ou_prop properties,
              old_cris_ou_pdef property_def,
              old_jdyna_values values
       WHERE  property_def.id = properties.typo_id
              AND values.id = properties.value_id
              AND properties.parent_id = entity.id
              AND property_def.shortname = 'name'
       UNION ALL
       SELECT 'rp'          AS pointer_table,
              entity.id        AS id,
              values.textvalue AS metadata_value,
              crisid
       FROM   old_cris_rpage entity,
              old_cris_rp_prop properties,
              old_cris_rp_pdef property_def,
              old_jdyna_values values
       WHERE  property_def.id = properties.typo_id
              AND values.id = properties.value_id
              AND properties.parent_id = entity.id
              AND property_def.shortname = 'fullName'
       UNION ALL
       SELECT 'do'             AS pointer_table,
              entity.id        AS id,
              values.textvalue AS metadata_value,
              crisid
       FROM   old_cris_do entity,
              old_cris_do_prop properties,
              old_cris_do_pdef property_def,
	   		  old_cris_do_tp entitytp,
              old_jdyna_values values
       WHERE  property_def.id = properties.typo_id
              AND entity.typo_id = entitytp.id
              AND values.id = properties.value_id
              AND properties.parent_id = entity.id
              AND ( property_def.shortname = 'title'
                     OR property_def.shortname LIKE concat(entitytp.shortname,'name') ))AS innerquery
WHERE  pointer_table = ?
       AND id = ? 