DROP TABLE IF EXISTS vetd.email_sent_log;
--;;
CREATE TABLE vetd.email_sent_log (deleted timestamp with time zone, etype text, updated timestamp with time zone, org_id bigint, created timestamp with time zone, idstr text, id bigint NOT NULL, user_id bigint, data jsonb)
--;;
ALTER TABLE vetd.email_sent_log OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.email_sent_log TO hasura;