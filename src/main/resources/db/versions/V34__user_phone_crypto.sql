-- 2단계: APP_USER.PHONE 필드 암호화를 위한 컬럼 확장.
-- AES-GCM 암호문은 평문보다 길어지므로 VARCHAR 길이를 넉넉히 둔다.
-- EMAIL은 아이디 찾기·비밀번호 재설정의 조회 조건이라 평문을 유지한다.
ALTER TABLE APP_USER ALTER COLUMN PHONE VARCHAR(500);
