DROP VIEW IF EXISTS vetd.top_products_by_group;
--;;
CREATE OR REPLACE VIEW vetd.top_products_by_group AS SELECT "gom"."group_id" AS "group_id", "si"."product_id" AS "product_id", count("si"."id") AS "count_stack_items" FROM "group_org_memberships" "gom" INNER JOIN "stack_items" "si" ON ("si"."buyer_id" = "gom"."org_id" AND "si"."deleted" IS NULL) GROUP BY "gom"."group_id", "si"."product_id";
--;;
ALTER VIEW vetd.top_products_by_group OWNER TO vetd
--;;
GRANT SELECT ON vetd.top_products_by_group TO hasura;