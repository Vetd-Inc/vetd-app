DROP TABLE IF EXISTS vetd.reqs;
--;;
CREATE TABLE vetd.reqs (deleted timestamp with time zone, updated timestamp with time zone, created timestamp with time zone, topic bigint, idstr text, title text, from_user_id bigint, to_org_id bigint, rsubtype text, status text, rtype text, id bigint NOT NULL, notes text, from_org_id bigint, req_template_id bigint, descr text, to_user_id bigint)
--;;
ALTER TABLE vetd.reqs OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.reqs TO hasura;