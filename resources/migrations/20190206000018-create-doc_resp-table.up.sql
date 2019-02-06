DROP TABLE IF EXISTS vetd.doc_resp;
--;;
CREATE TABLE vetd.doc_resp (id bigint NOT NULL, idstr text, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, doc_id bigint, resp_id bigint)
--;;
ALTER TABLE vetd.doc_resp OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.doc_resp TO hasura;