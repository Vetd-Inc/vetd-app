DROP TABLE IF EXISTS vetd.req_templates;
--;;
CREATE TABLE vetd.req_templates (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, title text, rtype text, descr text)
--;;
ALTER TABLE vetd.req_templates OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.req_templates TO hasura;