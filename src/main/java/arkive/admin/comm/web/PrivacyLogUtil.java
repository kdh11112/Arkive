package arkive.admin.comm.web;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 개인정보 접근 로그를 남기는 유틸 클래스
 */
public class PrivacyLogUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(PrivacyLogUtil.class);

    /**
     * 개인정보 접근 로그를 남긴다.
     *
     * @param request         HttpServletRequest
     * @param loggingService  LoggingService 인스턴스
     * @param trprId          대상자ID 값
     * @param menuId          메뉴ID 값
     */
    public static void insertLog(HttpServletRequest request,
                           //LoggingService loggingService,
                           String conectCnd,
                           String trprId,
                           String menuId,
                           long	dwnldCnt) {

            // 세션이 없거나 로그인 정보가 없으면 로그를 남기지 않음
            HttpSession session = request.getSession(false);
            if (session == null) {
                return;
            }
            //LoginVo loginVO = (LoginVo) session.getAttribute("USER");
//            if (loginVO == null) {
//                return;
//            }

//            String prcrId 	= loginVO.getUserId(); 		// 처리자 ID (로그인한 사용자)
            String conectIp = request.getRemoteAddr(); 	// 접속 IP
            String conectResn = request.getParameter("conectResn"); // 조회조건
            
            if("".equals(menuId) || menuId == null) {
            	menuId = request.getParameter("menuId");
            }
           
            
            Map<String, Object> logMap = new HashMap<>();
//            logMap.put("prcrId",   prcrId);
            logMap.put("conectIp", conectIp);
            logMap.put("conectCnd",  conectCnd);
            logMap.put("menuId",  	menuId);
            logMap.put("dwnldCnt",  dwnldCnt);
            logMap.put("conectResn",  conectResn);
            
        try {
        	
        	if("EXCEL".equals(conectCnd)) {
//        		loggingService.insertIndvdlinfoDwldLog(logMap);
        	}else {
//        		loggingService.insertIndvdlinfoConectLog(logMap);
        	}
        	
        } catch (DataAccessException e) {
        	logger.info("▒▒▒▒▒▒▒▒ PrivacyLogUtil.log() insertPrvcAccessLog error", e);
        }
    }
    
    /**
     * 여러 대상자에 대한 개인정보 접근 로그를 남긴다.
     * 
     * @param request         HttpServletRequest
     * @param loggingService  LoggingService 인스턴스
     * @param acsSeCd         접근 구분 코드
     * @param idListStr		  대상자ID 문자열
     * @param prcsCn          처리 사유 값
     */
    public static void insertMultipleLog(HttpServletRequest request,
//						           LoggingService loggingService,
						           String conectCnd,
						           String idListStr,
						           String menuId) {
		if (idListStr == null || idListStr.trim().isEmpty()) {
			return;
		}
		// 쉼표 앞뒤 공백 제거 후 분리
		String[] ids = idListStr.split("\\s*,\\s*");
		
		for (String trprId : ids) {
//			insertLog(request, loggingService,conectCnd ,trprId, menuId, 1);
		}
	}
    
    /**
     * 대상자 ID(trprId)가 없을 때 호출
     *
     * @param request         HttpServletRequest
     * @param loggingService  LoggingService 인스턴스
     * @param acsSeCd         접근 구분 코드
     * @param prcsCn          처리 사유 값
     */
    public static void insertEmptyLog(HttpServletRequest request,
//                           LoggingService loggingService,
                           String conectCnd,
                           String menuId,
                           int	dwnldCnt) {
        
    	// 대상자 ID(trprId)가 없으므로 null 을 넘긴다.
//    	insertLog(request, loggingService,conectCnd, null, menuId, dwnldCnt);
    }

}
