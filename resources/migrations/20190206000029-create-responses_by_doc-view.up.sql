DROP VIEW IF EXISTS vetd.responses_by_doc;
--;;
CREATE VIEW vetd.responses_by_doc AS SELECT "dr"."id" AS "drid", "dr"."doc_id", "r"."id", "r"."idstr", "r"."created", "r"."updated", "r"."deleted", "r"."prompt_id", "r"."user_id", "r"."org_id" FROM "doc_resp" "dr" INNER JOIN "responses" "r" ON "r"."id" = "dr"."resp_id";
--;;
ALTER VIEW vetd.responses_by_doc OWNER TO vetd
--;;
GRANT SELECT ON vetd.responses_by_doc TO hasura;