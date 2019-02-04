DROP TABLE IF EXISTS vetd.rounds;
--;;
CREATE TABLE vetd.rounds (id bigint NOT NULL, idstr text, created timestamp with time zone, udpated timestamp with time zone, deleted timestamp with time zone, buyer_id bigint, status text)
--;;
ALTER TABLE vetd.rounds OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.rounds TO hasura;