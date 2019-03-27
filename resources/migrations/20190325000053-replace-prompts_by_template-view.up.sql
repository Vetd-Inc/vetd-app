DROP VIEW IF EXISTS vetd.prompts_by_template;
--;;
CREATE OR REPLACE VIEW vetd.prompts_by_template AS SELECT "rp"."id" AS "rpid", "rp"."deleted" AS "rp_deleted", "rp"."form_template_id", "rp"."sort", "p"."id", "p"."idstr", "p"."created", "p"."updated", "p"."deleted", "p"."prompt", "p"."descr" FROM "form_template_prompt" "rp" INNER JOIN "prompts" "p" ON "p"."id" = "rp"."prompt_id";
--;;
ALTER VIEW vetd.prompts_by_template OWNER TO vetd
--;;
GRANT SELECT ON vetd.prompts_by_template TO hasura;