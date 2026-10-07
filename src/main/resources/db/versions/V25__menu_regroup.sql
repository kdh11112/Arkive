-- V25: 메뉴 regroup. 1단계 기능이 전부 게시판관리 밑에 몰려 있어 용도별 그룹으로 나눈다.
-- 게시판관리: CK·토스트·FAQ·Q&A·POLL·상담
-- 화면관리: 팝업·배너(메인이미지 포함)
-- 일정관리: 일정·공휴일·당직
-- 주소·분류관리: 주소록·명함·카테고리
-- 보고관리: 메모보고
-- 신규 그룹은 게시판관리(V9, grad 2)와 같은 규격으로 둔다. 기존 ID는 바꾸지 않고 상·순서만 옮긴다.
MERGE INTO menu target
USING (VALUES
    ('ME02040000','화면관리',NULL,2,'ME02000000',4,'Y',NULL,NULL,NULL,NULL),
    ('ME02050000','일정관리',NULL,2,'ME02000000',5,'Y',NULL,NULL,NULL,NULL),
    ('ME02060000','주소·분류관리',NULL,2,'ME02000000',6,'Y',NULL,NULL,NULL,NULL),
    ('ME02070000','보고관리',NULL,2,'ME02000000',7,'Y',NULL,NULL,NULL,NULL)
) source(menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
ON target.menu_id = source.menu_id
WHEN NOT MATCHED THEN INSERT (menu_id, menu_nm, menu_cours, grad, up_menu_id, ordr, use_yn, regist_dt, register, updt_dt, updusr)
VALUES (source.menu_id, source.menu_nm, source.menu_cours, source.grad, source.up_menu_id, source.ordr, source.use_yn, source.regist_dt, source.register, source.updt_dt, source.updusr);

-- 게시판관리 소속 정리
UPDATE menu SET up_menu_id = 'ME02030000', ordr = 3 WHERE menu_id = 'ME02030300';
UPDATE menu SET up_menu_id = 'ME02030000', ordr = 4 WHERE menu_id = 'ME02030400';
UPDATE menu SET up_menu_id = 'ME02030000', ordr = 5 WHERE menu_id = 'ME02031200';
UPDATE menu SET up_menu_id = 'ME02030000', ordr = 6 WHERE menu_id = 'ME02031500';

-- 화면관리
UPDATE menu SET up_menu_id = 'ME02040000', ordr = 1 WHERE menu_id = 'ME02030600';
UPDATE menu SET up_menu_id = 'ME02040000', ordr = 2 WHERE menu_id = 'ME02030700';

-- 일정관리
UPDATE menu SET up_menu_id = 'ME02050000', ordr = 1 WHERE menu_id = 'ME02030800';
UPDATE menu SET up_menu_id = 'ME02050000', ordr = 2 WHERE menu_id = 'ME02030900';
UPDATE menu SET up_menu_id = 'ME02050000', ordr = 3 WHERE menu_id = 'ME02031300';

-- 주소·분류관리
UPDATE menu SET up_menu_id = 'ME02060000', ordr = 1 WHERE menu_id = 'ME02031100';
UPDATE menu SET up_menu_id = 'ME02060000', ordr = 2 WHERE menu_id = 'ME02031400';
UPDATE menu SET up_menu_id = 'ME02060000', ordr = 3 WHERE menu_id = 'ME02031000';

-- 보고관리
UPDATE menu SET up_menu_id = 'ME02070000', ordr = 1 WHERE menu_id = 'ME02030500';
