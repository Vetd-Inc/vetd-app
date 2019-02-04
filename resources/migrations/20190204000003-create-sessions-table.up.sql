DROP TABLE IF EXISTS vetd.sessions;
--;;
CREATE TABLE vetd.sessions (id bigint NOT NULL, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, token text, user_id bigint)
--;;
ALTER TABLE vetd.sessions OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.sessions TO hasura;
--;;
GRANT INSERT ON TABLE vetd.sessions TO hasura;