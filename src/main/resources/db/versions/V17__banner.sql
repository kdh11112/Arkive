-- V17: 배너 테이블 + 메뉴 시드.
-- 참고: BLMS TS_BNR_MNG(BNR_SN, TTL, PSTG_BGNG_YMD, PSTG_END_YMD, ATCH_FILE_ID, URL, PSTG_YN).
-- 메인이미지는 별도 테이블 없이 POSITION='MAIN'으로 통합한다(노출위치 구분·이미지 규격은 운영에서 통일).
CREATE TABLE IF NOT EXISTS BANNER (
    BANNER_ID VARCHAR(50) NOT NULL,
    TITLE VARCHAR(200) NOT NULL,
    LINK_URL VARCHAR(500),
    POSITION VARCHAR(10) DEFAULT 'BANNER' NOT NULL,
    START_YMD VARCHAR(8) NOT NULL,
    END_YMD VARCHAR(8) NOT NULL,
    USE_YN VARCHAR(1) DEFAULT 'Y' NOT NULL,
    ORDR INTEGER DEFAULT 0 NOT NULL,
    ATCH_FILE_GRPID VARCHAR(50),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_BANNER PRIMARY KEY (BANNER_ID)
);

CREATE INDEX IF NOT EXISTS IDX_BANNER_POS ON BANNER(POSITION);

MERGE INTO menu target
USING (VALUES
    ('ME02030700','배너관리','/banner/bannerList',3,'ME02030000',7,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
