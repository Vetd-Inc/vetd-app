DROP TABLE IF EXISTS vetd.prompt_fields;
--;;
CREATE TABLE vetd.prompt_fields (deleted timestamp with time zone, fname text, dtype text, updated timestamp with time zone, created timestamp with time zone, prompt_id bigint, idstr text, id bigint NOT NULL, list_qm boolean, descr text, sort integer)
--;;
ALTER TABLE vetd.prompt_fields OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.prompt_fields TO hasura;