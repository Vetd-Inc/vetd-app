DROP VIEW IF EXISTS vetd.response_prompt_by_doc;
--;;
CREATE OR REPLACE VIEW vetd.response_prompt_by_doc AS SELECT "dr"."id" AS "drid", "dr"."doc_id" AS "doc_id", "dr"."id" AS "ref_id", "dr"."deleted" AS "ref_deleted", "r"."id", "r"."idstr", "r"."created", "r"."updated", "r"."deleted", "r"."prompt_id", "r"."user_id", "r"."notes", "p"."idstr" AS "prompt_idstr", "p"."created" AS "prompt_created", "p"."updated" AS "prompt_updated", "p"."deleted" AS "prompt_deleted", "p"."prompt" AS "prompt_prompt", "p"."term" AS "prompt_term", "p"."descr" AS "prompt_descr" FROM "doc_resp" "dr" INNER JOIN "responses" "r" ON "r"."id" = "dr"."resp_id" INNER JOIN "prompts" "p" ON "p"."id" = "r"."prompt_id";
--;;
ALTER VIEW vetd.response_prompt_by_doc OWNER TO vetd
--;;
GRANT SELECT ON vetd.response_prompt_by_doc TO hasura;