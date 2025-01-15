DROP TABLE IF EXISTS public.user_password;

CREATE TABLE public.user_password (
	user_id uuid NOT NULL,
	user_password_hash varchar NOT NULL,
	user_password_salt varchar NOT NULL,
	CONSTRAINT user_password_pk PRIMARY KEY (user_id),
	CONSTRAINT user_password_user_fk FOREIGN KEY (user_id) REFERENCES "user"(id)
);