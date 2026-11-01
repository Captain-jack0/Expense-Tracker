-- V2: per-user token version. Embedded in refresh tokens; bumping it on
-- logout revokes every refresh token issued before that point.
alter table users add column token_version integer default 0 not null;
