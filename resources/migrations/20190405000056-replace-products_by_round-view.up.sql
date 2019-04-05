DROP VIEW IF EXISTS vetd.products_by_round;
--;;
CREATE OR REPLACE VIEW vetd.products_by_round AS SELECT "rp"."id" AS "rpid", "rp"."id" AS "ref_id", "rp"."deleted" AS "ref_deleted", "rp"."round_id", "p"."id", "p"."idstr", "p"."created", "p"."pname", "p"."vendor_id", "p"."short_desc", "p"."long_desc", "p"."logo", "p"."url", "p"."deleted" FROM "round_product" "rp" INNER JOIN "products" "p" ON "p"."id" = "rp"."product_id";
--;;
ALTER VIEW vetd.products_by_round OWNER TO vetd
--;;
GRANT SELECT ON vetd.products_by_round TO hasura;