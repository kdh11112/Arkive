-- V15: 메모보고 테이블 + 메뉴 시드.
-- eGov cop:smt:mrm 대응. Arkive-local 상태값(보고/확인/의견), eGov 코드 매핑은 이식 시 조정.
-- 상태 전이: 보고(등록) → 확인/의견(대상자). 보고 후 질문 수정은 STATUS='보고'일 때만 허용한다.
CREATE TABLE IF NOT EXISTS MEMO_REPORT (
    REPORT_ID VARCHAR(50) NOT NULL,
    TITLE VARCHAR(200) NOT NULL,
    CONTENT CLOB,
    REPORTER VARCHAR(50) NOT NULL,
    RECEIVER VARCHAR(50) NOT NULL,
    STATUS VARCHAR(10) DEFAULT '보고' NOT NULL,
    REPORT_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONFIRM_DT TIMESTAMP,
    OPINION CLOB,
    CONSTRAINT PK_MEMO_REPORT PRIMARY KEY (REPORT_ID)
);

CREATE INDEX IF NOT EXISTS IDX_MEMO_RECEIVER ON MEMO_REPORT(RECEIVER);
CREATE INDEX IF NOT EXISTS IDX_MEMO_STATUS ON MEMO_REPORT(STATUS);

MERGE INTO menu target
USING (VALUES
    ('ME02030500','메모보고','/memo/memoList',3,'ME02030000',5,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
