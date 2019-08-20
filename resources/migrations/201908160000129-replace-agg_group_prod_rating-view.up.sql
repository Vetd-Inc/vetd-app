DROP VIEW IF EXISTS vetd.agg_group_prod_rating;
--;;
CREATE OR REPLACE VIEW vetd.agg_group_prod_rating AS SELECT "gom"."group_id" AS "group_id", "si"."product_id" AS "product_id", count("si"."id") AS "count_stack_items", "si"."rating" AS "rating" FROM "group_org_memberships" "gom" INNER JOIN "stack_items" "si" ON ("si"."buyer_id" = "gom"."org_id" AND "si"."deleted" IS NULL) GROUP BY "gom"."group_id", "si"."product_id", "si"."rating";
--;;
ALTER VIEW vetd.agg_group_prod_rating OWNER TO vetd
--;;
GRANT SELECT ON vetd.agg_group_prod_rating TO hasura;