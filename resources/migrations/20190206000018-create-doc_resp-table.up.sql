DROP TABLE IF EXISTS vetd.doc_resp;
--;;
CREATE TABLE vetd.doc_resp (deleted timestamp with time zone, replaced_by_id bigint, doc_id bigint, updated timestamp with time zone, created timestamp with time zone, idstr text, id bigint NOT NULL, user_id bigint, resp_id bigint)
--;;
ALTER TABLE vetd.doc_resp OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.doc_resp TO hasura;