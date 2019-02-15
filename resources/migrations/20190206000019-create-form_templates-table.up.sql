DROP TABLE IF EXISTS vetd.form_templates;
--;;
CREATE TABLE vetd.form_templates (deleted timestamp with time zone, updated timestamp with time zone, created timestamp with time zone, idstr text, title text, rsubtype text, rtype text, id bigint NOT NULL, descr text)
--;;
ALTER TABLE vetd.form_templates OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.form_templates TO hasura;