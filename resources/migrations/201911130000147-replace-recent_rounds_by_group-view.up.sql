DROP VIEW IF EXISTS vetd.recent_rounds_by_group;
--;;
CREATE OR REPLACE VIEW vetd.recent_rounds_by_group AS SELECT "gom"."group_id" AS "group_id", "r"."id" AS "round_id" FROM "group_org_memberships" "gom" INNER JOIN "rounds" "r" ON ("r"."buyer_id" = "gom"."org_id" AND "r"."deleted" IS NULL AND ("r"."status" in ('in-progress', 'complete'))) GROUP BY "gom"."group_id", "r"."id" ORDER BY "r"."created" DESC;
--;;
ALTER VIEW vetd.recent_rounds_by_group OWNER TO vetd
--;;
GRANT SELECT ON vetd.recent_rounds_by_group TO hasura;