DROP TABLE IF EXISTS vetd.rounds;
--;;
CREATE TABLE vetd.rounds (active_qm boolean, deleted timestamp with time zone, updated timestamp with time zone, buyer_id bigint, created timestamp with time zone, idstr text, status text, id bigint NOT NULL, user_id bigint)
--;;
ALTER TABLE vetd.rounds OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.rounds TO hasura;