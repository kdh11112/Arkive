-- 2단계: 초성 검색용 색인 컬럼. DB LIKE 검색이 기본이고 Elasticsearch는 선택이다.
ALTER TABLE BOARD ADD COLUMN SEARCH_CHOSUNG VARCHAR(200);
