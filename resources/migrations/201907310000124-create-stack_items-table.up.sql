DROP TABLE IF EXISTS vetd.stack_items;
--;;
CREATE TABLE vetd.stack_items (deleted timestamp with time zone, price_period text, product_id bigint, updated timestamp with time zone, price_amount numeric(12,2), buyer_id bigint, created timestamp with time zone, idstr text, status text, renewal_date timestamp with time zone, id bigint NOT NULL, renewal_reminder bool, rating numeric(12,2))
--;;
ALTER TABLE vetd.stack_items OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.stack_items TO hasura;