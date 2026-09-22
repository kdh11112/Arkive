DROP TABLE IF EXISTS menu;
CREATE TABLE menu (
    menu_id VARCHAR(20) NOT NULL,	-- 메뉴ID
    menu_nm VARCHAR(100) NULL, 		-- 메뉴명
    menu_cours VARCHAR(200) NULL, 	-- 메뉴경로
    grad INTEGER NULL, 				-- 등급
    up_menu_id VARCHAR(10) NULL, 	-- 상위메뉴ID
    ordr INTEGER NULL, 				-- 순서
    use_yn VARCHAR(1) NULL, 		-- 사용여부
    regist_dt TIMESTAMP NULL, 		-- 등록일시
    register VARCHAR(50) NULL,		-- 등록자
    updt_dt TIMESTAMP NULL, 		-- 수정일시
    updusr VARCHAR(50) NULL, 		-- 수정자
    CONSTRAINT idx_menu_pk PRIMARY KEY (menu_id)
);

INSERT INTO menu (menu_id,menu_nm,menu_cours,grad,up_menu_id,ordr,use_yn,regist_dt,register,updt_dt,updusr) 
VALUES	('ME00000000','아카이브 시스템',NULL,0,NULL,1,'Y',NULL,NULL,NULL,NULL),
		('ME01000000','외부포털',NULL,1,'ME00000000',1,'Y',NULL,NULL,NULL,NULL),
		('ME02000000','내부포털',NULL,1,'ME00000000',2,'Y',NULL,NULL,NULL,NULL),
		('ME02010000','시스템관리','/system/menuList',2,'ME02000000',1,'Y',NULL,NULL,NULL,NULL),
		('ME02010100','메뉴리스트','/system/menuList',3,'ME02010000',1,'Y',NULL,NULL,NULL,NULL),
		('ME02020000','업로드','/system/multipartUpload',2,'ME02000000',2,'Y',NULL,NULL,NULL,NULL),
		('ME02020100','멀파트 업로드','/system/multipartUpload',3,'ME02020000',1,'Y',NULL,NULL,NULL,NULL),
		('ME02020200','TUS 업로드','/system/tusUpload',3,'ME02020000',2,'Y',NULL,NULL,NULL,NULL),
		('ME02020300','기타 업로드','/system/otherUpload',3,'ME02020000',3,'Y',NULL,NULL,NULL,NULL);
		
DROP TABLE IF EXISTS menu_author;
CREATE TABLE menu_author (
    menu_id VARCHAR(20) NOT NULL,	-- 메뉴ID
    menu_nm VARCHAR(100) NULL, 		-- 메뉴명
    menu_cours VARCHAR(200) NULL, 	-- 메뉴경로
    grad INTEGER NULL, 				-- 등급
    up_menu_id VARCHAR(10) NULL, 	-- 상위메뉴ID
    ordr INTEGER NULL, 				-- 순서
    use_yn VARCHAR(1) NULL, 		-- 사용여부
    regist_dt TIMESTAMP NULL, 		-- 등록일시
    register VARCHAR(50) NULL,		-- 등록자
    updt_dt TIMESTAMP NULL, 		-- 수정일시
    updusr VARCHAR(50) NULL, 		-- 수정자
    CONSTRAINT idx_menu_author_pk PRIMARY KEY (menu_id)
);		

DROP TABLE IF EXISTS user;
CREATE TABLE user (
    menu_id VARCHAR(20) NOT NULL,	-- 메뉴ID
    menu_nm VARCHAR(100) NULL, 		-- 메뉴명
    menu_cours VARCHAR(200) NULL, 	-- 메뉴경로
    grad INTEGER NULL, 				-- 등급
    up_menu_id VARCHAR(10) NULL, 	-- 상위메뉴ID
    ordr INTEGER NULL, 				-- 순서
    use_yn VARCHAR(1) NULL, 		-- 사용여부
    regist_dt TIMESTAMP NULL, 		-- 등록일시
    register VARCHAR(50) NULL,		-- 등록자
    updt_dt TIMESTAMP NULL, 		-- 수정일시
    updusr VARCHAR(50) NULL, 		-- 수정자
    CONSTRAINT idx_user_pk PRIMARY KEY (menu_id)
);

DROP TABLE IF EXISTS ATCH_FILE;
CREATE TABLE ATCH_FILE (
    FILE_ID VARCHAR(50) NOT NULL,               -- 파일 고유 ID (UUID)
    ATCH_FILE_GRPID VARCHAR(50) NOT NULL,       -- 파일 그룹 ID
    ORGNL_FILE_NM VARCHAR(255) NOT NULL,        -- 원본 파일명
    PHYS_FILE_NM VARCHAR(255) NOT NULL,         -- 물리 저장 파일명 (UUID.확장자)
    FILE_PATH VARCHAR(500) NOT NULL,            -- 저장 경로 (예: C:/upload/arkive/2026/08/)
    FILE_SZ BIGINT NOT NULL,                    -- 파일 크기 (Byte)
    FILE_EXT VARCHAR(20) NULL,                  -- 파일 확장자
    USE_YN CHAR(1) DEFAULT 'Y' NOT NULL,        -- 사용 여부
    REG_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 등록일시
    RGTR_ID VARCHAR(50) NULL,                   -- 등록자 ID
    CONSTRAINT PK_ATCH_FILE PRIMARY KEY (FILE_ID)
);
CREATE INDEX IDX_ATCH_FILE_GRPID ON ATCH_FILE(ATCH_FILE_GRPID);
