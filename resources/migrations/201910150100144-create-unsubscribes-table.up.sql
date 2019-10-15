DROP TABLE IF EXISTS vetd.unsubscribes;
--;;
CREATE TABLE vetd.unsubscribes (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, user_id bigint, org_id bigint, etype text)
--;;
ALTER TABLE vetd.unsubscribes OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.unsubscribes TO hasura;