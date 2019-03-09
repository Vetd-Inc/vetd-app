DROP TABLE IF EXISTS vetd.products;
--;;
CREATE TABLE vetd.products (long_desc text, deleted timestamp with time zone, profile_doc_id bigint, logo text, updated timestamp with time zone, created timestamp with time zone, idstr text, vendor_id bigint, id bigint NOT NULL, url text, short_desc text, pname text)
--;;
ALTER TABLE vetd.products OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.products TO hasura;