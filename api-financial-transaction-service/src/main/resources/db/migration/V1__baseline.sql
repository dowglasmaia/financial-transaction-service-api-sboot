-- BE-02 baseline: schema as mapped by the JPA entities (Agency, User, Account, Transaction),
-- plus integrity constraints. The database is disposable (decision D-23): no data migration.
--
-- Known deviation: transaction.account_id has NO foreign key yet. Mobile recharge still stores the
-- account NUMBER there (defect F-08); spec BE-03 fixes it and adds the foreign key in a later migration.

CREATE TABLE agency (
    id     VARCHAR(255) NOT NULL,
    number VARCHAR(100),
    CONSTRAINT pk_agency PRIMARY KEY (id)
);

CREATE TABLE usuario (
    id           VARCHAR(255) NOT NULL,
    name         VARCHAR(100),
    cpf          VARCHAR(11),
    email        VARCHAR(100),
    phone_number INTEGER,
    area_code    INTEGER,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_cpf UNIQUE (cpf),
    CONSTRAINT uq_usuario_email UNIQUE (email)
);

CREATE TABLE account (
    id        VARCHAR(255) NOT NULL,
    agency_id VARCHAR(255),
    user_id   VARCHAR(255),
    number    VARCHAR(100),
    balance   NUMERIC(19, 2) NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT fk_account_agency FOREIGN KEY (agency_id) REFERENCES agency (id),
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES usuario (id),
    CONSTRAINT uq_account_agency_number UNIQUE (agency_id, number),
    CONSTRAINT ck_account_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE transaction (
    id               VARCHAR(255) NOT NULL,
    date_hour        TIMESTAMP WITH TIME ZONE NOT NULL,
    amount           NUMERIC(19, 2) NOT NULL,
    account_id       VARCHAR(255) NOT NULL,
    agency_id        VARCHAR(255),
    transaction_type VARCHAR(255) NOT NULL,
    CONSTRAINT pk_transaction PRIMARY KEY (id),
    CONSTRAINT fk_transaction_agency FOREIGN KEY (agency_id) REFERENCES agency (id),
    CONSTRAINT ck_transaction_type CHECK (transaction_type IN ('PIX', 'PHONE_RECHARGE', 'WITHDRAW', 'DEPOSIT'))
);

CREATE INDEX idx_transaction_account_date ON transaction (account_id, date_hour DESC);
