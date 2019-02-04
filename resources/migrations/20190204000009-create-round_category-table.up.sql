DROP TABLE IF EXISTS vetd.round_category;
--;;
CREATE TABLE vetd.round_category (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, round_id bigint, category_id bigint)
--;;
ALTER TABLE vetd.round_category OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.round_category TO hasura;