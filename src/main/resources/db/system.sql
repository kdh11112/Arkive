DROP TABLE IF EXISTS menu;
CREATE TABLE menu (
    menu_id VARCHAR(20) NOT NULL,	-- 메뉴ID
    menu_nm VARCHAR(100) NULL, 		-- 메뉴명
    menu_cours VARCHAR(200) NULL, 	-- 메뉴경로
    grad INTEGER NULL, 				-- 등급
    up_menu_id VARCHAR(10) NULL, 	-- 상위메뉴ID
    ordr INTEGER NULL, 				-- 순서
    use_at VARCHAR(1) NULL, 		-- 사용여부
    regist_dt TIMESTAMP NULL, 		-- 등록일시
    register VARCHAR(50) NULL,		-- 등록자
    updt_dt TIMESTAMP NULL, 		-- 수정일시
    updusr VARCHAR(50) NULL, 		-- 수정자
    CONSTRAINT idx_ts_menu_pk PRIMARY KEY (menu_id)
);

INSERT INTO menu (menu_id,menu_nm,menu_cours,grad,up_menu_id,ordr,use_at,regist_dt,register,updt_dt,updusr) 
VALUES	('ME00000000','아카이브 시스템',NULL,0,NULL,1,'Y',NULL,NULL,NULL,NULL),
		('ME01000000','외부포털',NULL,1,'ME00000000',1,'Y',NULL,NULL,NULL,NULL),
		('ME02000000','내부포털',NULL,1,'ME00000000',2,'Y',NULL,NULL,NULL,NULL);