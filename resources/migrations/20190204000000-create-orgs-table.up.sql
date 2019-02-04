DROP TABLE IF EXISTS vetd.orgs;
--;;
CREATE TABLE vetd.orgs (long_desc text, deleted timestamp with time zone, oname text, updated timestamp with time zone, created timestamp with time zone, idstr text, id bigint NOT NULL, vendor_qm boolean, url text, short_desc text, buyer_qm boolean)
--;;
ALTER TABLE vetd.orgs OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.orgs TO hasura;