DROP TABLE IF EXISTS vetd.req_templates;
--;;
CREATE TABLE vetd.req_templates (deleted timestamp with time zone, updated timestamp with time zone, created timestamp with time zone, idstr text, title text, rsubtype text, rtype text, id bigint NOT NULL, descr text)
--;;
ALTER TABLE vetd.req_templates OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.req_templates TO hasura;