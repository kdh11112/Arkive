package arkive.admin.auth.service;

/**
 * 메뉴 1건의 유효 권한. 여러 역할이 겹치면 조회·쓰기 모두 OR로 합친다.
 */
public record MenuPermission(boolean read, boolean write) {
}
