-- V1: initial schema (users, categories, sub_categories, transactions).
-- Generated from the JPA entities with Hibernate's MySQLDialect so that
-- spring.jpa.hibernate.ddl-auto=validate passes against it. Existing
-- databases created earlier by ddl-auto=update are baselined at V1
-- (spring.flyway.baseline-on-migrate) so this script is skipped for them.

create table categories (
    is_system bit not null,
    sort_order integer not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    id binary(16) not null,
    user_id binary(16) not null,
    color varchar(255),
    icon varchar(255),
    name varchar(255) not null,
    bucket enum ('INCOME','NEEDS','OTHER','SAVINGS','WANTS') not null,
    kind enum ('EXPENSE','INCOME') not null,
    primary key (id)
) engine=InnoDB;

create table sub_categories (
    is_system bit not null,
    monthly_limit decimal(19,2),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    category_id binary(16) not null,
    id binary(16) not null,
    user_id binary(16) not null,
    color varchar(255),
    icon varchar(255),
    name varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table transactions (
    amount decimal(19,2) not null,
    currency varchar(3) not null,
    installment_no integer,
    transaction_date date not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    category_id binary(16) not null,
    id binary(16) not null,
    installment_plan_id binary(16),
    recurring_rule_id binary(16),
    sub_category_id binary(16),
    user_id binary(16) not null,
    note varchar(500),
    name varchar(255) not null,
    source_type enum ('INSTALLMENT','MANUAL','RECURRING') not null,
    type enum ('EXPENSE','INCOME') not null,
    primary key (id)
) engine=InnoDB;

create table users (
    email_verified bit not null,
    is_active bit not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    id binary(16) not null,
    currency varchar(255) not null,
    email varchar(255) not null,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    password_hash varchar(255) not null,
    timezone varchar(255) not null,
    primary key (id)
) engine=InnoDB;

alter table categories 
   add constraint uq_category_user_name unique (user_id, name);

alter table sub_categories 
   add constraint uq_subcategory_category_name unique (category_id, name);

create index idx_tx_user_date 
   on transactions (user_id, transaction_date);

create index idx_tx_subcategory 
   on transactions (sub_category_id);

alter table users 
   add constraint uq_users_email unique (email);
