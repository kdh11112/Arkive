-- V19: 카테고리 테이블 + 메뉴 시드.
-- 참고: BLMS TN_CLSF_MNG(CLSF_CD, CLSF_NM, GRADE) 드릴다운, 메뉴 jstree(TS_MENU_MNG+selectMenuList.json).
-- N단 확장: UP_CAT_ID 자기참조. jstree형 화면과 드릴다운형 화면이 이 테이블을 공유한다.
CREATE TABLE IF NOT EXISTS CATEGORY (
    CAT_ID VARCHAR(50) NOT NULL,
    UP_CAT_ID VARCHAR(50),
    CAT_NM VARCHAR(200) NOT NULL,
    ORDR INTEGER DEFAULT 0 NOT NULL,
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_CATEGORY PRIMARY KEY (CAT_ID)
);

CREATE INDEX IF NOT EXISTS IDX_CATEGORY_UP ON CATEGORY(UP_CAT_ID);

MERGE INTO menu target
USING (VALUES
    ('ME02031000','카테고리관리','/category/tree',3,'ME02030000',10,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
