-- V21: 온라인POLL 테이블 + 메뉴 시드.
-- 참고: GTEMS TS_QESTNR_BASS(설문)+TS_QESTNR_MST(문항)+통계, gcsms TB_UP_SRVY_EXMN+TB_UP_SRVY_QITEM.
-- 1단계용으로 단일 질문·객관식 보기 구조로 축소했다. 3단계 설문관리 세트와 스키마 통일은 그때 조정.
-- 비로그인 중복투표 차단은 세션 집합(votedPolls)으로 한다. 세션 만료 시 재투표 가능(제약).
CREATE TABLE IF NOT EXISTS POLL (
    POLL_ID VARCHAR(50) NOT NULL,
    TITLE VARCHAR(200) NOT NULL,
    START_YMD VARCHAR(8) NOT NULL,
    END_YMD VARCHAR(8) NOT NULL,
    USE_YN VARCHAR(1) DEFAULT 'Y' NOT NULL,
    REGIST_DT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    REGISTER VARCHAR(50),
    UPDT_DT TIMESTAMP,
    UPDUSR VARCHAR(50),
    CONSTRAINT PK_POLL PRIMARY KEY (POLL_ID)
);

CREATE TABLE IF NOT EXISTS POLL_ITEM (
    ITEM_ID VARCHAR(50) NOT NULL,
    POLL_ID VARCHAR(50) NOT NULL,
    ITEM_NM VARCHAR(200) NOT NULL,
    ORDR INTEGER DEFAULT 0 NOT NULL,
    VOTE_CNT INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT PK_POLL_ITEM PRIMARY KEY (ITEM_ID),
    CONSTRAINT FK_POLL_ITEM_POLL FOREIGN KEY (POLL_ID) REFERENCES POLL(POLL_ID) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS IDX_POLL_ITEM_POLL ON POLL_ITEM(POLL_ID);

MERGE INTO menu target
USING (VALUES
    ('ME02031200','온라인POLL','/poll/pollList',3,'ME02030000',12,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
