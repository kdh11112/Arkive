-- V9: 게시판 테이블 생성 + 좌측 메뉴 시드(CK/토스트 2종).
-- BOARD_TYPE: CK | TOAST. 첨부파일은 ATCH_FILE_GRPID(BOARD_ 접두사)로 연결한다.
CREATE TABLE IF NOT EXISTS BOARD (
    BOARD_ID VARCHAR(50) NOT NULL,
    BOARD_TYPE VARCHAR(10) NOT NULL,
    TITLE VARCHAR(200) NOT NULL,
    CONTENT CLOB,
    ATCH_FILE_GRPID VARCHAR(50),
    VIEW_CNT INTEGER DEFAULT 0 NOT NULL,
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_BOARD PRIMARY KEY (BOARD_ID)
);

CREATE INDEX IF NOT EXISTS IDX_BOARD_TYPE ON BOARD(BOARD_TYPE);

MERGE INTO menu target
USING (VALUES
    ('ME02030000','게시판관리',NULL,2,'ME02000000',3,'Y',NULL,NULL,NULL,NULL),
    ('ME02030100','CK에디터 게시판','/board/ckeditorList',3,'ME02030000',1,'Y',NULL,NULL,NULL,NULL),
    ('ME02030200','토스트 게시판','/board/toastList',3,'ME02030000',2,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
