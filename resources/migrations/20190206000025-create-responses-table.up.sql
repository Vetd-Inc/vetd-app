DROP TABLE IF EXISTS vetd.responses;
--;;
CREATE TABLE vetd.responses (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, prompt_id bigint, user_id bigint, notes text)
--;;
ALTER TABLE vetd.responses OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.responses TO hasura;