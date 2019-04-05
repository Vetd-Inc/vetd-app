DROP VIEW IF EXISTS vetd.categories_by_product;
--;;
CREATE OR REPLACE VIEW vetd.categories_by_product AS SELECT "pc"."id" AS "pcid", "pc"."id" AS "ref_id", "pc"."deleted" AS "ref_deleted", "pc"."prod_id", "c"."id", "c"."idstr", "c"."cname", "c"."deleted" FROM "product_categories" "pc" INNER JOIN "categories" "c" ON "c"."id" = "pc"."cat_id";
--;;
ALTER VIEW vetd.categories_by_product OWNER TO vetd
--;;
GRANT SELECT ON vetd.categories_by_product TO hasura;