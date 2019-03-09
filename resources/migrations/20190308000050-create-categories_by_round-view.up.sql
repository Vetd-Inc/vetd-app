DROP VIEW IF EXISTS vetd.categories_by_round;
--;;
CREATE VIEW vetd.categories_by_round AS SELECT "rc"."id" AS "rcid", "rc"."round_id", "c"."id", "c"."idstr", "c"."created", "c"."cname" FROM "round_category" "rc" INNER JOIN "categories" "c" ON "c"."id" = "rc"."category_id";
--;;
ALTER VIEW vetd.categories_by_round OWNER TO vetd
--;;
GRANT SELECT ON vetd.categories_by_round TO hasura;