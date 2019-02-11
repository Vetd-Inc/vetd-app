DROP TABLE IF EXISTS vetd.admins;
--;;
CREATE TABLE vetd.admins (id bigint NOT NULL, created timestamp with time zone, updated timestamp with time zone, deleted timestamp with time zone, user_id bigint)
--;;
ALTER TABLE vetd.admins OWNER TO vetd