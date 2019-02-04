DROP TABLE IF EXISTS vetd.round_product;
--;;
CREATE TABLE vetd.round_product (id bigint NOT NULL, idstr text, created timestamp with time zone, udpated timestamp with time zone, deleted timestamp with time zone, round_id bigint, product_id bigint)
--;;
ALTER TABLE vetd.round_product OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.round_product TO hasura;