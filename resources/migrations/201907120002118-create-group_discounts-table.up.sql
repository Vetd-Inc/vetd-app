DROP TABLE IF EXISTS vetd.group_discounts;
--;;
CREATE TABLE vetd.group_discounts (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, group_id bigint, product_id bigint, descr text)
--;;
ALTER TABLE vetd.group_discounts OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.group_discounts TO hasura;