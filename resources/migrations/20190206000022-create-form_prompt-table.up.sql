DROP TABLE IF EXISTS vetd.form_prompt;
--;;
CREATE TABLE vetd.form_prompt (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, form_id bigint, prompt_id bigint, sort integer)
--;;
ALTER TABLE vetd.form_prompt OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.form_prompt TO hasura;