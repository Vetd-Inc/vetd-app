DROP VIEW IF EXISTS vetd.prompts_by_form;
--;;
CREATE VIEW vetd.prompts_by_form AS SELECT "rp"."id" AS "rpid", "rp"."form_id", "rp"."sort", "p"."id", "p"."idstr", "p"."created", "p"."updated", "p"."deleted", "p"."prompt", "p"."descr" FROM "form_prompt" "rp" INNER JOIN "prompts" "p" ON "p"."id" = "rp"."prompt_id";
--;;
ALTER VIEW vetd.prompts_by_form OWNER TO vetd
--;;
GRANT SELECT ON vetd.prompts_by_form TO hasura;