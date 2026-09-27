-- V2: 시스템 메뉴 관련 테이블을 생성하고 기본 메뉴를 추가합니다.
-- 기존 DB에도 안전하도록 DROP을 사용하지 않고, 기존 메뉴 ID는 중복 추가하지 않습니다.
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

MERGE INTO menu target
USING (VALUES
    ('ME00000000','아카이브 시스템',NULL,0,NULL,1,'Y',NULL,NULL,NULL,NULL),
    ('ME01000000','외부포털',NULL,1,'ME00000000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02000000','내부포털',NULL,1,'ME00000000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02010000','시스템관리','/system/menuList',2,'ME02000000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02010100','메뉴리스트','/system/menuList',3,'ME02010000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02020000','업로드','/system/multipartUpload',2,'ME02000000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02020100','멀파트 업로드','/system/multipartUpload',3,'ME02020000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02020200','TUS 업로드','/system/tusUpload',3,'ME02020000',2,'Y',NULL,NULL,NULL,NULL),
    ('ME02020300','기타 업로드','/system/otherUpload',3,'ME02020000',3,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
