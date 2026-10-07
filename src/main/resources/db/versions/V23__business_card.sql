-- V23: 명함 테이블 + 메뉴 시드.
-- 근거: eGov v5.0 cop 명함관리. COMTNNCRD(명함정보속성)+COMTNNCRDUSER(명함사용자속성) 대응.
-- 핵심 규칙: 공개여부(공개 명함만 목록 노출), 사용등록(타인 공개 명함을 내 명함으로 등록).
-- 1단계(비로그인) 제약: 사용자는 작성자명 문자열로 식별한다.
CREATE TABLE IF NOT EXISTS BUSINESS_CARD (
    CARD_ID VARCHAR(50) NOT NULL,
    CARD_NM VARCHAR(100) NOT NULL,
    COMPANY_NM VARCHAR(100),
    DEPT_NM VARCHAR(100),
    POSITION_NM VARCHAR(100),
    TEL_NO VARCHAR(30),
    EMAIL VARCHAR(100),
    OPEN_YN VARCHAR(1) DEFAULT 'Y' NOT NULL,
    MEMO VARCHAR(500),
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_BUSINESS_CARD PRIMARY KEY (CARD_ID)
);

CREATE TABLE IF NOT EXISTS CARD_USER (
    CARD_ID VARCHAR(50) NOT NULL,
    USER_NM VARCHAR(50) NOT NULL,
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_CARD_USER PRIMARY KEY (CARD_ID, USER_NM),
    CONSTRAINT FK_CARD_USER_CARD FOREIGN KEY (CARD_ID) REFERENCES BUSINESS_CARD(CARD_ID) ON DELETE CASCADE
);

MERGE INTO menu target
USING (VALUES
    ('ME02031400','명함관리','/card/cardList',3,'ME02030000',14,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
