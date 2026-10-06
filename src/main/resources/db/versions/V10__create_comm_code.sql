-- V10: 공통코드 테이블 생성 + 시스템관리 하위 메뉴 시드.
-- 마스터는 MASTR_CODE = DETAIL_CODE 인 행으로 표현한다 (별도 마스터 테이블 없음).
CREATE TABLE IF NOT EXISTS COMM_CODE (
    MASTR_CODE VARCHAR(50) NOT NULL,
    DETAIL_CODE VARCHAR(50) NOT NULL,
    CODE_NM VARCHAR(200) NOT NULL,
    CD_DC VARCHAR(500),
    ORDR INTEGER DEFAULT 0 NOT NULL,
    USE_YN VARCHAR(1) DEFAULT 'Y' NOT NULL,
    REFRN_1 VARCHAR(200),
    REFRN_2 VARCHAR(200),
    REFRN_3 VARCHAR(200),
    REFRN_4 VARCHAR(200),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_COMM_CODE PRIMARY KEY (MASTR_CODE, DETAIL_CODE)
);

MERGE INTO menu target
USING (VALUES
    ('ME02010200','공통코드관리','/system/commCodeList',3,'ME02010000',2,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
