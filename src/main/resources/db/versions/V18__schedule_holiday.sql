-- V18: 일정 + 공휴일 테이블 + 메뉴 시드.
-- 타 프로젝트에 범용 일정 모듈이 없어(서베이: gcsms SchduleSQL은 배치 스케줄러) Arkive-local로 만든다.
-- 부서/개인/일지/할일은 SCHEDULE_TYPE으로 구분하고 쿼리를 통일한다(월/주/일 = 기간 조건만 다름).
-- 공휴일은 당직·부서일정·휴가 공통 달력용으로 별도 관리한다. 날짜는 DB 함수 없이 문자열(YYYY-MM-DD HH24:MI)로 통일.
CREATE TABLE IF NOT EXISTS SCHEDULE (
    SCHEDULE_ID VARCHAR(50) NOT NULL,
    TITLE VARCHAR(200) NOT NULL,
    CONTENT CLOB,
    START_DT VARCHAR(16) NOT NULL,
    END_DT VARCHAR(16) NOT NULL,
    SCHEDULE_TYPE VARCHAR(10) DEFAULT 'DEPT' NOT NULL,
    REGISTER VARCHAR(50),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_SCHEDULE PRIMARY KEY (SCHEDULE_ID)
);

CREATE INDEX IF NOT EXISTS IDX_SCHEDULE_DT ON SCHEDULE(START_DT);
CREATE INDEX IF NOT EXISTS IDX_SCHEDULE_TYPE ON SCHEDULE(SCHEDULE_TYPE);

CREATE TABLE IF NOT EXISTS HOLIDAY (
    HOLIDAY_DE VARCHAR(8) NOT NULL,
    HOLIDAY_NM VARCHAR(100) NOT NULL,
    CONSTRAINT PK_HOLIDAY PRIMARY KEY (HOLIDAY_DE)
);

MERGE INTO menu target
USING (VALUES
    ('ME02030800','일정관리','/schedule/scheduleList',3,'ME02030000',8,'Y',NULL,NULL,NULL,NULL),
    ('ME02030900','공휴일관리','/schedule/holidayList',3,'ME02030000',9,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
