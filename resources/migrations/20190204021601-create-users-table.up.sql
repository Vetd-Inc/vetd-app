DROP TABLE IF EXISTS vetd.users;
--;;
CREATE TABLE vetd.users (id bigint NOT NULL, idstr text, created timestamp with time zone, udpated timestamp with time zone, deleted timestamp with time zone, uname text, email text, pwd text)
--;;
ALTER TABLE vetd.users OWNER TO vetd
--;;
GRANT SELECT ON TABLE vetd.users TO hasura;
--;;
GRANT INSERT ON TABLE vetd.users TO hasura;