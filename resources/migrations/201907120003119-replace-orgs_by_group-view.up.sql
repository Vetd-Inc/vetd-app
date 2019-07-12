DROP VIEW IF EXISTS vetd.orgs_by_group;
--;;
CREATE OR REPLACE VIEW vetd.orgs_by_group AS SELECT "gom"."id" AS "ref_id", "gom"."deleted" AS "ref_deleted", "gom"."group_id", "o"."id", "o"."idstr", "o"."created", "o"."deleted", "o"."oname", "o"."buyer_qm", "o"."vendor_qm", "o"."short_desc", "o"."long_desc", "o"."url", "o"."vendor_profile_doc_id" FROM "group_org_memberships" "gom" INNER JOIN "orgs" "o" ON "o"."id" = "gom"."org_id";
--;;
ALTER VIEW vetd.orgs_by_group OWNER TO vetd
--;;
GRANT SELECT ON vetd.orgs_by_group TO hasura;