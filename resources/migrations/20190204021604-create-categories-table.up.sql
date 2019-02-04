DROP TABLE IF EXISTS vetd.categories;
--;;
CREATE TABLE vetd.categories (id bigint NOT NULL, idstr text, cname text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone)
--;;
ALTER TABLE vetd.categories OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.categories TO hasura;