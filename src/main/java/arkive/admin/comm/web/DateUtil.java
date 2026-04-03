/************************************
 * @class ID    : .java
 * @Description	: 날짜 공통유틸
 * @Author      : 이상우
 * @LastUpdate  : 2020.03.26
 */

package arkive.admin.comm.web;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateUtil {
	
	/**
	 * 현재 날짜를  yyyy 으로 변환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getYyyy() {
		Calendar cal = Calendar.getInstance();

		Locale currentLocale = new Locale("KOREAN", "KOREA");
		String pattern = "yyyy";
	    SimpleDateFormat formatter = new SimpleDateFormat(pattern, currentLocale);
	
	    return formatter.format(cal.getTime());
	}
	
	/**
	 * 현재 날짜를  yyyymm 으로 변환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getYyyyMM() {
		Calendar cal = Calendar.getInstance();

		Locale currentLocale = new Locale("KOREAN", "KOREA");
		String pattern = "yyyyMM";
	    SimpleDateFormat formatter = new SimpleDateFormat(pattern, currentLocale);
	
	    return formatter.format(cal.getTime());
	}
	
	/**
	 * 현재 날짜를  yyyyMMdd 으로 변환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getYyyymmdd(Calendar cal) {
		Locale currentLocale = new Locale("KOREAN", "KOREA");
		String pattern = "yyyyMMdd";
	    SimpleDateFormat formatter = new SimpleDateFormat(pattern, currentLocale);
	
	    return formatter.format(cal.getTime());
	}
	
	/**
	 * 현재  날자를  yyyymmdd 형태로 변환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getCurrentDate() {
		Date today = new Date();
	    Locale currentLocale = new Locale("KOREAN", "KOREA");
	    String pattern = "yyyyMMdd";
	    SimpleDateFormat formatter = new SimpleDateFormat(pattern, currentLocale);
	    
	    return formatter.format(today);
	}
	
	/**
	 * 현재  시각을  hhmmss 형태로 변환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getCurrentTime() {
		Date today = new Date();
	    Locale currentLocale = new Locale("KOREAN", "KOREA");
	    String pattern = "HHmmss";
	    SimpleDateFormat formatter = new SimpleDateFormat(pattern, currentLocale);
	    
	    return formatter.format(today);
	}
	
	/**
	 * GregorianCalendar 객체를 반환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static GregorianCalendar getGregorianCalendar(String yyyymmdd) {
		int yyyy = Integer.parseInt(yyyymmdd.substring(0, 4));
		int mm = Integer.parseInt(yyyymmdd.substring(4, 6));
		int dd = Integer.parseInt(yyyymmdd.substring(6));
		
		GregorianCalendar calendar = new GregorianCalendar(yyyy, mm - 1, dd, 0, 0, 0);
		return calendar;
	}
	
	/**
	 * 날짜를 Format에 맞게 변형해서 반환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getFormatedDate(String date, String pattern) {
		GregorianCalendar calDate = getGregorianCalendar(date);
		Locale currentLocale = new Locale("KOREAN", "KOREA");
	    SimpleDateFormat formatter = new SimpleDateFormat(pattern, currentLocale);
	    formatter.setCalendar(calDate);
	    
	    String dateFormatted = formatter.format(calDate.getTime());
	    
	    return dateFormatted;
	}
	
	/**
	 * 날짜를 Format에 맞게 변형해서 반환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getFormatedDateTime(String date) {
		
	    
	    String dateFormatted = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + " " + date.substring(8, 10) + ":" + date.substring(10, 12) + ":" + date.substring(12, 14);
	    
	    return dateFormatted;
	}
	
	/**
	 * 해당월의 마지막 일자를 반환한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getLastDayofMonth(String date) {
		GregorianCalendar calDate = getGregorianCalendar(date);
		
		String lastDay = date.substring(0, 6) + calDate.getActualMaximum(Calendar.DAY_OF_MONTH);
		
		return lastDay;
	}
	
	/**
	 * 지정된 플래그에 따라 연도 , 월 , 일자를 구한다.
	 *
	 * @param value
	 * @return 
	 */
	public static String getOpDate(int field, int amount, String date) {
		GregorianCalendar calDate = getGregorianCalendar(date);
		 
		if(field == Calendar.YEAR) {
			calDate.add(GregorianCalendar.YEAR, amount);
		}else if(field == Calendar.MONTH) {
		    calDate.add(GregorianCalendar.MONTH, amount);
		}else{
			calDate.add(GregorianCalendar.DATE, amount);
		}
		
		return getYyyymmdd(calDate);
	}
	
	/**
	 * 두 날짜간의 날짜수를 반환(윤년을 감안함)
	 *
	 * @param value
	 * @return 
	 */
	public static int getDifferDays(String startDate, String endDate) {
		GregorianCalendar StartDate = getGregorianCalendar(startDate);
	    GregorianCalendar EndDate = getGregorianCalendar(endDate);
	    long difer = (EndDate.getTime().getTime() - StartDate.getTime().getTime()) / (60*60*24*1000);
	
	    return (int)Math.abs(difer);
	}
	
	/**
	 * 현재의 요일을 구한다.
	 *  SUNDAY    	= 1
     *  MONDAY    	= 2
     *  TUESDAY  	= 3
     *  WEDNESDAY 	= 4
     *  THURSDAY  	= 5
     *  FRIDAY    	= 6
	 *
	 * @param value
	 * @return 
	 */
	public static int getDayOfWeek(){
		Calendar rightNow = Calendar.getInstance();
	    int day_of_week = rightNow.get(Calendar.DAY_OF_WEEK);
	    
	    return day_of_week;
	}
	
	/**
	 * 입력된 년월의 마지막 일수를 구한다.
	 *
	 * @param value
	 * @return 
	 */
	public static int getLastDayOfMon(int year, int month) {
		Calendar cal = Calendar.getInstance();
        cal.set(year, month-1, 1);
        
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
	}
}
