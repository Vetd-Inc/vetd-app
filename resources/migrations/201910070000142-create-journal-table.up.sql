DROP TABLE IF EXISTS vetd.journal;
--;;
CREATE TABLE vetd.journal (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, session_id bigint, jtype text, entry jsonb)
--;;
ALTER TABLE vetd.journal OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.journal TO hasura;