DROP TABLE IF EXISTS vetd.forms;
--;;
CREATE TABLE vetd.forms (deleted timestamp with time zone, updated timestamp with time zone, form_template_id bigint, created timestamp with time zone, idstr text, title text, from_user_id bigint, to_org_id bigint, status text, id bigint NOT NULL, notes text, from_org_id bigint, descr text, to_user_id bigint, ftype text, subject bigint, fsubtype text)
--;;
ALTER TABLE vetd.forms OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.forms TO hasura;