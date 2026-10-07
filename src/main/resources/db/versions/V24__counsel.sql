-- V24: 상담 테이블 + 메뉴 시드.
-- 근거: eGov v5.0 uss 상담관리. COMTNCNSLTLIST(상담내용+조치처리내용) 대응.
-- 핵심 규칙: COM028 진행상태(접수대기/접수/완료), 수정·삭제 시 비밀번호 확인, 첨부 최대 3개, 관리자 답변목록 분리.
-- 비밀번호는 SHA 해시로 저장한다(CommUtil.encryptSHA).
CREATE TABLE IF NOT EXISTS COUNSEL (
    COUNSEL_ID VARCHAR(50) NOT NULL,
    TITLE VARCHAR(200) NOT NULL,
    CONTENT CLOB,
    WRITER VARCHAR(50) NOT NULL,
    PASSWORD VARCHAR(200) NOT NULL,
    STATUS VARCHAR(10) DEFAULT '접수대기' NOT NULL,
    ANSWER CLOB,
    ANSWER_DT TIMESTAMP,
    ATCH_FILE_GRPID VARCHAR(50),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDT_DT TIMESTAMP,
    CONSTRAINT PK_COUNSEL PRIMARY KEY (COUNSEL_ID)
);

CREATE INDEX IF NOT EXISTS IDX_COUNSEL_STATUS ON COUNSEL(STATUS);

MERGE INTO menu target
USING (VALUES
    ('ME02031500','상담관리','/counsel/counselList',3,'ME02030000',15,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
