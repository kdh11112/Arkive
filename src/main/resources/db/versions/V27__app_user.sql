-- V27: 사용자 테이블 + 초기 관리자 + 메뉴 시드.
-- V2의 user 정의(메뉴 컬럼 복사본)는 쓰지 않는다. 로그인은 이 APP_USER를 쓴다.
-- 비밀번호는 bcrypt 해시만 저장한다. 초기 admin/Admin1234! 은 개발용이며 운영 전 반드시 변경한다.
-- HSQLDB 예약어 충돌 회피용으로 USER가 아니라 APP_USER를 쓴다.
CREATE TABLE IF NOT EXISTS APP_USER (
    USER_ID VARCHAR(50) NOT NULL,
    PASSWORD VARCHAR(200) NOT NULL,
    USER_NM VARCHAR(100) NOT NULL,
    EMAIL VARCHAR(100),
    FAILED_CNT INTEGER DEFAULT 0 NOT NULL,
    LOCK_YN VARCHAR(1) DEFAULT 'N' NOT NULL,
    USE_YN VARCHAR(1) DEFAULT 'Y' NOT NULL,
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDT_DT TIMESTAMP,
    CONSTRAINT PK_APP_USER PRIMARY KEY (USER_ID)
);

-- 초기 관리자. 비밀번호 Admin1234! 의 bcrypt 해시(node bcryptjs 2.4.3, cost 10)다.
MERGE INTO APP_USER target
USING (VALUES
    ('admin', '$2a$10$MvioV9e1Afy22kJ3sQ8rSuRDMxvOz1IVHOxmasa43HhbeZWjGl3Pq', '관리자', NULL, 0, 'N', 'Y', CURRENT_TIMESTAMP, NULL)
) source(user_id, password, user_nm, email, failed_cnt, lock_yn, use_yn, regist_dt, updt_dt)
ON target.USER_ID = source.user_id
WHEN NOT MATCHED THEN INSERT (USER_ID, PASSWORD, USER_NM, EMAIL, FAILED_CNT, LOCK_YN, USE_YN, REGIST_DT, UPDT_DT)
VALUES (source.user_id, source.password, source.user_nm, source.email, source.failed_cnt, source.lock_yn, source.use_yn, source.regist_dt, source.updt_dt);

MERGE INTO menu target
USING (VALUES
    ('ME02010300','사용자관리','/auth/userList',3,'ME02010000',3,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
