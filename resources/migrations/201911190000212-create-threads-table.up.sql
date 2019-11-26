DROP TABLE IF EXISTS vetd.threads;
--;;
CREATE TABLE vetd.threads (deleted timestamp with time zone, updated timestamp with time zone, org_id bigint, created timestamp with time zone, idstr text, title text, group_id bigint, id bigint NOT NULL, user_id bigint)
--;;
ALTER TABLE vetd.threads OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.threads TO hasura;