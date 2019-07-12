DROP TABLE IF EXISTS vetd.groups;
--;;
CREATE TABLE vetd.groups (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, gname text, admin_org_id bigint)
--;;
ALTER TABLE vetd.groups OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.groups TO hasura;