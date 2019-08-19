DROP VIEW IF EXISTS vetd.agg_group_prod_price;
--;;
CREATE OR REPLACE VIEW vetd.agg_group_prod_price AS SELECT "gom"."group_id" AS "group_id", "si"."product_id" AS "product_id", percentile_disc (0.5) WITHIN GROUP (ORDER BY (CASE WHEN si.price_period = 'monthly' THEN (si.price_amount * 12) WHEN si.price_period = 'free' THEN 0 ELSE si.price_amount END)) AS "median_price" FROM "group_org_memberships" "gom" INNER JOIN "stack_items" "si" ON ("si"."buyer_id" = "gom"."org_id" AND "si"."deleted" IS NULL) GROUP BY "gom"."group_id", "si"."product_id";
--;;
ALTER VIEW vetd.agg_group_prod_price OWNER TO vetd
--;;
GRANT SELECT ON vetd.agg_group_prod_price TO hasura;