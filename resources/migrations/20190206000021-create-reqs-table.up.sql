DROP TABLE IF EXISTS vetd.reqs;
--;;
CREATE TABLE vetd.reqs (deleted timestamp with time zone, to_user bigint, updated timestamp with time zone, created timestamp with time zone, to_org bigint, idstr text, from_user bigint, title text, status text, id bigint NOT NULL, notes text, req_template_id bigint, descr text, from_org bigint)
--;;
ALTER TABLE vetd.reqs OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.reqs TO hasura;