-- V26: 게시판 메뉴 표시명 변경. CK에디터 게시판 → 공지사항관리, 토스트 게시판 → 자료실관리.
-- BOARD_TYPE 코드(CK/TOAST)와 URL은 그대로 둔다. 표시명만 바꾼다.
UPDATE menu SET menu_nm = '공지사항관리' WHERE menu_id = 'ME02030100';
UPDATE menu SET menu_nm = '자료실관리' WHERE menu_id = 'ME02030200';
