DROP TABLE IF EXISTS vetd.docs;
--;;
CREATE TABLE vetd.docs (deleted timestamp with time zone, dtype text, updated timestamp with time zone, created timestamp with time zone, idstr text, title text, from_user_id bigint, to_org_id bigint, dsubtype text, id bigint NOT NULL, notes text, form_id bigint, from_org_id bigint, descr text, to_user_id bigint, subject bigint)
--;;
ALTER TABLE vetd.docs OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.docs TO hasura;