SELECT *
FROM   ((SELECT DISTINCT crisid,
                         property_def2.shortname,
                         nested_object.parent_id,
                         nested_object.positiondef,
                         properties.parent_id AS nested_object_id,
                         properties.visibility,
                         property_def2.value  AS textvalue,
                         CURRENT_TIMESTAMP    AS datevalue,
                         'placeholder'        AS dtype,
                         0                    AS rpvalue,
                         0                    AS projectvalue,
                         0                    AS ouvalue,
                         0                    AS dovalue,
                         NULL                 AS filefolder,
                         NULL                 AS filename,
                         NULL                 AS fileextension,
                         false                AS booleanvalue,
                         NULL                 AS linkdescription,
                         NULL                 AS linkvalue,
                         0                    AS doublevalue,
                         0                    AS classificationvalue,
                         0                    AS custompointer,
               old_cris_project.sourceid,
               old_cris_project.sourceref
         FROM   ( (SELECT *
                 FROM   old_cris_pj_no_prop) AS properties
                  LEFT JOIN (SELECT *
                             FROM   old_cris_pj_no) AS nested_object
                         ON nested_object.id = properties.parent_id
                  LEFT JOIN (SELECT *
                             FROM   old_cris_pj_no_pdef) AS property_def
                         ON property_def.id = properties.typo_id
                  LEFT JOIN (SELECT *
                             FROM   old_cris_pj_no_tp2pdef) AS tp2
                         ON tp2.cris_pj_no_tp_id = nested_object.typo_id
                            AND tp2.cris_pj_no_pdef_id != property_def.id
                  -- CAN't use variables because they contain old_, thus introducing Placeholde variables
                  --join with non-existing values where not exist
                  LEFT JOIN (SELECT '#PLACEHOLDER_PARENT_METADATA_VALUE#' AS
                                    value,
                                    shortname,
                                    id
                             FROM   old_cris_pj_no_pdef) AS property_def2
                         ON property_def2.id = tp2.cris_pj_no_pdef_id
                            AND NOT EXISTS(SELECT *
                                           FROM   old_cris_pj_no_prop AS pdef3
                                           WHERE  pdef3.parent_id =
                                                  properties.parent_id
                                                  AND pdef3.typo_id =
                                                      property_def2.id)
                )
                LEFT JOIN old_cris_project
                       ON nested_object.parent_id = old_cris_project.id
                LEFT JOIN old_jdyna_values
                       ON old_jdyna_values.id = properties.value_id
         WHERE  property_def2.id IS NOT NULL)
        UNION
        (SELECT old_cris_project.crisid,
                old_cris_pj_no_pdef.shortname,
                old_cris_pj_no.parent_id,
                old_cris_pj_no_prop.positiondef,
                old_cris_pj_no_prop.parent_id AS nested_object_id,
                visibility,
                old_jdyna_values.textvalue,
                old_jdyna_values.datevalue,
                old_jdyna_values.dtype,
                old_jdyna_values.rpvalue,
                old_jdyna_values.projectvalue,
                old_jdyna_values.ouvalue,
                old_jdyna_values.dovalue,
                old_jdyna_values.filefolder,
                old_jdyna_values.filename,
                old_jdyna_values.fileextension,
                old_jdyna_values.booleanvalue,
                old_jdyna_values.linkdescription,
                old_jdyna_values.linkvalue,
                old_jdyna_values.doublevalue,
                old_jdyna_values.classificationvalue,
                old_jdyna_values.custompointer,
               old_cris_project.sourceid,
               old_cris_project.sourceref
         FROM   old_jdyna_values
                JOIN old_cris_pj_no_prop
                  ON old_jdyna_values.id = old_cris_pj_no_prop.value_id
                JOIN old_cris_pj_no
                  ON old_cris_pj_no_prop.parent_id = old_cris_pj_no.id
                JOIN old_cris_project
                  ON old_cris_pj_no.parent_id = old_cris_project.id
                RIGHT JOIN old_cris_pj_no_pdef
                        ON old_cris_pj_no_pdef.id = old_cris_pj_no_prop.typo_id)
        UNION
        SELECT old_cris_project.crisid,
               old_cris_pj_pdef.shortname,
               old_cris_pj_prop.parent_id,
               old_cris_pj_prop.positiondef,
               -1 AS nested_object_id,
               visibility,
               old_jdyna_values.textvalue,
               old_jdyna_values.datevalue,
               old_jdyna_values.dtype,
               old_jdyna_values.rpvalue,
               old_jdyna_values.projectvalue,
               old_jdyna_values.ouvalue,
               old_jdyna_values.dovalue,
               old_jdyna_values.filefolder,
               old_jdyna_values.filename,
               old_jdyna_values.fileextension,
               old_jdyna_values.booleanvalue,
               old_jdyna_values.linkdescription,
               old_jdyna_values.linkvalue,
               old_jdyna_values.doublevalue,
               old_jdyna_values.classificationvalue,
               old_jdyna_values.custompointer,
               old_cris_project.sourceid,
               old_cris_project.sourceref
        FROM   old_jdyna_values
               JOIN old_cris_pj_prop
                 ON old_jdyna_values.id = old_cris_pj_prop.value_id
               JOIN old_cris_project
                 ON old_cris_pj_prop.parent_id = old_cris_project.id
               JOIN old_cris_pj_pdef
                 ON old_cris_pj_pdef.id = old_cris_pj_prop.typo_id) AS
       innerquery
ORDER  BY crisid,
          nested_object_id ASC; 