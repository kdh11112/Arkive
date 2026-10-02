-- V7: hard delete 전환에 따라 ATCH_FILE.USE_YN 컬럼을 제거한다.
-- 이미 N으로 표시된 행은 실제 파일이 삭제된 것이므로 함께 정리한다.
DELETE FROM ATCH_FILE WHERE USE_YN = 'N';
ALTER TABLE ATCH_FILE DROP COLUMN USE_YN;
