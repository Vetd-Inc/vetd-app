DROP TABLE IF EXISTS vetd.enum_vals;
--;;
CREATE TABLE vetd.enum_vals (deleted timestamp with time zone, updated timestamp with time zone, value text, created timestamp with time zone, enum_id bigint, idstr text, label text, id bigint NOT NULL, fsubtype text)
--;;
ALTER TABLE vetd.enum_vals OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.enum_vals TO hasura;