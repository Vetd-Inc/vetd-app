DROP VIEW IF EXISTS vetd.rounds_by_category;
--;;
CREATE OR REPLACE VIEW vetd.rounds_by_category AS SELECT "rc"."id" AS "rcid", "rc"."category_id", "rc"."id" AS "ref_id", "rc"."deleted" AS "ref_deleted", "r"."id", "r"."idstr", "r"."created", "r"."deleted", "r"."buyer_id", "r"."status", "r"."req_form_template_id", "r"."doc_id" FROM "round_category" "rc" INNER JOIN "rounds" "r" ON "r"."id" = "rc"."round_id";
--;;
ALTER VIEW vetd.rounds_by_category OWNER TO vetd
--;;
GRANT SELECT ON vetd.rounds_by_category TO hasura;