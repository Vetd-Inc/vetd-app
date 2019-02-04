DROP TABLE IF EXISTS vetd.product_categories;
--;;
CREATE TABLE vetd.product_categories (id bigint NOT NULL, idstr text, created timestamp with time zone, udpated timestamp with time zone, deleted timestamp with time zone, prod_id bigint, cat_id bigint)
--;;
ALTER TABLE vetd.product_categories OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.product_categories TO hasura;