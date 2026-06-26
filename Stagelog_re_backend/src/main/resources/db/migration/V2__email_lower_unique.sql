-- ============================================================================
-- V2 — 이메일 대소문자 무시 유일성 보장 (#2 email normalization, A안: 앱 + DB)
--
-- 도메인(User.normalizeEmail)이 저장/조회 시 lower+trim 정규화를 강제하지만,
-- 팩토리를 우회하는 경로(직접 SQL/백필 등)까지 막기 위한 DB 최종 방어선.
-- V1의 UNIQUE(email)와 함께 동작 — 저장값이 항상 정규화되므로 정상 경로에선 실질 중복이지만,
-- 비정규화 INSERT가 끼어들 경우 lower(email) 인덱스가 'Foo@x.com' vs 'foo@x.com'을 차단한다.
-- ============================================================================
CREATE UNIQUE INDEX uk_users_email_lower ON users (lower(email));
