DROP TABLE IF EXISTS vetd.group_org_memberships;
--;;
CREATE TABLE vetd.group_org_memberships (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, group_id bigint, org_id bigint)
--;;
ALTER TABLE vetd.group_org_memberships OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.group_org_memberships TO hasura;