DROP VIEW IF EXISTS vetd.products_by_round;
--;;
CREATE VIEW vetd.products_by_round AS SELECT "rp"."id" AS "rpid", "rp"."round_id", "p"."id", "p"."idstr", "p"."created", "p"."pname", "p"."vendor_id", "p"."short_desc", "p"."long_desc", "p"."logo", "p"."url" FROM "round_product" "rp" INNER JOIN "products" "p" ON "p"."id" = "rp"."product_id";
--;;
ALTER VIEW vetd.products_by_round OWNER TO vetd
--;;
GRANT SELECT ON vetd.products_by_round TO hasura;