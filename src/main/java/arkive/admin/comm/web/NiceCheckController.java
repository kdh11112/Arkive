/************************************
 * @class ID    : .java
 * @Description	: 파일 Controller
 * @Author      : 문준구
 * @LastUpdate  : 2020.02.12
*/

package arkive.admin.comm.web;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import arkive.admin.comm.service.NiceInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/niceExt")
public class NiceCheckController extends FormBasedFileUtil{

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
//	@Resource(name = "logService")
//	private LogService logService;
	 
	private CommUtil cmmUtil = new CommUtil();
	private DateUtil dateUtil;
	
	@Autowired
	private NiceCheck niceCheck;
	

	/**
	 * nice체크 오프너로 이용
	 * @param request
	 * @param commandMap
	 * @param niceInfoVO
	 * @param model
	 * @param session
	 * @return
	 * @throws NullPointerException
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 */
	@RequestMapping("/niceCheckAjax.do")
	public String niceCheckWeb(HttpServletRequest request, @RequestParam Map<String, Object> commandMap,
									ModelMap model , HttpSession session) throws NullPointerException, InvalidKeyException, NoSuchAlgorithmException, ParseException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {	
		String fncNm = "niceCheckWeb";
		String fncCn = "nice체크 오프너로 이용";
		
		try {
			
			String req_no = (String)session.getAttribute("req_no");
	        String key = (String)session.getAttribute("key");
	        String iv = (String)session.getAttribute("iv");
	        String hmac_key = (String)session.getAttribute("hmac_key");
	        String s_token_version_id = (String)session.getAttribute("token_version_id");
	        String enc_data = request.getParameter("enc_data");
	        String token_version_id = request.getParameter("token_version_id");
	        String integrity_value = request.getParameter("integrity_value");

	        String enctime ="";
	        String requestno ="";
	        String responseno ="";
	        String authtype ="";
	        String name ="";
	        String birthdate = "";
	        String gender ="";
	        String nationalinfo="";
	        String ci ="";
	        String di ="";
	        String mobileno ="";
	        String mobileco ="";

	        String sMessage ="";
	        String resultCode = "";
	        try {    
		        byte[] hmacSha256 = hmac256(hmac_key.getBytes(), enc_data.getBytes());
		        String integrity = Base64.getEncoder().encodeToString(hmacSha256);
		       
		        if (!integrity.equals(integrity_value)){
		            sMessage = "무결성 값이 다릅니다. 데이터가 변경된 것이 아닌지 확인 바랍니다.";
		        }else{
		            String dec_data = getAesDecDataPKCS5(key.getBytes(), iv.getBytes(), enc_data);
		            JSONParser jsonParse = new JSONParser();
		            JSONObject plain_data = (JSONObject) jsonParse.parse(dec_data);
		            
		            
		            if (!req_no.equals(plain_data.get("requestno").toString())){
		                sMessage = "세션값이 다릅니다. 올바른 경로로 접근하시기 바랍니다.";
		            }else{
		                sMessage = "복호화 성공";
		                resultCode = plain_data.get("resultcode").toString();
		                name = URLDecoder.decode(plain_data.get("utf8_name").toString(), "UTF-8");
		                birthdate = plain_data.get("birthdate").toString();
		                gender =plain_data.get("gender").toString();
		                mobileno =plain_data.get("mobileno").toString();
		            }
		        }
		        
		        
		        if("0".equals(gender)) {
		        	gender = "2";
		        }
		        
		        
		        NiceInfoVO tempVO = new NiceInfoVO();
		        
		        tempVO.setPhone(mobileno);
				tempVO.setNiceNm(name);
				tempVO.setGender(gender);
				tempVO.setBirthdate(birthdate);
	            
	            session.setAttribute("niceInfoVO", tempVO);
	            String niceLoginDt = dateUtil.getFormatedDateTime(dateUtil.getCurrentDate() + dateUtil.getCurrentTime());
		        session.setAttribute("niceLoginDt", niceLoginDt);
	            
	            model.addAttribute("niceMessage", sMessage);
	            model.addAttribute("resultCode", resultCode);
	            
	            
	            EgovMap logMap = new EgovMap();
				logMap.put("userId", name);
				logMap.put("userIp", request.getRemoteAddr());
				logMap.put("conectAt", "Y");
				//logService.insertLoginLog(logMap);
	            
	            
	        } catch (NullPointerException e) {
	        	logger.error(fncNm + "Exception");
			}

		} catch(DataAccessException e) {
			logger.error(fncNm + "DataAccessException");
			cmmUtil.debug(getClass(), e);
		} catch(RuntimeException e) {
			logger.error(fncNm + "RumtimeException");
			cmmUtil.debug(getClass(), e);
		}
		
		return "gtemsExt/main/niceCheckAjax";
	}
	
	
	
    //복호화를 위한 함수
    public static String getAesDecDataPKCS5(byte[] key, byte[] iv, String base64Enc) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        SecretKey secureKey = new SecretKeySpec(key, "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(iv));
        byte[] cipherEnc = Base64.getDecoder().decode(base64Enc);
                
        String Dec = new String(c.doFinal(cipherEnc), "utf-8");
                
        return Dec;
    }
    
    //무결성값 생성을 위한 함수
    public static byte[] hmac256(byte[] secretKey,byte[] message) 
            throws NoSuchAlgorithmException, InvalidKeyException{
        byte[] hmac256 = null;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec sks = new SecretKeySpec(secretKey, "HmacSHA256");
        mac.init(sks);
        hmac256 = mac.doFinal(message);
        
        return hmac256;     
      }
    
	@RequestMapping("/niceOpen.do")
	public String niceOpen(HttpServletRequest request, @RequestParam Map<String, Object> commandMap, ModelMap model , HttpSession session) {	

		return "gtemsExt/main/niceOpen";
	}
}
