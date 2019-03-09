DROP TABLE IF EXISTS vetd.form_template_prompt;
--;;
CREATE TABLE vetd.form_template_prompt (deleted timestamp with time zone, updated timestamp with time zone, form_template_id bigint, created timestamp with time zone, prompt_id bigint, idstr text, term text, id bigint NOT NULL, sort integer)
--;;
ALTER TABLE vetd.form_template_prompt OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.form_template_prompt TO hasura;