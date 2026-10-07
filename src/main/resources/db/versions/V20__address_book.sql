-- V20: 주소록 테이블 + 메뉴 시드.
-- 타 프로젝트에 주소록 모듈이 없어(서베이) Arkive-local로 만든다. 부서일정 팝업(부서·사용자 선택) 기반용.
CREATE TABLE IF NOT EXISTS ADDRESS_BOOK (
    ADDR_ID VARCHAR(50) NOT NULL,
    ADDR_NM VARCHAR(100) NOT NULL,
    DEPT_NM VARCHAR(100),
    TEL_NO VARCHAR(30),
    EMAIL VARCHAR(100),
    MEMO VARCHAR(500),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_ADDRESS_BOOK PRIMARY KEY (ADDR_ID)
);

MERGE INTO menu target
USING (VALUES
    ('ME02031100','주소록관리','/address/addressList',3,'ME02030000',11,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
