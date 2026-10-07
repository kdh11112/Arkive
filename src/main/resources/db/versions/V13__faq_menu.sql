-- V13: FAQ관리 메뉴 시드. FAQ는 BOARD 테이블(BOARD_TYPE='FAQ')을 재사용하므로 신규 테이블 없음.
MERGE INTO menu target
USING (VALUES
    ('ME02030300','FAQ관리','/faq/faqList',3,'ME02030000',3,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);
