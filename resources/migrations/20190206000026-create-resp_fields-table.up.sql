DROP TABLE IF EXISTS vetd.resp_fields;
--;;
CREATE TABLE vetd.resp_fields (deleted timestamp with time zone, pf_id bigint, updated timestamp with time zone, created timestamp with time zone, idstr text, id bigint NOT NULL, nval numeric(12,3), sval text, dval timestamp with time zone, resp_id bigint)
--;;
ALTER TABLE vetd.resp_fields OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.resp_fields TO hasura;