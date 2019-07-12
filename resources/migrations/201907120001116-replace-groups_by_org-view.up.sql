DROP VIEW IF EXISTS vetd.groups_by_org;
--;;
CREATE OR REPLACE VIEW vetd.groups_by_org AS SELECT "gom"."id" AS "ref_id", "gom"."deleted" AS "ref_deleted", "gom"."org_id", "g"."id", "g"."idstr", "g"."created", "g"."deleted", "g"."gname", "g"."admin_org_id" FROM "group_org_memberships" "gom" INNER JOIN "groups" "g" ON "g"."id" = "gom"."group_id";
--;;
ALTER VIEW vetd.groups_by_org OWNER TO vetd
--;;
GRANT SELECT ON vetd.groups_by_org TO hasura;