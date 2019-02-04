DROP TABLE IF EXISTS vetd.memberships;
--;;
CREATE TABLE vetd.memberships (id bigint NOT NULL, idstr text, created timestamp with time zone, udpated timestamp with time zone, deleted timestamp with time zone, org_id bigint, user_id bigint)
--;;
ALTER TABLE vetd.memberships OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.memberships TO hasura;