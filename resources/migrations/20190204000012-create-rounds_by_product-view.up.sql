DROP VIEW IF EXISTS vetd.rounds_by_product;
--;;
CREATE VIEW vetd.rounds_by_product AS SELECT "rp"."id" AS "rcid", "rp"."product_id", "r"."id", "r"."idstr", "r"."created", "r"."buyer_id", "r"."status" FROM "round_product" "rp" INNER JOIN "rounds" "r" ON "r"."id" = "rp"."round_id";
--;;
ALTER VIEW vetd.rounds_by_product OWNER TO vetd
--;;
GRANT SELECT ON vetd.rounds_by_product TO hasura;