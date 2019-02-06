DROP TABLE IF EXISTS vetd.req_template;
--;;
CREATE TABLE vetd.req_template (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, title text)
--;;
ALTER TABLE vetd.req_template OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.req_template TO hasura;