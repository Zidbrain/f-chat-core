--
-- PostgreSQL database dump
--

-- Dumped from database version 16.2 (Debian 16.2-1.pgdg120+2)
-- Dumped by pg_dump version 16.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 4 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: pg_database_owner
--

CREATE SCHEMA IF NOT EXISTS public;


ALTER SCHEMA public OWNER TO pg_database_owner;

--
-- TOC entry 3404 (class 0 OID 0)
-- Dependencies: 4
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: pg_database_owner
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- TOC entry 222 (class 1255 OID 32796)
-- Name: clean_up_messages(); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.clean_up_messages()
    LANGUAGE plpgsql
    AS $$
	BEGIN
	delete from public.message where message_received;
	END;
$$;


ALTER PROCEDURE public.clean_up_messages() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 220 (class 1259 OID 16469)
-- Name: contact; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.contact (
    user_id uuid NOT NULL,
    contact_user_id uuid NOT NULL
);


ALTER TABLE public.contact OWNER TO postgres;

--
-- TOC entry 217 (class 1259 OID 16426)
-- Name: conversation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.conversation (
    id uuid NOT NULL
);


ALTER TABLE public.conversation OWNER TO postgres;

--
-- TOC entry 216 (class 1259 OID 16398)
-- Name: device; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.device (
    public_key character varying NOT NULL,
    user_id uuid NOT NULL,
    device_refresh_token character varying NOT NULL,
    device_last_online timestamp with time zone NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL
);


ALTER TABLE public.device OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 16431)
-- Name: device_conversation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.device_conversation (
    conversation_id uuid NOT NULL,
    device_id uuid NOT NULL,
    conversation_symmetric_key_for_device character varying NOT NULL
);


ALTER TABLE public.device_conversation OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16452)
-- Name: message; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message (
    id uuid NOT NULL,
    conversation_id uuid NOT NULL,
    message_content bytea NOT NULL,
    message_sent_at timestamp with time zone NOT NULL,
    message_status_id integer NOT NULL,
    device_id uuid NOT NULL
);


ALTER TABLE public.message OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 32830)
-- Name: message_status; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_status (
    id integer NOT NULL,
    name character varying NOT NULL,
    comment character varying
);


ALTER TABLE public.message_status OWNER TO postgres;

--
-- TOC entry 215 (class 1259 OID 16389)
-- Name: user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."user" (
    id uuid NOT NULL,
    user_email character varying NOT NULL,
    user_display_name character varying NOT NULL
);


ALTER TABLE public."user" OWNER TO postgres;

--
-- TOC entry 3243 (class 2606 OID 16473)
-- Name: contact contact_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contact
    ADD CONSTRAINT contact_pk PRIMARY KEY (user_id, contact_user_id);


--
-- TOC entry 3237 (class 2606 OID 16430)
-- Name: conversation conversation_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.conversation
    ADD CONSTRAINT conversation_pk PRIMARY KEY (id);


--
-- TOC entry 3239 (class 2606 OID 32819)
-- Name: device_conversation device_conversation_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device_conversation
    ADD CONSTRAINT device_conversation_pk PRIMARY KEY (conversation_id, device_id);


--
-- TOC entry 3233 (class 2606 OID 32815)
-- Name: device device_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device
    ADD CONSTRAINT device_pk PRIMARY KEY (id);


--
-- TOC entry 3235 (class 2606 OID 32817)
-- Name: device device_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device
    ADD CONSTRAINT device_unique UNIQUE (public_key);


--
-- TOC entry 3241 (class 2606 OID 16458)
-- Name: message message_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pk PRIMARY KEY (id);


--
-- TOC entry 3245 (class 2606 OID 32836)
-- Name: message_status message_status_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_status
    ADD CONSTRAINT message_status_pk PRIMARY KEY (id);


--
-- TOC entry 3247 (class 2606 OID 32838)
-- Name: message_status message_status_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_status
    ADD CONSTRAINT message_status_unique UNIQUE (name);


--
-- TOC entry 3229 (class 2606 OID 16397)
-- Name: user user_email_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_email_unique UNIQUE (user_email);


--
-- TOC entry 3231 (class 2606 OID 16395)
-- Name: user user_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_pk PRIMARY KEY (id);


--
-- TOC entry 3254 (class 2606 OID 16474)
-- Name: contact contact_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contact
    ADD CONSTRAINT contact_user_fk FOREIGN KEY (user_id) REFERENCES public."user"(id);


--
-- TOC entry 3255 (class 2606 OID 16479)
-- Name: contact contact_user_fk_1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contact
    ADD CONSTRAINT contact_user_fk_1 FOREIGN KEY (contact_user_id) REFERENCES public."user"(id);


--
-- TOC entry 3249 (class 2606 OID 16447)
-- Name: device_conversation device_conversation_conversation_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device_conversation
    ADD CONSTRAINT device_conversation_conversation_fk FOREIGN KEY (conversation_id) REFERENCES public.conversation(id);


--
-- TOC entry 3250 (class 2606 OID 32820)
-- Name: device_conversation device_conversation_device_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device_conversation
    ADD CONSTRAINT device_conversation_device_fk FOREIGN KEY (device_id) REFERENCES public.device(id);


--
-- TOC entry 3248 (class 2606 OID 16405)
-- Name: device device_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.device
    ADD CONSTRAINT device_user_fk FOREIGN KEY (user_id) REFERENCES public."user"(id);


--
-- TOC entry 3251 (class 2606 OID 16464)
-- Name: message message_conversation_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_conversation_fk FOREIGN KEY (conversation_id) REFERENCES public.conversation(id);


--
-- TOC entry 3252 (class 2606 OID 32825)
-- Name: message message_device_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_device_fk FOREIGN KEY (id) REFERENCES public.device(id);


--
-- TOC entry 3253 (class 2606 OID 32845)
-- Name: message message_message_status_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_message_status_fk FOREIGN KEY (message_status_id) REFERENCES public.message_status(id);


--
-- PostgreSQL database dump complete
--

