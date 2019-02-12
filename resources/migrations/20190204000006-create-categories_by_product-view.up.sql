DROP VIEW IF EXISTS vetd.categories_by_product;
--;;
CREATE VIEW vetd.categories_by_product AS SELECT "pc"."id" AS "pcid", "pc"."prod_id", "c"."id", "c"."idstr", "c"."cname" FROM "product_categories" "pc" INNER JOIN "categories" "c" ON "c"."id" = "pc"."cat_id";
--;;
ALTER VIEW vetd.categories_by_product OWNER TO vetd
--;;
GRANT SELECT ON vetd.categories_by_product TO hasura;