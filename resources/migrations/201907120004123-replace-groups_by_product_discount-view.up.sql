DROP VIEW IF EXISTS vetd.groups_by_product_discount;
--;;
CREATE OR REPLACE VIEW vetd.groups_by_product_discount AS SELECT "gd"."id" AS "ref_id", "gd"."deleted" AS "ref_deleted", "gd"."descr" AS "group_discount_descr", "gd"."product_id", "g"."id", "g"."idstr", "g"."created", "g"."deleted", "g"."gname", "g"."admin_org_id" FROM "group_discounts" "gd" INNER JOIN "groups" "g" ON "g"."id" = "gd"."group_id";
--;;
ALTER VIEW vetd.groups_by_product_discount OWNER TO vetd
--;;
GRANT SELECT ON vetd.groups_by_product_discount TO hasura;