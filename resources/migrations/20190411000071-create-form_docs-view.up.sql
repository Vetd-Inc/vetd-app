DROP VIEW IF EXISTS vetd.form_docs;
--;;
CREATE VIEW vetd.form_docs AS SELECT "f"."id", "f"."idstr", "f"."created", "f"."updated", "f"."form_template_id", "f"."title", "f"."subject", "f"."descr", "f"."notes", "f"."ftype", "f"."fsubtype", "f"."from_org_id", "f"."from_user_id", "f"."to_org_id", "f"."to_user_id", "f"."status", "d"."id" AS "doc_id", "d"."idstr" AS "doc_idstr", "d"."created" AS "doc_created", "d"."updated" AS "doc_updated", "d"."deleted" AS "doc_deleted", "d"."title" AS "doc_title", "d"."subject" AS "doc_subject", "d"."descr" AS "doc_descr", "d"."notes" AS "doc_notes", "d"."dtype" AS "doc_dtype", "d"."dsubtype" AS "doc_dsubtype", "d"."from_org_id" AS "doc_from_org_id", "d"."from_user_id" AS "doc_from_user_id", "d"."to_org_id" AS "doc_to_org_id", "d"."to_user_id" AS "doc_to_user_id" FROM "forms" "f" LEFT JOIN "docs" "d" ON ("d"."form_id" = "f"."id" AND "d"."deleted" IS NULL) WHERE "f"."deleted" IS NULL;
--;;
ALTER VIEW vetd.form_docs OWNER TO vetd
--;;
GRANT SELECT ON vetd.form_docs TO hasura;