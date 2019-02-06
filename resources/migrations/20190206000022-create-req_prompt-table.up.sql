DROP TABLE IF EXISTS vetd.req_prompt;
--;;
CREATE TABLE vetd.req_prompt (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, req_id bigint, prompt_id bigint, sort integer)
--;;
ALTER TABLE vetd.req_prompt OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.req_prompt TO hasura;