DROP TABLE IF EXISTS vetd.form_prompt;
--;;
CREATE TABLE vetd.form_prompt (deleted timestamp with time zone, replaced_by_id bigint, updated timestamp with time zone, created timestamp with time zone, prompt_id bigint, idstr text, id bigint NOT NULL, form_id bigint, user_id bigint, sort integer)
--;;
ALTER TABLE vetd.form_prompt OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.form_prompt TO hasura;