DROP TABLE IF EXISTS vetd.stack_items;
--;;
CREATE TABLE vetd.stack_items (deleted timestamp with time zone, price_period text, product_id bigint, updated timestamp with time zone, price_amount integer, buyer_id bigint, created timestamp with time zone, idstr text, renewal_date timestamp with time zone, id bigint NOT NULL, renewal_reminder bool, statue text, rating integer)
--;;
ALTER TABLE vetd.stack_items OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.stack_items TO hasura;