DROP VIEW IF EXISTS vetd.response_prompt;
--;;
CREATE OR REPLACE VIEW vetd.response_prompt AS SELECT "r"."id", "r"."idstr", "r"."created", "r"."updated", "r"."deleted", "r"."prompt_id", "r"."user_id", "r"."notes", "p"."idstr" AS "prompt_idstr", "p"."created" AS "prompt_created", "p"."updated" AS "prompt_updated", "p"."deleted" AS "prompt_deleted", "p"."prompt" AS "prompt_prompt", "p"."term" AS "prompt_term", "p"."descr" AS "prompt_descr" FROM "responses" "r" INNER JOIN "prompts" "p" ON "p"."id" = "r"."prompt_id";
--;;
ALTER VIEW vetd.response_prompt OWNER TO vetd
--;;
GRANT SELECT ON vetd.response_prompt TO hasura;