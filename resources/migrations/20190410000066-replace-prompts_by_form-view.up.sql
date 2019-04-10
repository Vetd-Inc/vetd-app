DROP VIEW IF EXISTS vetd.prompts_by_form;
--;;
CREATE OR REPLACE VIEW vetd.prompts_by_form AS SELECT "fp"."id" AS "rpid", "fp"."id" AS "ref_id", "fp"."deleted" AS "ref_deleted", "fp"."form_id", "fp"."sort", "p"."id", "p"."idstr", "p"."created", "p"."updated", "p"."deleted", "p"."prompt", "p"."term", "p"."descr" FROM "form_prompt" "fp" INNER JOIN "prompts" "p" ON "p"."id" = "fp"."prompt_id";
--;;
ALTER VIEW vetd.prompts_by_form OWNER TO vetd
--;;
GRANT SELECT ON vetd.prompts_by_form TO hasura;