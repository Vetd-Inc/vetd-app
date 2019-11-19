DROP TABLE IF EXISTS vetd.messages;
--;;
CREATE TABLE vetd.messages (deleted timestamp with time zone, updated timestamp with time zone, org_id bigint, created timestamp with time zone, idstr text, thread_id bigint, id bigint NOT NULL, user_id bigint, text text)
--;;
ALTER TABLE vetd.messages OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.messages TO hasura;