--
-- PostgreSQL database cluster dump
--

SET default_transaction_read_only = off;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Roles
--

CREATE ROLE hasura;
ALTER ROLE hasura WITH SUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md524483b34a37af88aac5145aee74127e9';
CREATE ROLE postgres;
ALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS;
CREATE ROLE vetd;
ALTER ROLE vetd WITH SUPERUSER INHERIT NOCREATEROLE CREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md5ef5c8e367daceb0766b7ab0be670e673';

--
-- Database creation
--

REVOKE CONNECT,TEMPORARY ON DATABASE template1 FROM PUBLIC;
GRANT CONNECT ON DATABASE template1 TO PUBLIC;
CREATE DATABASE vetd1 WITH TEMPLATE = template0 OWNER = vetd;
GRANT CONNECT ON DATABASE vetd1 TO hasura;
ALTER DATABASE vetd1 SET search_path TO 'vetd';
CREATE DATABASE vtmp1 WITH TEMPLATE = template0 OWNER = vetd;
REVOKE CONNECT,TEMPORARY ON DATABASE vtmp1 FROM PUBLIC;
GRANT ALL ON DATABASE vtmp1 TO PUBLIC;

\connect vetd1

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 10.6 (Ubuntu 10.6-0ubuntu0.18.04.1)
-- Dumped by pg_dump version 10.6 (Ubuntu 10.6-0ubuntu0.18.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

CREATE SCHEMA vetd;

ALTER SCHEMA vetd OWNER TO vetd;

ALTER DATABASE vetd1 SET search_path TO vetd, public;
