DROP TABLE IF EXISTS vetd.prompts;
--;;
CREATE TABLE vetd.prompts (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, prompt text, descr text)
--;;
ALTER TABLE vetd.prompts OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.prompts TO hasura;