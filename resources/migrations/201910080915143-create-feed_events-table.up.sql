DROP TABLE IF EXISTS vetd.feed_events;
--;;
CREATE TABLE vetd.feed_events (deleted timestamp with time zone, journal_entry_id bigint, updated timestamp with time zone, org_id bigint, journal_entry_created timestamp with time zone, created timestamp with time zone, idstr text, id bigint NOT NULL, ftype text, data jsonb)
--;;
ALTER TABLE vetd.feed_events OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.feed_events TO hasura;