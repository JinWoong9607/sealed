CREATE TABLE users (
                       id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       nickname   VARCHAR(50)  NOT NULL,
                       created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE social_account (
                                id               BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id          BIGINT       NOT NULL REFERENCES users (id),
                                provider         VARCHAR(20)  NOT NULL,
                                provider_user_id VARCHAR(100) NOT NULL,
                                created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                UNIQUE (provider, provider_user_id)
);