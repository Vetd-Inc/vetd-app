DROP TABLE IF EXISTS vetd.responses;
--;;
CREATE TABLE vetd.responses (deleted timestamp with time zone, updated timestamp with time zone, org_id bigint, created timestamp with time zone, prompt_id bigint, idstr text, id bigint NOT NULL, notes text, user_id bigint)
--;;
ALTER TABLE vetd.responses OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.responses TO hasura;