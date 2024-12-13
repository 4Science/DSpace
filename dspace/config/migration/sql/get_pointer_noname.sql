SELECT *
FROM  (SELECT 'project'        AS pointer_table,
              entity.id        AS id,
              crisid
       FROM   old_cris_project entity
       UNION ALL
       SELECT 'ou'        AS pointer_table,
              entity.id        AS id,
              crisid
       FROM   old_cris_orgunit entity
       UNION ALL
       SELECT 'rp'          AS pointer_table,
              entity.id        AS id,
              crisid
       FROM   old_cris_rpage entity
       UNION ALL
       SELECT 'do'             AS pointer_table,
              entity.id        AS id,
              crisid
       FROM   old_cris_do entity)AS innerquery
WHERE  pointer_table = ?
       AND id = ?;