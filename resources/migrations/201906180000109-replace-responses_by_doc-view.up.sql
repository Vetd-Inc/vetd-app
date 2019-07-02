DROP VIEW IF EXISTS vetd.responses_by_doc;
--;;
CREATE OR REPLACE VIEW vetd.responses_by_doc AS SELECT "dr"."id" AS "drid", "dr"."doc_id", "dr"."id" AS "ref_id", "dr"."deleted" AS "ref_deleted", "r"."id", "r"."idstr", "r"."created", "r"."updated", "r"."deleted", "r"."prompt_id", "r"."user_id", "r"."notes", "r"."subject" FROM "doc_resp" "dr" INNER JOIN "responses" "r" ON "r"."id" = "dr"."resp_id";
--;;
ALTER VIEW vetd.responses_by_doc OWNER TO vetd
--;;
GRANT SELECT ON vetd.responses_by_doc TO hasura;