-- V16: 팝업 테이블 + 메뉴 시드.
-- 참고: BLMS TS_POPUP_MNG(POPUP_SN, TTL, PSTG_BGNG_YMD, PSTG_END_YMD, PSTG_YN, URL, ATCH_FILE_ID).
-- 이미지는 기존 공통 업로드(ATCH_FILE, POPUP_ 그룹)로 연결한다.
CREATE TABLE IF NOT EXISTS POPUP (
    POPUP_ID VARCHAR(50) NOT NULL,
    TITLE VARCHAR(200) NOT NULL,
    CONTENT CLOB,
    START_YMD VARCHAR(8) NOT NULL,
    END_YMD VARCHAR(8) NOT NULL,
    LINK_URL VARCHAR(500),
    USE_YN VARCHAR(1) DEFAULT 'Y' NOT NULL,
    ATCH_FILE_GRPID VARCHAR(50),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_POPUP PRIMARY KEY (POPUP_ID)
);

CREATE INDEX IF NOT EXISTS IDX_POPUP_USE ON POPUP(USE_YN);

MERGE INTO menu target
USING (VALUES
    ('ME02030600','팝업창관리','/popup/popupList',3,'ME02030000',6,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
