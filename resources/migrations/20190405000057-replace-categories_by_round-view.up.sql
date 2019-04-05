DROP VIEW IF EXISTS vetd.categories_by_round;
--;;
CREATE OR REPLACE VIEW vetd.categories_by_round AS SELECT "rc"."id" AS "rcid", "rc"."round_id", "rc"."id" AS "ref_id", "rc"."deleted" AS "ref_deleted", "c"."id", "c"."idstr", "c"."created", "c"."cname", "c"."deleted" FROM "round_category" "rc" INNER JOIN "categories" "c" ON "c"."id" = "rc"."category_id";
--;;
ALTER VIEW vetd.categories_by_round OWNER TO vetd
--;;
GRANT SELECT ON vetd.categories_by_round TO hasura;