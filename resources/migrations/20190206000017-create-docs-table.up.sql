DROP TABLE IF EXISTS vetd.docs;
--;;
CREATE TABLE vetd.docs (deleted timestamp with time zone, to_user bigint, updated timestamp with time zone, created timestamp with time zone, to_org bigint, idstr text, from_user bigint, title text, id bigint NOT NULL, notes text, descr text, from_org bigint)
--;;
ALTER TABLE vetd.docs OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.docs TO hasura;