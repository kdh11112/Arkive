/************************************
 * @class    : ExcelUtil.java
 * @Description	: 엑셀업로드 공통유틸
 * @Author      : 이상우
 * @LastUpdate  : 2020.02.20
*/

package arkive.admin.comm.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionException;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddressList;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hssf.util.Region;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

import twitter4j.JSONArray;
import twitter4j.JSONObject;

public class ExcelUtil {
	
	protected final Logger logger = Logger.getLogger(getClass());		//log4j 사용 정의
	
	/* 정규표현식 패턴 */
	public static final String YYYYMM_PATTERN	= "^[1-2][0-9][0-9][0-9][0-1][0-9]$";	// yyyymm
	public static final String YYYYMMDD_PATTERN	= "^[1-2][0-9][0-9][0-9][0-1][0-9][0-3][0-9]$";	// yyyymmdd
	public static final String NUMBER_PATTERN 	= "^[0-9]*$";				// 숫자
	public static final String AMOUNT_PATTERN	= "^(0|-?[1-9][0-9]*)$";	// 금액
	public static final String YYYY_MM_DD_PATTERN	= "^(19|20)\\d{2}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[0-1])$";	// YYYY-MM-DD
	public static final String MTEL_PATTERN		= "^[0-9]{2,3}-[0-9]{3,4}-[0-9]{4}$";	// 전화번호
	public static final String AR_PATTERN		= "^[0-9]*\\.?[0-9]*$";	// 면적(소수점 포함),응답비율
	public static final String MOL_ACNTCTGR_ID_PATTERN	= "^MOL[0-3][0-9][0-9][0-9][0-9][0-9][0-9] $";	// MOL숫자7자리
	public static final String TEL_PATTERN		= "^[0-9]{2,4}-[0-9]{3,4}-[0-9]{4}$";	// 전화번호
	
	public static String AR_FORMAT = "#,###.##"; //천단위, 소수점
	public static String AMOUNT_FORMAT = "#,###.###"; //천단위, 소수점 3자리
	
	/**
	 * 엑셀 row의 cell 값을 구함  
	 *
	 * @param value
	 * @return 
	 */
	public static String getCellResult(Row row, int number) {
		Cell cell = null;
		String result = null;
		cell = row.getCell(number, Row.CREATE_NULL_AS_BLANK);
		
		try {
			if(cell.getCellType() == Cell.CELL_TYPE_FORMULA) {	// 수식
				result = cell.getCellFormula();
			}else if (cell.getCellType() != Cell.CELL_TYPE_STRING && DateUtil.isCellDateFormatted(cell)) {	// 날짜
				if (cell.getDateCellValue() != null) {
					Date date = row.getCell(number, Row.CREATE_NULL_AS_BLANK).getDateCellValue();
					result = new SimpleDateFormat("yyyyMMdd").format(date);
				}
			}else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
				result = cell.getStringCellValue();
			}else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				result = Long.toString((long) cell.getNumericCellValue());
			}
		}catch(SqlSessionException e) {
			Logger.getLogger("ExcelUtil").debug("오류발생");
		}
		
		if(result != null) {
			result = result.trim();
		}
		
		return result;
	}
	
	/**
	 * 엑셀 row의 수식으로 등록된 cell 값을 구함  
	 *
	 * @param value
	 * @return 
	 */
	private static CellValue formulaEvaluation(Workbook wbook, Cell cell) {
	    FormulaEvaluator formulaEval = wbook.getCreationHelper().createFormulaEvaluator();
	    return formulaEval.evaluate(cell);
	}
	
	/**
	 * 엑셀 row의 cell 값을 구함  
	 *
	 * @param value
	 * @return 
	 */
	public static String getCellResult(Workbook wbook, Row row, int number) {
		Cell cell = null;
		String result = null;
		
		try {
			cell = row.getCell(number, Row.CREATE_NULL_AS_BLANK);
			
			if(cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
				
				result = "";
			}else if(cell.getCellType() == Cell.CELL_TYPE_FORMULA) {	// 수식
				result = cell.getCellFormula();	
				
	            try {
	                CellValue objCellValue = formulaEvaluation(wbook, cell);
	                
	                if(objCellValue.getCellType() == Cell.CELL_TYPE_NUMERIC) {
	                    result = Long.toString((long) objCellValue.getNumberValue());
	                } else if(objCellValue.getCellType() == Cell.CELL_TYPE_STRING) {
	                    result = objCellValue.getStringValue();
	                } else if(objCellValue.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
	                    result = Boolean.toString(objCellValue.getBooleanValue());
	                } else {
	                    result = "";
	                }
	            } catch (Exception e) {//수식을 못읽을때
	                switch(cell.getCachedFormulaResultType()) {
	                    case Cell.CELL_TYPE_NUMERIC:
	                        if (DateUtil.isCellDateFormatted(cell)) {
	                            Date date = cell.getDateCellValue();
	                            result = new SimpleDateFormat("yyyyMMdd").format(date);
	                        } else {
	                            result = Long.toString((long)cell.getNumericCellValue());
	                        }
	                        break;
	                    case Cell.CELL_TYPE_STRING:
	                        result = cell.getStringCellValue();
	                        break;
	                    case Cell.CELL_TYPE_BOOLEAN:
	                        result = Boolean.toString(cell.getBooleanCellValue());
	                        break;
	                    case Cell.CELL_TYPE_BLANK:
	                        result = "";
	                        break;
	                    default:
	                        result = "";
	                }
	            }
				
			}else if (cell.getCellType() != Cell.CELL_TYPE_STRING && DateUtil.isCellDateFormatted(cell)) {	// 날짜
				
				if (cell.getDateCellValue() != null) {
					Date date = row.getCell(number, Row.CREATE_NULL_AS_BLANK).getDateCellValue();
					result = new SimpleDateFormat("yyyyMMdd").format(date);
				}
			}else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
				
				result = cell.getStringCellValue();
			}else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				
				result = Long.toString((long) cell.getNumericCellValue());
			}
		}catch(SqlSessionException e) {
			Logger.getLogger("ExcelUtil").debug("오류발생");
		}
		
		if(result != null) {
			result = result.trim();
		}
		
		return result;
	}
	
	/**
	 * 엑셀 row의  날짜가 등록된 cell 값을 구함  
	 *
	 * @param value
	 * @return 
	 */
	public static String getCellDateFormatResult(Workbook wbook, Row row, int number) {
		Cell cell = null;
		String result = null;
		
		try {
			cell = row.getCell(number, Row.CREATE_NULL_AS_BLANK);
			
			if(DateUtil.isCellDateFormatted(cell)) {	// 날짜
				if (cell.getDateCellValue() != null) {
					Date date = row.getCell(number, Row.CREATE_NULL_AS_BLANK).getDateCellValue();
					result = new SimpleDateFormat("yyyyMMdd").format(date);
				}
			}else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
				result = cell.getStringCellValue();
			}else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				Date date = cell.getDateCellValue();
				result = new SimpleDateFormat("yyyyMMdd").format(date);
			}
		}catch(SqlSessionException e) {
			Logger.getLogger("ExcelUtil").debug("오류발생");
		}
		
		if(result != null) {
			result = result.trim();
		}
		
		return result;
	}
	public static HSSFWorkbook createWorkBook(String sheetName, String title, String[] header, String[] colName, List<?> resultList) {

		HSSFRow row;
		HSSFCell cell;
		/** 엑셀 파일 생성 START */
		HSSFWorkbook workbook = new HSSFWorkbook();
		
		/** 스타일 입히기 START */
		HSSFCellStyle titleStyle = workbook.createCellStyle();
		HSSFCellStyle cellStyle = workbook.createCellStyle();
		HSSFCellStyle contentStyle = workbook.createCellStyle();
		
		// 타이틀 폰트
		HSSFFont titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		  
		// 컬럼명 폰트
		HSSFFont colNameFont = workbook.createFont();
		  
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		  
		// 내용 폰트
		HSSFFont contentFont = workbook.createFont();
		 
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		  
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		cellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		  
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		contentStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		contentStyle.setFont(contentFont);
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		HSSFSheet sheet = workbook.createSheet(sheetName);
		
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		// 공백
		row = sheet.createRow(sheet1_row);
		sheet1_row++;
		
		//숨김 처리
		List<Integer> hiddenColumns = new ArrayList<>();
		
		/** 헤더 START */
		row = sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		for (int i = 0; i < header.length; i++) {
			cell = row.createCell(i);
			
		    String headerName = header[i];
		    if (headerName.startsWith("hidden_")) {
		        headerName = headerName.substring("hidden_".length());
		        hiddenColumns.add(i);
		    }
			
			cell.setCellValue(header[i]);
			cell.setCellStyle(cellStyle);
		}
		sheet1_row++;
		/** 헤더 END */
		
		
		/** 데이터 셀 만들기 START */
		for (int i = 0; i < resultList.size(); i++) {
			EgovMap map = (EgovMap) resultList.get(i);
			row = sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);
				
			for (int j = 0; j < colName.length; j++) {
				cell = row.createCell(j);
				cell.setCellStyle(contentStyle);
				if (null != map.get(colName[j])) {
					cell.setCellValue(map.get(colName[j]).toString());
				} else {
					cell.setCellValue("");
				}
				
			}
			sheet1_row++;
		}
		
		// A열 숨김 처리 (인덱스 0)
		for (int colIndex : hiddenColumns) {
		    sheet.setColumnHidden(colIndex, true);
		}
		
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < colName.length; h++) {
			// 셀 사이즈 자동 조절
			sheet.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet.getColumnWidth(h)+512;			
			if(maxWidth > 25500){
				maxWidth = 25500;
			}
			sheet.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet.addMergedRegion(new Region(0,(short)0, 0,(short)(header.length-1)));
		
		return workbook;
	}
	
	public static HSSFWorkbook createWorkBook(String sheetName, String title, String[] header, String[] colName, List<?> resultList, List<?> codeList) {
		
		HSSFRow row;
		HSSFCell cell;
		/** 엑셀 파일 생성 START */
		HSSFWorkbook workbook = new HSSFWorkbook();
		
		/** 스타일 입히기 START */
		HSSFCellStyle titleStyle = workbook.createCellStyle();
		HSSFCellStyle cellStyle = workbook.createCellStyle();
		HSSFCellStyle contentStyle = workbook.createCellStyle();
		
		// 타이틀 폰트
		HSSFFont titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		
		// 컬럼명 폰트
		HSSFFont colNameFont = workbook.createFont();
		
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		
		// 내용 폰트
		HSSFFont contentFont = workbook.createFont();
		
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		cellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		contentStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		contentStyle.setFont(contentFont);
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		HSSFSheet sheet = workbook.createSheet(sheetName);
		
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		// 공백
		row = sheet.createRow(sheet1_row);
		sheet1_row++;
		
		/** 헤더 START */
		row = sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		for (int i = 0; i < header.length; i++) {
			cell = row.createCell(i);
			cell.setCellValue(header[i]);
			cell.setCellStyle(cellStyle);
		}
		sheet1_row++;
		/** 헤더 END */
		
		//드롭다운 목록
		JSONArray array = new JSONArray(codeList);
		String[] dropdownList = new String[array.length()];

		for (int i = 0; i < array.length(); i++) {
		    JSONObject item = array.optJSONObject(i); 
		    if (item != null) {
		    	dropdownList[i] = item.optString("realmCdNm", "");
		    }
		}
		
		String[] dropdownSttusList = {"신청중", "위촉", "보완요청"}; 		
		
		
		/** 데이터 셀 만들기 START */
		for (int i = 0; i < resultList.size(); i++) {
			EgovMap map = (EgovMap) resultList.get(i);
			row = sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);
			
			for (int j = 0; j < colName.length; j++) {
				cell = row.createCell(j);
				cell.setCellStyle(contentStyle);
				if (null != map.get(colName[j])) {
					cell.setCellValue(map.get(colName[j]).toString());
				} else {
					cell.setCellValue("");
				}
				
				if (colName[j].equals("전문분야1*") || j == 11) {
					
					CellRangeAddressList addressList = new CellRangeAddressList(sheet1_row,sheet1_row,j,j);
					
					HSSFDataValidationHelper dvHelper = new HSSFDataValidationHelper(sheet);
					
					DataValidationConstraint dvConstraint = DVConstraint.createExplicitListConstraint(dropdownList);
					
					DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
					
					validation.setShowErrorBox(true);
					
					sheet.addValidationData(validation);
				}
				
				if (colName[j].equals("전문분야2") || j == 13) {
					
					CellRangeAddressList addressList = new CellRangeAddressList(sheet1_row,sheet1_row,j,j);
					
					HSSFDataValidationHelper dvHelper = new HSSFDataValidationHelper(sheet);
					
					DataValidationConstraint dvConstraint = DVConstraint.createExplicitListConstraint(dropdownList);
					
					DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
					
					validation.setShowErrorBox(true);
					
					sheet.addValidationData(validation);
				}
				
				if (colName[j].equals("위촉상태") || j == 33) {
					
//					Object val11 = map.get(colName[11]);
//					Object val14 = map.get(colName[13]);
//					
//					List<String> tempDropdownList = new ArrayList<>();
//					
//					if (val11 != null) {
//						tempDropdownList.add(val11.toString());
//					}
//					
//					if (val14 != null) {
//						tempDropdownList.add(val14.toString());
//					}
//					
//					String[] dropdownRealmList = tempDropdownList.toArray(new String[0]);
//					
//					
//					if (dropdownRealmList != null && dropdownRealmList.length > 0) {
//						CellRangeAddressList addressList = new CellRangeAddressList(sheet1_row, sheet1_row, j, j);
//						
//						HSSFDataValidationHelper dvHelper = new HSSFDataValidationHelper(sheet);
//						
//						DataValidationConstraint dvConstraint = DVConstraint.createExplicitListConstraint(dropdownRealmList);
//						
//						DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
//						validation.setShowErrorBox(true);
//						sheet.addValidationData(validation);
//					}
					
					CellRangeAddressList addressList = new CellRangeAddressList(sheet1_row,sheet1_row,j,j);
					
					HSSFDataValidationHelper dvHelper = new HSSFDataValidationHelper(sheet);
					
					DataValidationConstraint dvConstraint = DVConstraint.createExplicitListConstraint(dropdownList);
					
					DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
					
					validation.setShowErrorBox(true);
					
					sheet.addValidationData(validation);
				}
				
				if (colName[j].equals("상태") || j == 34) {
					CellRangeAddressList addressList = new CellRangeAddressList(sheet1_row,sheet1_row,j,j);
					
					HSSFDataValidationHelper dvHelper = new HSSFDataValidationHelper(sheet);
					
					DataValidationConstraint dvConstraint = DVConstraint.createExplicitListConstraint(dropdownSttusList);
					
					DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);
					
					validation.setShowErrorBox(true);
					
					sheet.addValidationData(validation);
				}
			}
			sheet1_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < colName.length; h++) {
			// 셀 사이즈 자동 조절
			sheet.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet.getColumnWidth(h)+512;			
			if(maxWidth > 25500){
				maxWidth = 25500;
			}
			sheet.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet.addMergedRegion(new Region(0,(short)0, 0,(short)(header.length-1)));
		
		return workbook;
	}
	
	
	
	/**
	 * POI WORK 생성 Parameter : String 시트명, String 제목, String []헤더 , String[]헤더명, 리스트
	 * 
	 * 금액이 포함된 엑셀 다운로드용
	 * 
	 * @param sheetName
	 * @param title
	 * @param header
	 * @param colName
	 * @param resultList
	 * @return
	 *
	 *	사용 예제 >
	 *	response.setHeader("Content-Disposition", "attachment; filename=excelTotalResult.xls");
	 *
	 *	SXSSFWorkbook workbook = new SXSSFWorkbook();
	 *	String[] header = { "번호", "이름", "사번" }; 	// 헤더
	 *	String[] column = { "name", "emp_num" }; 	// 컬럼명
	 *	workbook = ExcelUtil.createWorkBook("사원 현황", "1. 사원명단", header, column, list);
	 * 
	 *	fileOut = response.getOutputStream(); 
	 *	workbook.write(fileOut);
	 *	fileOut.close();
	 */
	public static XSSFWorkbook createWorkBook(String sheetName, String title, String[] mergeHeader, String[] header, String[] colName, List<?> resultList, int firstMergeSize, int mergeSize, String userNm) {
		XSSFRow row;
		XSSFCell cell;

		/** 엑셀 파일 생성 START */
		XSSFWorkbook workbook = new XSSFWorkbook();
		
		/** 스타일 입히기 START */
		CellStyle titleStyle 	= workbook.createCellStyle();
		CellStyle cellStyle 	= workbook.createCellStyle();
		CellStyle contentStyle 	= workbook.createCellStyle();
		CellStyle contentAmountStyle = workbook.createCellStyle();
		CellStyle evlStyle 	= workbook.createCellStyle();
		
		CellStyle qyStyle 		= workbook.createCellStyle();
		
		// 타이틀 폰트
		Font titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		  
		// 컬럼명 폰트
		Font colNameFont = workbook.createFont();
		  
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		  
		// 내용 폰트
		Font contentFont = workbook.createFont();
		 
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		
		evlStyle.setFont(titleFont);
		evlStyle.setAlignment(XSSFCellStyle.ALIGN_RIGHT);
		evlStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		  
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);  
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		  
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentStyle.setAlignment(CellStyle.ALIGN_CENTER);
		contentStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		contentStyle.setFont(contentFont);
		contentStyle.setWrapText(true);
		
		contentAmountStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentAmountStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentAmountStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentAmountStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentAmountStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		contentAmountStyle.setFont(contentFont);

		// 수량 스타일
		qyStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		qyStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		qyStyle.setBorderTop(CellStyle.BORDER_THIN);  
		qyStyle.setBorderBottom(CellStyle.BORDER_THIN);
		qyStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		qyStyle.setFont(contentFont);
		DataFormat qyFormat = workbook.createDataFormat();
		qyStyle.setDataFormat(qyFormat.getFormat("#,##0"));
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		XSSFSheet sheet = (XSSFSheet)workbook.createSheet(sheetName);
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = (XSSFCell) row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		if(!userNm.equals("")) {
			row = (XSSFRow)sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x350); // 높이지정
			cell = (XSSFCell) row.createCell(0);
			cell.setCellValue("평가위원 : " + userNm);
			cell.setCellStyle(evlStyle);
			
			sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row, 0, (short)(header.length+3)));//rowspan
			sheet1_row++;
		}
		// 공백
		row = (XSSFRow)sheet.createRow(sheet1_row);
		sheet1_row++;
		
		/** 머지헤더 START */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for(int i=0; i < firstMergeSize; i++) {
			cell = (XSSFCell) row.createCell(i);
			cell.setCellStyle(cellStyle);
			if(i == 0) {
				cell.setCellValue(mergeHeader[0]);
			}
		}
		sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+1, 0, 3));//rowspan
		for(int i=0; i < mergeSize; i++) {
			cell = (XSSFCell) row.createCell(i+4);
			cell.setCellStyle(cellStyle);
			if(i == 0) {
				cell.setCellValue(mergeHeader[1]);
			}
		}
		sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row, 4, mergeSize+3));//colspan
		cell = (XSSFCell) row.createCell(mergeSize+4);
		cell.setCellStyle(cellStyle);
		cell.setCellValue(mergeHeader[2]);
		sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+1, mergeSize+4, mergeSize+4));//colspan
		/**머지헤더1 END**/
		
		sheet1_row++;
		
		/** 헤더 START */
		int headSize =  header.length;	
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for (int i = 0; i < headSize; i++) {
			cell = (XSSFCell) row.createCell(i+4);
			cell.setCellValue(header[i]);
			cell.setCellStyle(cellStyle);
			
		}
		/** 헤더 END */
		
		sheet1_row++;
		
		
		/** 데이터 셀 만들기 START */
		String preRowSpan 	= "";
		String preRowId		= "";
		String preStdId		= "";
		
		for (int i = 0; i < resultList.size(); i++) {
			EgovMap map = (EgovMap) resultList.get(i);
			row = (XSSFRow)sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);
			
			String rowspan = "";
			String subRowSpan = "";
			String rowId = map.get("rowId").toString();
			String stdId = map.get("stdId").toString();
			int count = 0;
			double sumAvg = 0;
			
			if(map.get("upRowCnt") != null){
				rowspan = map.get("upRowCnt").toString();
			}
			
			if(map.get("subRowCnt") != null){
				subRowSpan = map.get("subRowCnt").toString();
			}
			
			if(rowspan.equals("")) {
				rowspan = preRowSpan;
			}
			
			for (int j = 0; j < colName.length; j++) {
				cell = (XSSFCell) row.createCell(j);
				cell.setCellStyle(contentStyle);
				
				String conNm	= colName[j];

				if (null != map.get(colName[j])) {
					cell.setCellValue(map.get(colName[j]).toString().replace("<br>", "\n"));
					String tit = "calAvg";
					tit = tit + (j - 3)+"";
					if(j > 3 && !map.get(tit).toString().equals("0")) {
						sumAvg = sumAvg + Double.parseDouble(map.get(tit).toString());
						count++;
					}
				} else {
					cell.setCellValue("");
				}
				
				if(j == 1) {
					if(conNm.equals("methNm") && map.get(colName[j]).equals("합계")) {
						sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row, 1, 3));//rowspan
					}
				}
				if(conNm.equals("subDetStdNm") && null == map.get(colName[j]) && stdId.length() > 8) {
					sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row, 2, 3));//rowspan
				}
			}
			Integer number = Integer.parseInt(rowspan);
			Integer subNumber = Integer.parseInt(subRowSpan);
			
			if(!preRowId.equals(rowId)) {
				sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+number-1, 0, 0));//rowspan
			}
			
			if(!preStdId.equals(stdId) && stdId.length() > 8 && subNumber > 1) {

				sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+subNumber-1, 1, 1));//rowspan
				sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+subNumber-1, 2, 2));//rowspan
			}
			
			preRowSpan = rowspan;
			
			preRowId = rowId;
			preStdId = stdId;
			
			cell = (XSSFCell) row.createCell(colName.length);
			cell.setCellStyle(contentStyle);
			if(count == 0 ) {
				count = 1;
			}
			cell.setCellValue(Math.round(sumAvg/count*100)/100.0);
			sheet1_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < header.length+4; h++) {
			// 셀 사이즈 자동 조절
			sheet.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet.getColumnWidth(h)+512;			
			if(maxWidth > 30000){
				maxWidth = 30000;
			}
			sheet.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet.addMergedRegion(new CellRangeAddress(0,(short)0, 0,(short)(header.length+3)));
		
		return workbook;
	}
	
	/**
	 * POI WORK 생성 Parameter : String 시트명, String 제목, String []헤더 , String[]헤더명, 리스트
	 * 
	 * 금액이 포함된 엑셀 다운로드용
	 * 
	 * @param sheetName
	 * @param title
	 * @param header
	 * @param colName
	 * @param resultList
	 * @return
	 *
	 *	사용 예제 >
	 *	response.setHeader("Content-Disposition", "attachment; filename=excelTotalResult.xls");
	 *
	 *	SXSSFWorkbook workbook = new SXSSFWorkbook();
	 *	String[] header = { "번호", "이름", "사번" }; 	// 헤더
	 *	String[] column = { "name", "emp_num" }; 	// 컬럼명
	 *	workbook = ExcelUtil.createWorkBook("사원 현황", "1. 사원명단", header, column, list);
	 * 
	 *	fileOut = response.getOutputStream(); 
	 *	workbook.write(fileOut);
	 *	fileOut.close();
	 */
	public static XSSFWorkbook createWorkBook2(String sheetName, String title, String[] mergeHeader, String[] header, String[] colName, List<?> resultList, int firstMergeSize, int mergeSize) {

		XSSFRow row;
		XSSFCell cell;
		/** 엑셀 파일 생성 START */
		XSSFWorkbook workbook = new XSSFWorkbook();
		
		/** 스타일 입히기 START */
		CellStyle titleStyle 	= workbook.createCellStyle();
		CellStyle cellStyle 	= workbook.createCellStyle();
		CellStyle contentStyle 	= workbook.createCellStyle();
		CellStyle contentAmountStyle = workbook.createCellStyle();
		
		CellStyle qyStyle 		= workbook.createCellStyle();
		
		// 타이틀 폰트
		Font titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		  
		// 컬럼명 폰트
		Font colNameFont = workbook.createFont();
		  
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		  
		// 내용 폰트
		Font contentFont = workbook.createFont();
		 
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		  
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);  
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		  
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentStyle.setAlignment(CellStyle.ALIGN_CENTER);
		contentStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		contentStyle.setFont(contentFont);
		contentStyle.setWrapText(true);
		
		contentAmountStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentAmountStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentAmountStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentAmountStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentAmountStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		contentAmountStyle.setFont(contentFont);

		// 수량 스타일
		qyStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		qyStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		qyStyle.setBorderTop(CellStyle.BORDER_THIN);  
		qyStyle.setBorderBottom(CellStyle.BORDER_THIN);
		qyStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		qyStyle.setFont(contentFont);
		DataFormat qyFormat = workbook.createDataFormat();
		qyStyle.setDataFormat(qyFormat.getFormat("#,##0"));
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		XSSFSheet sheet = (XSSFSheet)workbook.createSheet(sheetName);
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = (XSSFCell) row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		// 공백
		row = (XSSFRow)sheet.createRow(sheet1_row);
		sheet1_row++;
		
		/** 머지헤더 START */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for(int i=0; i < firstMergeSize; i++) {
			cell = (XSSFCell) row.createCell(i);
			cell.setCellStyle(cellStyle);
			if(i == 0) {
				cell.setCellValue(mergeHeader[0]);
			}
		}
		sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+1, 0, 0));//rowspan
		
		for(int i=0; i < mergeSize; i++) {
			cell = (XSSFCell) row.createCell(i+1);
			cell.setCellStyle(cellStyle);
			if(i == 0) {
				cell.setCellValue(mergeHeader[1]);
			}
		}
		sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row, 1, mergeSize));//colspan
		
		cell = (XSSFCell) row.createCell(mergeSize+1);
		cell.setCellStyle(cellStyle);
		cell.setCellValue(mergeHeader[2]);
		sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+1, mergeSize+1, mergeSize+1));//colspan
		
		/**머지헤더1 END**/
		
		sheet1_row++;
		
		/** 헤더 START */
		int headSize =  header.length;	
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for (int i = 0; i < headSize; i++) {
			cell = (XSSFCell) row.createCell(i+1);
			cell.setCellValue(header[i]);
			cell.setCellStyle(cellStyle);
			
		}
		/** 헤더 END */
		
		sheet1_row++;
		
		
		/** 데이터 셀 만들기 START */
		for (int i = 0; i < resultList.size(); i++) {
			Map<String,Object> map = (Map<String,Object>) resultList.get(i);
			row = (XSSFRow)sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);
			int count = 0;
			double sumAvg = 0;
			for (int j = 0; j < colName.length; j++) {
				cell = (XSSFCell) row.createCell(j);
				cell.setCellStyle(contentStyle);
				
				if (null != map.get(colName[j])) {
					cell.setCellValue(map.get(colName[j]).toString());
					
					if(j > 0 && !map.get(colName[j]).toString().equals("0")) {
						sumAvg = sumAvg + Double.parseDouble(map.get(colName[j]).toString());
						count++;
					}
				} else {
					cell.setCellValue("");
				}
			}
			
			cell = (XSSFCell) row.createCell(colName.length);
			cell.setCellStyle(contentStyle);
			if(count == 0 ) {
				count = 1;
			}
			cell.setCellValue(Math.round(sumAvg/count*100)/100.0);
			
			sheet1_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < header.length; h++) {
			// 셀 사이즈 자동 조절
			sheet.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet.getColumnWidth(h)+512;			
			if(maxWidth > 30000){
				maxWidth = 30000;
			}
			sheet.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet.addMergedRegion(new CellRangeAddress(0,(short)0, 0,(short)(header.length+2)));
		
		return workbook;
	}
	
	/**
	 * POI WORK 생성 Parameter : String 시트명, String 제목, String []헤더 , String[]헤더명, 리스트
	 * 
	 * 금액이 포함된 엑셀 다운로드용
	 * 
	 * @param sheetName
	 * @param title
	 * @param header
	 * @param colName
	 * @param resultList
	 * @return
	 *
	 *	사용 예제 >
	 *	response.setHeader("Content-Disposition", "attachment; filename=excelTotalResult.xls");
	 *
	 *	SXSSFWorkbook workbook = new SXSSFWorkbook();
	 *	String[] header = { "번호", "이름", "사번" }; 	// 헤더
	 *	String[] column = { "name", "emp_num" }; 	// 컬럼명
	 *	workbook = ExcelUtil.createWorkBook("사원 현황", "1. 사원명단", header, column, list);
	 * 
	 *	fileOut = response.getOutputStream(); 
	 *	workbook.write(fileOut);
	 *	fileOut.close();
	 */
	public static XSSFWorkbook createWorkBook3(String sheetName, String title, String[] header, String[] colName, List<?> resultList) {
		XSSFRow row;
		XSSFCell cell;

		/** 엑셀 파일 생성 START */
		XSSFWorkbook workbook = new XSSFWorkbook();
		
		/** 스타일 입히기 START */
		CellStyle titleStyle 	= workbook.createCellStyle();
		CellStyle cellStyle 	= workbook.createCellStyle();
		CellStyle contentStyle 	= workbook.createCellStyle();
		CellStyle contentAmountStyle = workbook.createCellStyle();
		CellStyle evlStyle 	= workbook.createCellStyle();
		
		CellStyle qyStyle 		= workbook.createCellStyle();
		
		// 타이틀 폰트
		Font titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		  
		// 컬럼명 폰트
		Font colNameFont = workbook.createFont();
		  
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		  
		// 내용 폰트
		Font contentFont = workbook.createFont();
		 
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		
		evlStyle.setFont(titleFont);
		evlStyle.setAlignment(XSSFCellStyle.ALIGN_RIGHT);
		evlStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		  
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);  
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		  
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentStyle.setAlignment(CellStyle.ALIGN_CENTER);
		contentStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		contentStyle.setFont(contentFont);
		contentStyle.setWrapText(true);
		
		contentAmountStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentAmountStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentAmountStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentAmountStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentAmountStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		contentAmountStyle.setFont(contentFont);

		// 수량 스타일
		qyStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		qyStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		qyStyle.setBorderTop(CellStyle.BORDER_THIN);  
		qyStyle.setBorderBottom(CellStyle.BORDER_THIN);
		qyStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		qyStyle.setFont(contentFont);
		DataFormat qyFormat = workbook.createDataFormat();
		qyStyle.setDataFormat(qyFormat.getFormat("#,##0"));
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		XSSFSheet sheet = (XSSFSheet)workbook.createSheet(sheetName);
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = (XSSFCell) row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		// 공백
		row = (XSSFRow)sheet.createRow(sheet1_row);
		sheet1_row++;
		
		/** 헤더 START */
		int headSize =  header.length;	
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for (int i = 0; i < headSize; i++) {
			cell = (XSSFCell) row.createCell(i);
			cell.setCellValue(header[i]);
			cell.setCellStyle(cellStyle);
			
		}
		/** 헤더 END */
		
		sheet1_row++;
		
		
		/** 데이터 셀 만들기 START */
		
		for (int i = 0; i < resultList.size(); i++) {
			EgovMap map = (EgovMap) resultList.get(i);
			row = (XSSFRow)sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);

			for (int j = 0; j < colName.length; j++) {
				cell = (XSSFCell) row.createCell(j);
				cell.setCellStyle(contentStyle);
				
				String conNm	= colName[j];

				if (null != map.get(colName[j])) {
					cell.setCellValue(map.get(colName[j]).toString());
				} else {
					cell.setCellValue("");
				}
			}
			sheet1_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < header.length; h++) {
			// 셀 사이즈 자동 조절
			sheet.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet.getColumnWidth(h)+512;			
			
			if(maxWidth > 30000){
				maxWidth = 30000;
			}
			
			if(h == 0) {
				maxWidth = 3000;
			}
			sheet.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet.addMergedRegion(new CellRangeAddress(0,(short)0, 0,(short)(header.length-1)));
		
		return workbook;
	}
	
	/**
	 * POI WORK 생성 Parameter : String 시트명, String 제목, String []헤더 , String[]헤더명, 리스트
	 * 
	 * 금액이 포함된 엑셀 다운로드용
	 * 
	 * @param sheetName
	 * @param title
	 * @param header
	 * @param colName
	 * @param resultList
	 * @return
	 *
	 *	사용 예제 >
	 *	response.setHeader("Content-Disposition", "attachment; filename=excelTotalResult.xls");
	 *
	 *	SXSSFWorkbook workbook = new SXSSFWorkbook();
	 *	String[] header = { "번호", "이름", "사번" }; 	// 헤더
	 *	String[] column = { "name", "emp_num" }; 	// 컬럼명
	 *	workbook = ExcelUtil.createWorkBook("사원 현황", "1. 사원명단", header, column, list);
	 * 
	 *	fileOut = response.getOutputStream(); 
	 *	workbook.write(fileOut);
	 *	fileOut.close();
	 */
	public static XSSFWorkbook createWorkBook4(String sheetName, String title, String[] header, String[] colName, List<?> resultList, String userNm) {
		XSSFRow row;
		XSSFCell cell;

		/** 엑셀 파일 생성 START */
		XSSFWorkbook workbook = new XSSFWorkbook();
		
		/** 스타일 입히기 START */
		CellStyle titleStyle 	= workbook.createCellStyle();
		CellStyle cellStyle 	= workbook.createCellStyle();
		CellStyle contentStyle 	= workbook.createCellStyle();
		CellStyle contentAmountStyle = workbook.createCellStyle();
		CellStyle evlStyle 	= workbook.createCellStyle();
		
		CellStyle qyStyle 		= workbook.createCellStyle();
		
		// 타이틀 폰트
		Font titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		  
		// 컬럼명 폰트
		Font colNameFont = workbook.createFont();
		  
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		  
		// 내용 폰트
		Font contentFont = workbook.createFont();
		 
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		
		evlStyle.setFont(titleFont);
		evlStyle.setAlignment(XSSFCellStyle.ALIGN_RIGHT);
		evlStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		  
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);  
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		  
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentStyle.setAlignment(CellStyle.ALIGN_CENTER);
		contentStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		contentStyle.setFont(contentFont);
		contentStyle.setWrapText(true);
		
		contentAmountStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentAmountStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentAmountStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentAmountStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentAmountStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		contentAmountStyle.setFont(contentFont);

		// 수량 스타일
		qyStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		qyStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		qyStyle.setBorderTop(CellStyle.BORDER_THIN);  
		qyStyle.setBorderBottom(CellStyle.BORDER_THIN);
		qyStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		qyStyle.setFont(contentFont);
		DataFormat qyFormat = workbook.createDataFormat();
		qyStyle.setDataFormat(qyFormat.getFormat("#,##0"));
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		XSSFSheet sheet = (XSSFSheet)workbook.createSheet(sheetName);
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = (XSSFCell) row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		if(!userNm.equals("")) {
			row = (XSSFRow)sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x350); // 높이지정
			cell = (XSSFCell) row.createCell(0);
			cell.setCellValue("평가위원 : " + userNm);
			cell.setCellStyle(evlStyle);
			
			sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row, 0, (short)(header.length-1)));//rowspan
			sheet1_row++;
		}
		
		// 공백
		row = (XSSFRow)sheet.createRow(sheet1_row);
		sheet1_row++;
		
		/** 헤더 START */
		int headSize =  header.length;	
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for (int i = 0; i < headSize; i++) {
			cell = (XSSFCell) row.createCell(i);
			cell.setCellValue(header[i]);
			cell.setCellStyle(cellStyle);
			
		}
		/** 헤더 END */
		
		sheet1_row++;
		
		
		/** 데이터 셀 만들기 START */
		
		for (int i = 0; i < resultList.size(); i++) {
			EgovMap map = (EgovMap) resultList.get(i);
			row = (XSSFRow)sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);
			
			double sumAvg = 0;
			for (int j = 0; j < colName.length; j++) {
				cell = (XSSFCell) row.createCell(j);
				cell.setCellStyle(contentStyle);
				Integer aRnum = Integer.parseInt(map.get("aRnum").toString());
				Integer aRcnt = Integer.parseInt(map.get("aRcnt").toString());
				if (null != map.get(colName[j])) {
					if(j == 0) {
						if(aRnum == 1) {
							cell.setCellValue(map.get(colName[j]).toString());
							sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+aRcnt-1, 0, 0));//rowspan
						}
					}else {
						cell.setCellValue(map.get(colName[j]).toString());
					}
				} else {
					cell.setCellValue("");
				}
				
				if(j > 1) {
					sumAvg = sumAvg + Double.parseDouble(map.get(colName[j]).toString());
				}
				
				if(j == colName.length-1) {
					cell = (XSSFCell) row.createCell(j+1);
					cell.setCellStyle(contentStyle);
					cell.setCellValue(sumAvg);
				}
			}
			sheet1_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < header.length; h++) {
			// 셀 사이즈 자동 조절
			sheet.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet.getColumnWidth(h)+512;			
			
			if(maxWidth > 30000){
				maxWidth = 30000;
			}

			sheet.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet.addMergedRegion(new CellRangeAddress(0,(short)0, 0,(short)(header.length-1)));
		
		return workbook;
	}
	
	/**
	 * POI WORK 생성 Parameter : String 시트명, String 제목, String []헤더 , String[]헤더명, 리스트
	 * 
	 * 금액이 포함된 엑셀 다운로드용
	 * 
	 * @param sheetName
	 * @param title
	 * @param header
	 * @param colName
	 * @param resultList
	 * @return
	 *
	 *	사용 예제 >
	 *	response.setHeader("Content-Disposition", "attachment; filename=excelTotalResult.xls");
	 *
	 *	SXSSFWorkbook workbook = new SXSSFWorkbook();
	 *	String[] header = { "번호", "이름", "사번" }; 	// 헤더
	 *	String[] column = { "name", "emp_num" }; 	// 컬럼명
	 *	workbook = ExcelUtil.createWorkBook("사원 현황", "1. 사원명단", header, column, list);
	 * 
	 *	fileOut = response.getOutputStream(); 
	 *	workbook.write(fileOut);
	 *	fileOut.close();
	 */
	public static XSSFWorkbook createWorkBook5(String sheetName, String title, String[] mergeHeader, String[] header, String[] colName, List<?> resultList, int firstMergeSize, int mergeSize) {

		XSSFRow row;
		XSSFCell cell;
		/** 엑셀 파일 생성 START */
		XSSFWorkbook workbook = new XSSFWorkbook();
		
		/** 스타일 입히기 START */
		CellStyle titleStyle 	= workbook.createCellStyle();
		CellStyle cellStyle 	= workbook.createCellStyle();
		CellStyle contentStyle 	= workbook.createCellStyle();
		CellStyle contentAmountStyle = workbook.createCellStyle();
		
		CellStyle qyStyle 		= workbook.createCellStyle();
		
		// 타이틀 폰트
		Font titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		  
		// 컬럼명 폰트
		Font colNameFont = workbook.createFont();
		  
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		  
		// 내용 폰트
		Font contentFont = workbook.createFont();
		 
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
		  
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);  
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		  
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentStyle.setAlignment(CellStyle.ALIGN_CENTER);
		contentStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER); // 세로 가운데 정렬
		contentStyle.setFont(contentFont);
		contentStyle.setWrapText(true);
		
		contentAmountStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		contentAmountStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		contentAmountStyle.setBorderTop(CellStyle.BORDER_THIN);  
		contentAmountStyle.setBorderBottom(CellStyle.BORDER_THIN);
		contentAmountStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		contentAmountStyle.setFont(contentFont);

		// 수량 스타일
		qyStyle.setBorderRight(CellStyle.BORDER_THIN);              //테두리 설정    
		qyStyle.setBorderLeft(CellStyle.BORDER_THIN);    
		qyStyle.setBorderTop(CellStyle.BORDER_THIN);  
		qyStyle.setBorderBottom(CellStyle.BORDER_THIN);
		qyStyle.setAlignment(CellStyle.ALIGN_RIGHT);
		qyStyle.setFont(contentFont);
		DataFormat qyFormat = workbook.createDataFormat();
		qyStyle.setDataFormat(qyFormat.getFormat("#,##0"));
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		XSSFSheet sheet = (XSSFSheet)workbook.createSheet(sheetName);
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = (XSSFCell) row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		// 공백
		row = (XSSFRow)sheet.createRow(sheet1_row);
		sheet1_row++;
		
		/** 머지헤더 START */
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for(int i=0; i < mergeHeader.length; i++) {
			cell = (XSSFCell) row.createCell(i);
			cell.setCellStyle(cellStyle);
			cell.setCellValue(mergeHeader[i]);
			if(i < mergeHeader.length -1) {
				sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row+1, i, i));//rowspan
			}
		}
		
		sheet.addMergedRegion(new CellRangeAddress(sheet1_row, sheet1_row, mergeSize, firstMergeSize+mergeSize));//colspan
		/**머지헤더1 END**/
		
		sheet1_row++;
		
		/** 헤더 START */
		int headSize =  header.length;	
		row = (XSSFRow)sheet.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		
		for (int i = 0; i < headSize; i++) {
			cell = (XSSFCell) row.createCell(i+mergeSize);
			cell.setCellValue(header[i]);
			cell.setCellStyle(cellStyle);
			
		}
		
		
		/** 헤더 END */
		
		sheet1_row++;
		
		
		/** 데이터 셀 만들기 START */
		for (int i = 0; i < resultList.size(); i++) {
			Map<String,Object> map = (Map<String,Object>) resultList.get(i);
			row = (XSSFRow)sheet.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);
			int count = 0;
			double sumAvg = 0;
			for (int j = 0; j < colName.length; j++) {
				cell = (XSSFCell) row.createCell(j);
				cell.setCellStyle(contentStyle);
				
				if (null != map.get(colName[j])) {
					cell.setCellValue(map.get(colName[j]).toString());
				} else {
					cell.setCellValue("");
				}
			}
			
			sheet1_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < header.length; h++) {
			// 셀 사이즈 자동 조절
			sheet.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet.getColumnWidth(h)+512;			
			if(maxWidth > 30000){
				maxWidth = 30000;
			}
			sheet.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet.addMergedRegion(new CellRangeAddress(0,(short)0, 0,(short)(header.length+2)));
		
		return workbook;
	}

	public static HSSFWorkbook createWorkBookSheet(String sheetName1, String title1, String[] header1, String[] colName1, List<?> resultList1, String sheetName2, String[] header2, String[] colName2, List<?> resultList2) {
		HSSFRow row;
		HSSFCell cell;
		/** 엑셀 파일 생성 START */
		HSSFWorkbook workbook = new HSSFWorkbook();
		
		/** 스타일 입히기 START */
		HSSFCellStyle titleStyle 	= workbook.createCellStyle();
		HSSFCellStyle cellStyle 	= workbook.createCellStyle();
		HSSFCellStyle contentStyle 	= workbook.createCellStyle();
		HSSFCellStyle contentStyle2 = workbook.createCellStyle();
		HSSFCellStyle arStyle 		= workbook.createCellStyle();
		HSSFCellStyle rateStyle 	= workbook.createCellStyle();
		HSSFCellStyle amountStyle 	= workbook.createCellStyle();
		HSSFCellStyle srchStyle		= workbook.createCellStyle();
		
		// 검색어 폰트
		HSSFFont srchFont = workbook.createFont();
		
		srchFont.setFontHeightInPoints((short)12);
		srchFont.setFontName("맑은 고딕");
		
		// 타이틀 폰트
		HSSFFont titleFont = workbook.createFont();
		titleFont.setFontHeightInPoints((short)13);
		titleFont.setFontName("맑은 고딕");
		  
		// 컬럼명 폰트
		HSSFFont colNameFont = workbook.createFont();
		  
		colNameFont.setFontHeightInPoints((short)10);
		colNameFont.setFontName("맑은 고딕");
		  
		// 내용 폰트
		HSSFFont contentFont = workbook.createFont();
		 
		/** 타이틀 폰트 스타일 지정 */
		titleStyle.setFont(titleFont);
		titleStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		titleStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		  
		/** 컬럼 셀 테두리 / 폰트 스타일 지정 */
		cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index); // 셀 색상
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		cellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		cellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		cellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);  
		cellStyle.setFont(colNameFont);
		  
		/** 내용 셀 테두리 / 폰트 지정 */
		contentStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		contentStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		contentStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		contentStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		contentStyle.setFont(contentFont);
		
		contentStyle2.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		contentStyle2.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		contentStyle2.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		contentStyle2.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		contentStyle2.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		contentStyle2.setFont(contentFont);
		contentStyle2.setFillForegroundColor(IndexedColors.TAN.index);
		contentStyle2.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		
		srchStyle.setBorderRight(HSSFCellStyle.BORDER_NONE);              //테두리 설정    
		srchStyle.setBorderLeft(HSSFCellStyle.BORDER_NONE);    
		srchStyle.setBorderTop(HSSFCellStyle.BORDER_NONE);  
		srchStyle.setBorderBottom(HSSFCellStyle.BORDER_NONE);
		srchStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		srchStyle.setFont(srchFont);
		
		// 면적 스타일
		arStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		arStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		arStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		arStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		arStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		arStyle.setFont(contentFont);
		DataFormat arFormat = workbook.createDataFormat();
		arStyle.setDataFormat(arFormat.getFormat("0.00"));
		// 비율 스타일
		rateStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		rateStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		rateStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		rateStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		rateStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		rateStyle.setFont(contentFont);
		DataFormat rateFormat = workbook.createDataFormat();
		rateStyle.setDataFormat(rateFormat.getFormat("0.000"));
		// 금액 스타일
		amountStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);              //테두리 설정    
		amountStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);    
		amountStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);  
		amountStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		amountStyle.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		amountStyle.setFont(contentFont);
		//amountStyle.setDataFormat((short)3);	// 천단위 콤마
		DataFormat amountFormat = workbook.createDataFormat();
		amountStyle.setDataFormat(amountFormat.getFormat("#,###,###,###0"));
		/** 스타일 입히기 END */
		
		/** 시트 생성 */
		HSSFSheet sheet1 = workbook.createSheet(sheetName1);
		
		
		// 행 인덱스
		int sheet1_row = 0;
		
		/** 제목 */
		row = sheet1.createRow(sheet1_row);
		row.setHeight((short)(short) 0x350); // 높이지정
		cell = row.createCell(0);
		cell.setCellValue(title1);
		cell.setCellStyle(titleStyle);
		sheet1_row++;
		
		/** 헤더 START */
		row = sheet1.createRow(sheet1_row);
		row.setHeight((short)(short) 0x150);
		for (int i = 0; i < header1.length; i++) {
			cell = row.createCell(i);
			cell.setCellValue(header1[i]);
			cell.setCellStyle(cellStyle);
		}
		sheet1_row++;
		/** 헤더 END */
		
		/** 데이터 셀 만들기 START */
		for (int i = 0; i < resultList1.size(); i++) {
			Map<String, Object> map = (Map<String, Object>) resultList1.get(i);
			row = sheet1.createRow(sheet1_row);
			row.setHeight((short)(short) 0x150);
				
			String reqstSttus = map.get("reqstSttus") != null ? map.get("reqstSttus").toString() : "";
			for (int j = 0; j < colName1.length; j++) {
				cell = row.createCell(j);
				//신청완료 , 보완완료 , 위촉예정
				if("01".equals(reqstSttus) || "04".equals(reqstSttus) || "07".equals(reqstSttus)) {
					cell.setCellStyle(contentStyle2);
				}else {
					cell.setCellStyle(contentStyle);
					
				}
				if (null != map.get(colName1[j])) {
					
					if(map.get(colName1[j]) != null) {
						cell.setCellValue(map.get(colName1[j]).toString());
					}
				} else {
					cell.setCellValue("");
				}
			}
			sheet1_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < colName1.length; h++) {
			// 셀 사이즈 자동 조절
			sheet1.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet1.getColumnWidth(h)+512;			
			if(maxWidth > 25500){
				maxWidth = 25500;
			}
			sheet1.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet1.addMergedRegion(new Region(0,(short)0, 0,(short)(header1.length-1)));
		
		
		HSSFSheet sheet2 = workbook.createSheet(sheetName2);
		
		
		// 행 인덱스
		int sheet2_row = 1;
		
		// 공백
		row = sheet2.createRow(sheet2_row);
		sheet2_row++;
		
		/** 헤더 START */
		row = sheet2.createRow(sheet2_row);
		row.setHeight((short)(short) 0x150);
		for (int i = 0; i < header2.length; i++) {
			cell = row.createCell(i + 1);
			cell.setCellValue(header2[i]);
			cell.setCellStyle(cellStyle);
		}
		sheet2_row++;
		/** 헤더 END */
		
		/** 데이터 셀 만들기 START */
		for (int i = 0; i < resultList2.size(); i++) {
			Map<String, Object> map = (Map<String, Object>) resultList2.get(i);
			row = sheet2.createRow(sheet2_row);
			row.setHeight((short)(short) 0x150);
			
			for (int j = 0; j < colName2.length; j++) {
				cell = row.createCell(j + 1);
				cell.setCellStyle(contentStyle);
				if (null != map.get(colName2[j])) {
					
					if(map.get(colName2[j]) != null) {
						cell.setCellValue(map.get(colName2[j]).toString());
					}
				} else {
					cell.setCellValue("");
				}
			}
			sheet2_row++;
		}
		/** 데이터 셀 만들기 END */
		
		/** 셀 크기 조정 START */
		for (int h = 0; h < colName2.length; h++) {
			// 셀 사이즈 자동 조절
			sheet2.autoSizeColumn((short)h);
			// 셀 맥스 사이즈 설정
			int maxWidth = sheet2.getColumnWidth(h)+512;			
			if(maxWidth > 25500){
				maxWidth = 25500;
			}
			sheet2.setColumnWidth(h, maxWidth);
		}
		/** 셀 크기 조정 END */
		
		sheet2.addMergedRegion(new Region(0,(short)0, 0,(short)(header2.length-1)));
		
		return workbook;
	}
	
	/**
	 * 엑셀시트의 첫번째 컬럼의 cell병합  
	 *
	 * @param value
	 * @return 
	 */
	public static void setMergeFirstCell(HSSFSheet sheet, int mergeStartRow) {
		// 셀 병합
		int iRowCnt = sheet.getLastRowNum();
		
		String strPreBlockNm 	= ""; 	// 이전행 블록명
		String strBlockNm 		= ""; 	// 현재행 블록명
		
		int iMergeStartIdx 	= 0;		// 지구명 셀병합 시작행
		int iMergeEndIdx 	= 0;		// 지구명 셀병합 종료행
		
		for(int i=mergeStartRow; i<=iRowCnt+1; i++) {
			HSSFRow row = sheet.getRow(i);
			
			if(row != null) {
				HSSFCell cell = row.getCell(0);		// 구분 셀
				
				if(cell != null) {
					strBlockNm = cell.getStringCellValue();
					
					if("".equals(strPreBlockNm)) {
						strPreBlockNm 	= strBlockNm;
						
						if(i==3) {
							iMergeStartIdx	= i;
						}else {
							iMergeStartIdx	= i-1;
						}
					}
					
					if(!"".equals(strPreBlockNm) && !strPreBlockNm.equals(strBlockNm)) {
						strPreBlockNm 	= "";
						iMergeEndIdx 	= i-1;
						
						sheet.addMergedRegion(new CellRangeAddress(iMergeStartIdx,iMergeEndIdx,0,0));
						
						iMergeStartIdx = i;
					}
				}
			}else {
				iMergeEndIdx = i-2;
				
				sheet.addMergedRegion(new CellRangeAddress(iMergeStartIdx,iMergeEndIdx,0,0));
				
			}
		}
	}
	
	/**
	 * 동적으로 컬럼 변경하는 경우 엑셀 row의 cell 값을 구함  
	 *
	 * @param value
	 * @return 
	 */
	public static void setResizeCellWith(HSSFSheet sheet, List<String> colName) {
		
		if(sheet != null && colName != null && colName.size() > 0) {
			/** 셀 크기 조정 START */
			for (int h = 0; h < colName.size(); h++) {
				// 셀 사이즈 자동 조절
				sheet.autoSizeColumn((short)h);
				// 셀 맥스 사이즈 설정
				int maxWidth = sheet.getColumnWidth(h)+512;			
				if(maxWidth > 25500){
					maxWidth = 25500;
				}
				sheet.setColumnWidth(h, maxWidth);
			}
		}
	}

}
