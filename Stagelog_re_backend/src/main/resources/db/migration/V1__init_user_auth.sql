-- ============================================================================
-- V1 baseline — Stagelog MVP-CORE schema
--
-- 4 테이블:
--   * users / refresh_tokens  ← Email-only User foundation 신규 모델 (Task 2)
--   * login_attempts / kopis_performance  ← brownfield baseline (validate 통과 목적, 후속 Epic 7/9에서 재설계)
--
-- Postgres 16+ has gen_random_uuid() built-in (no pgcrypto needed).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- users — MVP 결정:
--   * 로그인 식별자는 email 단일 (userId 컬럼 없음)
--   * public_id UUID = 외부 노출 안전 식별자 (PK 노출 방지)
--   * consent 3컬럼은 LOCAL 가입자만 채워짐. KAKAO는 NULL 유지 (auto-set 금지).
--   * handle은 v1.1+에서 공개 프로필/멘션용 — 예약 컬럼만, MVP 미사용.
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id                BIGSERIAL    PRIMARY KEY,
    public_id         UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255),
    nickname          VARCHAR(20)  NOT NULL,
    handle            VARCHAR(30)  UNIQUE,
    profile_image_url VARCHAR(500),
    provider          VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id       VARCHAR(255),
    role_type         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    user_status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    -- consent: LOCAL 가입자만 NOT NULL. KAKAO는 NULL.
    -- 도메인(User.createLocalUser)에서 NOT NULL 강제. DB CHECK 미적용 (백필 유연성).
    age_confirmed_at  TIMESTAMPTZ,
    terms_agreed_at   TIMESTAMPTZ,
    terms_version     VARCHAR(20),
    email_notification_enabled BOOLEAN NOT NULL DEFAULT true,
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT uk_user_provider_provider_id UNIQUE (provider, provider_id),
    CONSTRAINT ck_user_local_password
        CHECK ((provider = 'LOCAL' AND password IS NOT NULL)
            OR (provider <> 'LOCAL' AND password IS NULL))
);

CREATE INDEX idx_users_handle ON users(handle) WHERE handle IS NOT NULL;
CREATE INDEX idx_users_deleted ON users(deleted_at) WHERE deleted_at IS NOT NULL;

-- ----------------------------------------------------------------------------
-- refresh_tokens — row-per-token 모델:
--   * 사용자당 다중 row 허용 (다중 디바이스).
--   * Task 2 단계: simplified refresh (delete + insert), rotated_to_id/revoked_* 컬럼은 NULL.
--   * Task 3 단계: rotation chain + reuse detection — markRotated/markRevoked로 의미 부여.
-- ----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(128) NOT NULL UNIQUE,
    issued_at       TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    rotated_to_id   BIGINT       REFERENCES refresh_tokens(id),
    revoked_at      TIMESTAMPTZ,
    revoked_reason  VARCHAR(50),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_active  ON refresh_tokens(user_id) WHERE revoked_at IS NULL;

-- ============================================================================
-- brownfield baseline (validate 통과 목적)
--   본 두 테이블은 Epic 1 MVP-CORE scope 밖. 정식 도메인 모델은 후속 Epic에서:
--    - login_attempts: Epic 9 (Reliability/Security) 시점에 재설계
--    - kopis_performance: Epic 7 (KOPIS Data Pipeline) Story 7.1에서 4-tier raw/curated로 재설계
--   V1에 포함하는 유일한 이유는 기존 @Entity가 schema와 일치해야 JPA validate가 통과하기 때문.
--   단 LoginAttempt.user_id 컬럼은 Task 2의 LoginAttemptService email 통일과 맞춰 email로 rename
--   — 엔티티/리포지토리 메서드명 동반 변경.
-- ============================================================================

CREATE TABLE login_attempts (
    id              BIGSERIAL    PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    client_ip       VARCHAR(64)  NOT NULL,
    fail_count      INTEGER      NOT NULL,
    -- LoginAttempt 엔티티는 LocalDateTime 사용 → TIMESTAMP (without timezone).
    -- Epic 9에서 OffsetDateTime으로 통일 예정.
    first_failed_at TIMESTAMP    NOT NULL,
    locked_until    TIMESTAMP,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_login_attempts_email_ip UNIQUE (email, client_ip)
);

CREATE TABLE kopis_performance (
    id                BIGSERIAL    PRIMARY KEY,
    mt20id            VARCHAR(255) NOT NULL UNIQUE,
    prfnm             VARCHAR(255),
    poster            VARCHAR(1000),
    fcltynm           VARCHAR(255),
    prfstate          VARCHAR(255),
    prfpdfrom         DATE,
    prfpdto           DATE,
    genrenm           VARCHAR(255),
    has_detail        BOOLEAN,
    prfcast           TEXT,
    prfruntime        VARCHAR(255),
    pcseguidance      VARCHAR(1000),
    area              VARCHAR(255),
    dtguidance        VARCHAR(255),
    visit             BOOLEAN      NOT NULL,
    festival          BOOLEAN      NOT NULL,
    relatenm          VARCHAR(255),
    relateurl         TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
