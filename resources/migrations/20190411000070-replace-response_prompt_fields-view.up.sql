DROP VIEW IF EXISTS vetd.response_prompt_fields;
--;;
CREATE OR REPLACE VIEW vetd.response_prompt_fields AS SELECT "rf"."id" AS "ref_id", "rf"."deleted" AS "ref_deleted", "rf"."id", "rf"."idstr", "rf"."created", "rf"."updated", "rf"."deleted", "rf"."resp_id", "rf"."pf_id", "rf"."idx", "rf"."sval", "rf"."nval", "rf"."dval", "rf"."jval", "pf"."id" AS "prompt_field_id", "pf"."idstr" AS "prompt_field_idstr", "pf"."created" AS "prompt_field_created", "pf"."updated" AS "prompt_field_updated", "pf"."deleted" AS "prompt_field_deleted", "pf"."fname" AS "prompt_field_fname", "pf"."descr" AS "prompt_field_descr", "pf"."ftype" AS "prompt_field_ftype", "pf"."fsubtype" AS "prompt_field_fsubtype", "pf"."list_qm" AS "prompt_field_list_qm", "pf"."sort" AS "prompt_field_sort" FROM "resp_fields" "rf" INNER JOIN "prompt_fields" "pf" ON "rf"."pf_id" = "pf"."id";
--;;
ALTER VIEW vetd.response_prompt_fields OWNER TO vetd
--;;
GRANT SELECT ON vetd.response_prompt_fields TO hasura;