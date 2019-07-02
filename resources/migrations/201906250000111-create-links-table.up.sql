DROP TABLE IF EXISTS vetd.links;
--;;
CREATE TABLE vetd.links (deleted timestamp with time zone, max_uses_action integer, expires_read timestamp with time zone, key text, uses_read integer, input_data text, updated timestamp with time zone, output_data text, created timestamp with time zone, idstr text, id bigint NOT NULL, uses_action integer, expires_action timestamp with time zone, max_uses_read integer, cmd text)
--;;
ALTER TABLE vetd.links OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.links TO hasura;