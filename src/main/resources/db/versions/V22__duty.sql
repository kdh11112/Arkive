-- V22: 당직 테이블 + 메뉴 시드.
-- 참고: gcsms DutySchedule(WORK_TEAM + MEMBER 2테이블, downloadExcel).
-- 1단계용으로 단일 DUTY 테이블로 축소했다. 승인연동(약식결재) 제외, 달력 표시는 일정관리와 같은 월 목록으로 한다.
-- 엑셀 일괄등록 형식: 날짜(YYYYMMDD) | 당직자 | 비고. 이 순서를 고정한다.
CREATE TABLE IF NOT EXISTS DUTY (
    DUTY_ID VARCHAR(50) NOT NULL,
    DUTY_DE VARCHAR(8) NOT NULL,
    DUTY_USER VARCHAR(100) NOT NULL,
    NOTE VARCHAR(500),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_DUTY PRIMARY KEY (DUTY_ID)
);

CREATE INDEX IF NOT EXISTS IDX_DUTY_DE ON DUTY(DUTY_DE);

MERGE INTO menu target
USING (VALUES
    ('ME02031300','당직관리','/duty/dutyList',3,'ME02030000',13,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
