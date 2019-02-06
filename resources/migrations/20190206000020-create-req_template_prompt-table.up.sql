DROP TABLE IF EXISTS vetd.req_template_prompt;
--;;
CREATE TABLE vetd.req_template_prompt (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, prompt_id bigint, sort integer)
--;;
ALTER TABLE vetd.req_template_prompt OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.req_template_prompt TO hasura;