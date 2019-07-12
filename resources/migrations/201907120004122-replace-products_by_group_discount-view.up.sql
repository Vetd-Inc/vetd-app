DROP VIEW IF EXISTS vetd.products_by_group_discount;
--;;
CREATE OR REPLACE VIEW vetd.products_by_group_discount AS SELECT "gd"."id" AS "ref_id", "gd"."deleted" AS "ref_deleted", "gd"."descr" AS "group_discount_descr", "gd"."descr", "gd"."group_id", "p"."id", "p"."idstr", "p"."created", "p"."deleted", "p"."pname", "p"."vendor_id", "p"."short_desc", "p"."long_desc", "p"."logo", "p"."url", "p"."profile_doc_id" FROM "group_discounts" "gd" INNER JOIN "products" "p" ON "p"."id" = "gd"."product_id";
--;;
ALTER VIEW vetd.products_by_group_discount OWNER TO vetd
--;;
GRANT SELECT ON vetd.products_by_group_discount TO hasura;