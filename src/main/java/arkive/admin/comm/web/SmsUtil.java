/************************************
 * @Description	: SMS, MMS 공통유틸
 * @Author      : 안현진
 * @LastUpdate  : 2021.05.12
 */

package arkive.admin.comm.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import twitter4j.JSONObject;

public class SmsUtil {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final String encodingType = "utf-8";
	private final String boundary = "____boundary____";
	
	/**
	 * SMS(단문), LMS(장문) 동일 내용 1천명까지 동시 발송 가능
	 *
	 * @param pEgovMap
	 * @return 등록 결과
	 * @throws HttpClientErrorException
	 * @throws ParseException 
	 * @throws NumberFormatException 
	 * @throws ClientProtocolException 
	 * @throws IOException 
	 */
	public int sendMessage(EgovMap pEgovMap) throws HttpClientErrorException, ParseException, NumberFormatException, ClientProtocolException, IOException {
		Map<String, String> sms = new HashMap<String, String>();
		
		int resultCode 		= -101;	//결과코드, 정상 : 1, 오류 : 0 이하
		int successCnt 		= 0;	//성공갯수, 정상 송신시 카운트 됨
		int errorCnt		= 0;	//에러갯수, 정상 송신시 카운트 됨
		String message		= "";   //결과문구, 정상: success, 오류: 오류메세지

		String smsUrl = String.valueOf(ConfigUtil.getProperty("sms.url")) + "/send/";
		String smsId = String.valueOf(ConfigUtil.getProperty("sms.id"));
		String smsKey = String.valueOf(ConfigUtil.getProperty("sms.apiKey"));
		String sender = String.valueOf(ConfigUtil.getProperty("sms.sender"));
		
		/******************** 인증정보 ********************/
		sms.put("user_id", smsId); 	// SMS 아이디
		sms.put("key", smsKey); 	//인증키
		/******************** 인증정보 ********************/
		
		/******************** 전송정보 ********************/
		//필수값
		sms.put("sender", sender); 											// 발신번호
		sms.put("receiver", (String)pEgovMap.get("receiver")); 				// 수신번호 컴마(,)분기 입력으로 최대 1천명, 예: "01111111111,01111111112"
		sms.put("msg", (String)pEgovMap.get("msg")); 						// 메세지 내용
		//필수값 아님
		sms.put("title", (String)pEgovMap.get("title")); 					// LMS, MMS 제목 (미입력시 본문중 44Byte 또는 엔터 구분자 첫라인)
		sms.put("msg_type", (String)pEgovMap.get("msgType")); 				// SMS(단문), LMS(장문), MMS(그림문자)
		//sms.put("destination", 	(String)pEgovMap.get("destination")); 	// 수신인 %고객명% 치환
		//sms.put("testmode_yn",  "Y"); // Y 인경우 실제문자 전송X , 자동취소(환불) 처리
		/******************** 전송정보 ********************/
		
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		
		builder.setBoundary(boundary);
		builder.setCharset(Charset.forName(encodingType));
		
		for(Iterator<String> i = sms.keySet().iterator(); i.hasNext();){
			String key = i.next();
			
			builder.addTextBody(key, sms.get(key), ContentType.create("Multipart/related", encodingType));
		}
		
		HttpEntity entity = builder.build();
		
		HttpClient client = HttpClients.createDefault();
		HttpPost post = new HttpPost(smsUrl);
		post.setEntity(entity);
		
		HttpResponse res = null;
		String result = "";

		res = client.execute(post);
		
		if(res != null){
			BufferedReader in = new BufferedReader(new InputStreamReader(((CloseableHttpResponse) res).getEntity().getContent(), encodingType));
			try {
				String buffer = null;
				while((buffer = in.readLine())!=null){
					result += buffer;
				}	
				
			} catch(IOException ioe){
				logger.error("sendMessage IOException");
			} finally {
				in.close();
			}
		}
		
		logger.info("result :: " + result);
		
		JSONParser parser 	= new JSONParser();
		Object obj = parser.parse(result);
		JSONObject resultJson = (JSONObject)obj;
		 
		resultCode = Integer.parseInt(String.valueOf(resultJson.get("result_code"))) ;
		if(resultCode > 0){
			successCnt = Integer.parseInt(String.valueOf(resultJson.get("success_cnt"))) ;
			errorCnt = Integer.parseInt(String.valueOf(resultJson.get("error_cnt"))) ;
		} else {
			message = String.valueOf(resultJson.get("message"));
			logger.error("message :: " + message);
		}
		
		return resultCode;
	}
	

	/**
	 * 각각 다른내용 동시 500명까지 전송가능
	 *
	 * @param pEgovMap
	 * @return 등록 결과
	 * @throws HttpClientErrorException
	 * @throws ParseException 
	 * @throws NumberFormatException 
	 * @throws ClientProtocolException 
	 * @throws IOException 
	 */
	public JSONObject curlSendMessage(EgovMap pEgovMap) throws HttpClientErrorException, ParseException, NumberFormatException, ClientProtocolException, IOException {
		Map<String, String> sms = new HashMap<String, String>();
		
		int resultCode 		= -101;	//결과코드, 정상 : 1, 오류 : 0 이하
		int successCnt 		= 0;	//성공갯수, 정상 송신시 카운트 됨
		int errorCnt		= 0;	//에러갯수, 정상 송신시 카운트 됨
		String message		= "";   //결과문구, 정상: success, 오류: 오류메세지
		
		String smsMassUrl = String.valueOf(ConfigUtil.getProperty("sms.url")) + "/send_mass/";
		String smsId = String.valueOf(ConfigUtil.getProperty("sms.id"));
		String smsKey = String.valueOf(ConfigUtil.getProperty("sms.apiKey"));
		String sender = String.valueOf(ConfigUtil.getProperty("sms.sender"));
		String smsEvlMssg = ""; 	//propertiesService.getString("Globals.EVL.MSSG");
		
		/******************** 인증정보 ********************/
		sms.put("user_id", smsId); 	// SMS 아이디
		sms.put("key", smsKey);		//인증키
		/******************** 인증정보 ********************/
		
		/******************** 전송정보 ********************/
		//필수값
		sms.put("sender", sender); 					// 발신번호
		sms.put("msg_type", "SMS"); 				// SMS(단문), LMS(장문), MMS(그림문자)
		sms.put("title", ""); 						// LMS, MMS 제목 (미입력시 본문중 44Byte 또는 엔터 구분자 첫라인)
		sms.put("testmode_yn", "Y"); 				// Y 인경우 실제문자 전송X , 자동취소(환불) 처리
		
		String receiver = String.valueOf(pEgovMap.get("receiver"));
		String receiverNm = String.valueOf(pEgovMap.get("receiverNm"));
		
		String[] receiverArr = null;
		String[] receiverNmArr = null;
		
		receiverArr = receiver.split(",");
		receiverNmArr = receiverNm.split(",");

		int cnt = receiverArr.length;
		
		for(int i=0; i < cnt; i++){
			
			String msg = ""; 	// smsEvlMssg;
			
			msg = msg.replace("EVL_NM", receiverNmArr[i]);
			msg = msg.replace("MTG_NM", String.valueOf(pEgovMap.get("mtgNm")));
			
			sms.put("rec_" + (i+1), receiverArr[i]); // 수신번호_$i 번째  = 필수항목
			sms.put("msg_" + (i+1), msg); // 내용_$i번째  = 필수항목
			
		}
		
		sms.put("cnt", String.valueOf(cnt));
		
		//logger.info("sms?" + sms.toString());
		
		//String image = "";
		//image = "/tmp/pic_57f358af08cf7_sms_.jpg"; // MMS 이미지 파일 위치
		
		/******************** 전송정보 ********************/
		
		/*****/
		/*** ※ 중요 - 기존 send 와 다른 부분  ***
		 *  msg_type 추가 : SMS 와 LMS 구분자 = 필수항목
		 *  receiver(수신번호) 와 msg(내용) 가 rec_1 ~ rec_500 과 msg_1 ~ msg_500 으로 설정가능 = 필수입력(최소 1개이상)
		 * cnt 추가 : 위 rec_갯수 와 msg_갯수에 지정된 갯수정보 지정 = 필수항목 (최대 500개)
		/******/
		
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		
		builder.setBoundary(boundary);
		builder.setCharset(Charset.forName(encodingType));
		
		for(Iterator<String> i = sms.keySet().iterator(); i.hasNext();){
			String key = i.next();
			builder.addTextBody(key, sms.get(key)
					, ContentType.create("Multipart/related", encodingType));
		}
		
		//이미지 보낼 때
		/*
		File imageFile = new File(image);
		if(image!=null && image.length()>0 && imageFile.exists()){
	
			builder.addPart("image",
					new FileBody(imageFile, ContentType.create("application/octet-stream"),
							URLEncoder.encode(imageFile.getName(), encodingType)));
		}
		*/
		
		HttpEntity entity = builder.build();
		
		HttpClient client = HttpClients.createDefault();
		HttpPost post = new HttpPost(smsMassUrl);
		post.setEntity(entity);
		
		HttpResponse res = client.execute(post);
		
		String result = "";
		if(res != null){
			BufferedReader in = new BufferedReader(new InputStreamReader(((CloseableHttpResponse) res).getEntity().getContent(), encodingType));
			
			try {
				String buffer = null;
				while((buffer = in.readLine())!=null){
					result += buffer;
				}	
			}catch( IOException ioe ){
				logger.error("curlSendMessage IOException");
			}finally {
				in.close();
			}
		}
		
		logger.info("result :: " + result);
		
		JSONParser parser 	= new JSONParser();
		Object obj = parser.parse(result);
		JSONObject resultJson = (JSONObject)obj;
		
		resultCode 			= Integer.parseInt(String.valueOf(resultJson.get("result_code"))) ;
		
		if(resultCode > 0){
			successCnt 		= Integer.parseInt(String.valueOf(resultJson.get("success_cnt"))) ;
			errorCnt		= Integer.parseInt(String.valueOf(resultJson.get("error_cnt"))) ;
		} else {
			message = String.valueOf(resultJson.get("message"));
			logger.error("message :: " + message);
		}
		
		return resultJson;
	}
	
	
	/**
	 * 문자전송 내역 보기
	 *
	 * @param pEgovMap
	 * @return 전송 내역
	 * @throws HttpClientErrorException
	 * @throws ParseException 
	 * @throws NumberFormatException 
	 * @throws ClientProtocolException 
	 * @throws IOException 
	 */
	public JSONObject curlSmsList() throws HttpClientErrorException, ParseException, NumberFormatException, ClientProtocolException, IOException {
		
		int resultCode 		= -101;	//결과코드, 정상 : 1, 오류 : 0 이하
		String message		= "";   //결과문구, 정상: success, 오류: 오류메세지
		String list			= "";
		String nextYn		= "";
		/**************** 최근 전송 목록 ******************/
		/* "result_code":결과코드,"message":결과문구, */
		/** list : 전송된 목록 배열 ***/
		
		/******************** 인증정보 ********************/
		List<NameValuePair> sms = new ArrayList<NameValuePair>();
		
		String smsCurlUrl 	= "";
		String smsId = String.valueOf(ConfigUtil.getProperty("sms.id"));
		String smsKey = String.valueOf(ConfigUtil.getProperty("sms.apiKey"));
		
		sms.add(new BasicNameValuePair("user_id", smsId)); // SMS 아이디 
		sms.add(new BasicNameValuePair("key", smsKey)); //인증키
		/******************** 인증정보 ********************/
		
		/*
		sms.add(new BasicNameValuePair("page", "1")); //조회 시작번호1
		sms.add(new BasicNameValuePair("page_size", "10")); //출력 갯수
		sms.add(new BasicNameValuePair("start_date", "")); //조회일 시작
		sms.add(new BasicNameValuePair("limit_day", "7")); //조회일수
		*/
	
		HttpClient client = HttpClients.createDefault();
		HttpPost post = new HttpPost(smsCurlUrl);
		post.setEntity(new UrlEncodedFormEntity(sms, Charset.forName(encodingType)));
		
		HttpResponse res = client.execute(post);
		
		String result = "";
		if(res != null){
			BufferedReader in = new BufferedReader(new InputStreamReader(((CloseableHttpResponse) res).getEntity().getContent(), encodingType));
			try {
				String buffer = null;
				while((buffer = in.readLine())!=null){
					result += buffer;
				}	
			}catch( IOException ioe ){
				logger.error("curlSmsList IOException");
			}finally {
				in.close();
			}
		}

		JSONParser parser 	= new JSONParser();
		Object obj = parser.parse(result);
		JSONObject resultJson = (JSONObject)obj;
		 
		resultCode 			= Integer.parseInt(String.valueOf(resultJson.get("result_code"))) ;
		if(resultCode > 0){
			list			= (String.valueOf(resultJson.get("list"))) ;
			nextYn			= (String.valueOf(resultJson.get("error_cnt"))) ;
		} else {
			message = String.valueOf(resultJson.get("message"));
			logger.error("message :: " + message);
		}
		
		return resultJson;
	}
	
	/**
	 * 문자전송 결과 상세보기
	 *
	 * @param pEgovMap
	 * @return 전송 결과 상세
	 * @throws HttpClientErrorException
	 * @throws ParseException 
	 * @throws NumberFormatException 
	 * @throws ClientProtocolException 
	 * @throws IOException 
	 */
	public JSONObject curlSendSmsList(int msgId) throws HttpClientErrorException, ParseException, NumberFormatException, ClientProtocolException, IOException {
		
		int resultCode 		= -101;	//결과코드, 정상 : 1, 오류 : 0 이하
		String message		= "";   //결과문구, 정상: success, 오류: 오류메세지
		String list			= "";
		String nextYn		= "";
		
		/*************  문자전송 결과 상세보기 *****************/
		/** SMS_CNT / LMS_CNT / MMS_CNT : 전송유형별 잔여건수 ***/
		
		/******************** 인증정보 ********************/
		String smsListUrl = String.valueOf(ConfigUtil.getProperty("sms.url")) + "/sms_list/";
		String smsId = String.valueOf(ConfigUtil.getProperty("sms.id"));
		String smsKey = String.valueOf(ConfigUtil.getProperty("sms.apiKey"));
		
		List<NameValuePair> sms = new ArrayList<NameValuePair>();
		
		sms.add(new BasicNameValuePair("user_id", smsId)); 		// SMS 아이디 
		sms.add(new BasicNameValuePair("key", smsKey)); 		//인증키
		/******************** 인증정보 ********************/
		
		sms.add(new BasicNameValuePair("mid", String.valueOf(msgId))); // 메세지ID
		//sms.add(new BasicNameValuePair("page", "1")); //조회 시작번호1 기본:1
		//sms.add(new BasicNameValuePair("page_size", "10")); //출력 갯수 기본:30
		
		HttpClient client = HttpClients.createDefault();
		HttpPost post = new HttpPost(smsListUrl);
		post.setEntity(new UrlEncodedFormEntity(sms, Charset.forName(encodingType)));
		
		HttpResponse res = client.execute(post);
		
		String result = "";
		if(res != null){
			BufferedReader in = new BufferedReader(new InputStreamReader(((CloseableHttpResponse) res).getEntity().getContent(), encodingType));
			try {
				String buffer = null;
				while((buffer = in.readLine())!=null){
					result += buffer;
				}	
			}catch( IOException ioe ){
				logger.error("curlSendSmsList IOException");
			}finally {
				in.close();
			}
		}
		
		JSONParser parser 	= new JSONParser();
		Object obj = parser.parse(result);
		JSONObject resultJson = (JSONObject)obj;
		 
		resultCode = Integer.parseInt(String.valueOf(resultJson.get("result_code"))) ;
		if(resultCode > 0){
			list = (String.valueOf(resultJson.get("list"))) ;
			nextYn = (String.valueOf(resultJson.get("error_cnt"))) ;
		} else {
			message = String.valueOf(resultJson.get("message"));
			logger.error("message :: " + message);
		}
		
		return resultJson;
	}
	
}
