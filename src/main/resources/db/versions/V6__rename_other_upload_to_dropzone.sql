-- V6: 기타 업로드를 드롭존 업로드로 변경합니다.
-- FileController 리네임과 메뉴 경로를 일치시키며, 기존 메뉴 ID는 유지합니다.
UPDATE menu
SET menu_nm = '드롭존 업로드',
    menu_cours = '/system/dropzoneUpload'
WHERE menu_id = 'ME02020300';
