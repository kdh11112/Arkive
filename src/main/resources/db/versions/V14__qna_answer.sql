-- V14: Q&A 답변 테이블 + Q&A관리 메뉴 시드.
-- 질문은 BOARD(BOARD_TYPE='QNA') 재사용, 답변만 1:1로 분리한다(pony QUESTION/ANSWER 분리 참조).
-- 답변 상태는 별도 컬럼 없이 답변 존재 여부로 판정하므로(COM028 불필요) 동기화 불일치가 없다.
-- 질문 삭제 시 답변도 함께 삭제된다.
CREATE TABLE IF NOT EXISTS QNA_ANSWER (
    BOARD_ID VARCHAR(50) NOT NULL,
    ANSWER_CONTENT CLOB,
    ANSWERER VARCHAR(50),
    ANSWER_DT TIMESTAMP,
    CONSTRAINT PK_QNA_ANSWER PRIMARY KEY (BOARD_ID),
    CONSTRAINT FK_QNA_ANSWER_BOARD FOREIGN KEY (BOARD_ID) REFERENCES BOARD(BOARD_ID) ON DELETE CASCADE
);

MERGE INTO menu target
USING (VALUES
    ('ME02030400','Q&A관리','/qna/qnaList',3,'ME02030000',4,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
