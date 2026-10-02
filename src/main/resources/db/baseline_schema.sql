-- ============================================================================
-- Arkive DB 밑그림 (baseline). 새 프로젝트 복사 전용이다.
--
-- 왜 나눴는가:
--   db/versions/의 V1, V2, V6... 파일은 "진화 체인"이다. 이미 적용된 DB가
--   있어서 적용된 파일은 고쳐도 소용없고, 변경은 새 버전으로만 간다.
--   반면 새 프로젝트에 복사할 때는 이력이 필요 없고 현재 스키마 전체가 필요하다.
--   그래서 진화용 체인(db/versions/)과 복사용 밑그림(db/baseline_schema.sql)을 분리했다.
--   실행기는 db/versions/V*__*.sql 패턴만 읽으므로 이 파일은 자동 실행되지 않는다.
--
-- 사용법 (새 프로젝트를 만들 때):
--   1. 이 파일을 db/versions/V1__baseline.sql 로 복사한다.
--   2. db/versions/의 기존 V1, V2, V6, V7, V8... 파일을 지운다.
--   3. 빈 DB로 시작하면 실행기가 V1(baseline)을 한 번 적용한다.
--
-- 경고:
--   이력이 있는 DB(SCHEMA_MIGRATION에 기록 있음)에는 절대 쓰지 않는다.
--   그런 DB는 V 체인으로만 변경한다. 밑그림을 덮어쓰면 이력이 꼬인다.
-- ============================================================================

-- 업로드 파일 메타데이터. USE_YN은 hard delete 전환(V7)으로 제거된 상태다.
CREATE TABLE IF NOT EXISTS ATCH_FILE (
    FILE_ID VARCHAR(50) NOT NULL,
    ATCH_FILE_GRPID VARCHAR(50) NOT NULL,
    ORGNL_FILE_NM VARCHAR(255) NOT NULL,
    PHYS_FILE_NM VARCHAR(255) NOT NULL,
    FILE_PATH VARCHAR(500) NOT NULL,
    FILE_SZ BIGINT NOT NULL,
    FILE_EXT VARCHAR(20),
    REG_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    RGTR_ID VARCHAR(50),
    CONSTRAINT PK_ATCH_FILE PRIMARY KEY (FILE_ID)
);

CREATE INDEX IF NOT EXISTS IDX_ATCH_FILE_GRPID ON ATCH_FILE(ATCH_FILE_GRPID);

-- 시스템 메뉴 관련 테이블.
CREATE TABLE IF NOT EXISTS menu (
    menu_id VARCHAR(20) NOT NULL,
    menu_nm VARCHAR(100),
    menu_cours VARCHAR(200),
    grad INTEGER,
    up_menu_id VARCHAR(10),
    ordr INTEGER,
    use_yn VARCHAR(1),
    regist_dt TIMESTAMP,
    register VARCHAR(50),
    updt_dt TIMESTAMP,
    updusr VARCHAR(50),
    CONSTRAINT idx_menu_pk PRIMARY KEY (menu_id)
);

CREATE TABLE IF NOT EXISTS menu_author (
    menu_id VARCHAR(20) NOT NULL,
    menu_nm VARCHAR(100),
    menu_cours VARCHAR(200),
    grad INTEGER,
    up_menu_id VARCHAR(10),
    ordr INTEGER,
    use_yn VARCHAR(1),
    regist_dt TIMESTAMP,
    register VARCHAR(50),
    updt_dt TIMESTAMP,
    updusr VARCHAR(50),
    CONSTRAINT idx_menu_author_pk PRIMARY KEY (menu_id)
);

CREATE TABLE IF NOT EXISTS user (
    menu_id VARCHAR(20) NOT NULL,
    menu_nm VARCHAR(100),
    menu_cours VARCHAR(200),
    grad INTEGER,
    up_menu_id VARCHAR(10),
    ordr INTEGER,
    use_yn VARCHAR(1),
    regist_dt TIMESTAMP,
    register VARCHAR(50),
    updt_dt TIMESTAMP,
    updusr VARCHAR(50),
    CONSTRAINT idx_user_pk PRIMARY KEY (menu_id)
);

-- 기본 메뉴. 현재값 기준이다 (드롭존 업로드 반영됨).
INSERT INTO menu (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr) VALUES
    ('ME00000000','아카이브 시스템',NULL,0,NULL,1,'Y',NULL,NULL,NULL,NULL),
    ('ME01000000','외부포털',NULL,1,'ME00000000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02000000','내부포털',NULL,1,'ME00000000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02010000','시스템관리','/system/menuList',2,'ME02000000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02010100','메뉴리스트','/system/menuList',3,'ME02010000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02020000','업로드','/system/multipartUpload',2,'ME02000000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02020100','멀티파트 업로드','/system/multipartUpload',3,'ME02020000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02020200','TUS 업로드','/system/tusUpload',3,'ME02020000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02020300','드롭존 업로드','/system/dropzoneUpload',3,'ME02020000',3,'Y',NULL,NULL,NULL,NULL);
