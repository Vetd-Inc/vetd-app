DROP TABLE IF EXISTS vetd.enum_vals;
--;;
CREATE TABLE vetd.enum_vals (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, enum_id bigint, value text)
--;;
ALTER TABLE vetd.enum_vals OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.enum_vals TO hasura;