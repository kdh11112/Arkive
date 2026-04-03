package arkive.admin.comm.web;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionUtil {

   	//protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final static   Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
    
	public static byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() % 2 != 0) {
			return new byte[] {};
		}

		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < hex.length(); i += 2) {
			byte value = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
			bytes[(int) Math.floor(i / 2)] = value;
		}
		return bytes;
	}

	public static String byteArrayToHex(byte[] ba) {
		if (ba == null || ba.length == 0) {
			return null;
		}
		StringBuffer sb = new StringBuffer(ba.length * 2);

		for (int x = 0; x < ba.length; x++) {
			String hexNumber = "0" + Integer.toHexString(0xff & ba[x]);
			sb.append(hexNumber.substring(hexNumber.length() - 2));
		}
		return sb.toString();
	}

	
	/**
     * SHA256 암호화
     * 
     * @param planText
     * @return 
     * @throws Exception
     */
    public static String encryptionSHA256(String planText) throws NoSuchAlgorithmException { 
    	try{ 
    		MessageDigest md = MessageDigest.getInstance("SHA-256"); 
    		md.update(planText.getBytes()); 
    		byte byteData[] = md.digest(); 
    		StringBuffer sb = new StringBuffer(); 
    		for (int i = 0; i < byteData.length; i++) { 
    			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1)); 
    		} 
    		StringBuffer hexString = new StringBuffer(); 
    		for (int i=0;i<byteData.length;i++) { 
    			String hex=Integer.toHexString(0xff & byteData[i]); 
    			if(hex.length()==1){ 
    				hexString.append('0'); 
    			} hexString.append(hex); 
    		} 
    		return hexString.toString(); 
    	}catch(NoSuchAlgorithmException e){ 
    		throw e;
    	} 
    }

}
