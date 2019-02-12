DROP VIEW IF EXISTS vetd.rounds_by_category;
--;;
CREATE VIEW vetd.rounds_by_category AS SELECT "rc"."id" AS "rcid", "rc"."category_id", "r"."id", "r"."idstr", "r"."created", "r"."buyer_id", "r"."status" FROM "round_category" "rc" INNER JOIN "rounds" "r" ON "r"."id" = "rc"."round_id";
--;;
ALTER VIEW vetd.rounds_by_category OWNER TO vetd
--;;
GRANT SELECT ON vetd.rounds_by_category TO hasura;