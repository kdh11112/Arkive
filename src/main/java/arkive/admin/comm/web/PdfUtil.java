package arkive.admin.comm.web;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Component;

import com.itextpdf.io.codec.Base64;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TabAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;

import arkive.admin.comm.service.EgovProperties;

@Component("pdfUtil")
public class PdfUtil {
	CommUtil cmmUtil = new CommUtil();
	PdfFont baseFont;
	
	public static int FONT_SIZE_20 = 20;
	public static int FONT_SIZE_24 = 24;
	public static int FONT_SIZE_30 = 30;
	public static int FONT_SIZE_15 = 15;
	public static int FONT_SIZE_14 = 14;
	public static int FONT_SIZE_13 = 13;
	public static int FONT_SIZE_12 = 12;
	public static int FONT_SIZE_11 = 11;
	public static int FONT_SIZE_10 = 10;
	public static int FONT_SIZE_9 = 9;
	public static int FONT_SIZE_1 = 1;
	public static int MARGIN = 20;
	
	//@Resource(name = "evlService")
	//private EvlService evlService;
	
	/**
	 * pdf 텍스트 생성
	 * @param value 텍스트 값
	 * @param fontSize 폰트 크기
	 * @param bold 폰트 bold 여부
	 * @return Text
	 * @throws
	*/	
	public Text makeText(String value, float fontSize, boolean bold) {
		Text text = new Text(value).setFont(baseFont).setFontSize(fontSize);
		if(bold) {
			text.setBold();
		}
		return text;
		
	}
	/**
	 * pdf 텍스트 생성
	 * @param String 텍스트 값
	 * @param float 폰트 크기
	 * @param boolean 폰트 bold 여부
	 * @param Color 폰트 색깔
	 * @return Text
	 * @throws
	*/	
	public Text makeText(String value, float fontSize, boolean bold, Color color) {
		Text text = new Text(value).setFont(baseFont).setFontSize(fontSize).setFontColor(color);
		if(bold) {
			text.setBold();
		}
		return text;
		
	}
	
	/**
	 * pdf 셀 생성
	 * @param Paragraph 
	 * @param int rowspan
	 * @param int colspan
	 * @param boolean 텍스트 가운데 정렬
	 * @param boolean vertical 가운데 정렬
	 * @return Cell
	 * @throws
	*/	
	public Cell makeCell(Paragraph value, int rowspan, int colspan, boolean textCenter, boolean verticalMiddle) {
		Cell cell = new Cell(rowspan, colspan).add(value);
		
		if(textCenter) {
			cell.setTextAlignment(TextAlignment.CENTER);
		}
		if(verticalMiddle) {
			cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
		}
		
    	return cell;
    }
	
	public String createInsertDocumentFile(List<EgovMap> documentList) throws IOException{
		String lsFncCn = "서류 생성 ";
		
		try {
			String fileRealPath = EgovProperties.getProperty("Globals.FILE_REAL_PATH");
			String jobId = "PD";
			String fileKnd = "pdf";
			String fileId = cmmUtil.getFileId();
			
			String filePath = cmmUtil.filePathBlackList(fileRealPath + File.separator + jobId + File.separator + fileId);		//GlobalsPath로 수정해야함.
			
			File file = new File(filePath);
			
			if (file == null || file.getParentFile() == null) {
				throw new IOException("file.getParentFile() is null");
			}
			
			// 디렉토리 생성
			if (!file.getParentFile().exists()) {
				if(file.getParentFile().mkdirs()){
				}
			}
			baseFont = PdfFontFactory.createFont(
				    getClass().getResource("/font/NanumGothic.ttf").getPath(),
				    PdfEncodings.IDENTITY_H,
				    true   // 폰트 내장(Embed)
				);
			
			// step 1
			PdfWriter writer = new PdfWriter(filePath);
			PdfDocument pdf = new PdfDocument(writer);
			Document document = new Document(pdf,PageSize.A4);
			document.setMargins(30f,50f,50f,50f); //상화좌우 마진
			
			SolidLine line = new SolidLine(1f);
		    LineSeparator ls = new LineSeparator(line);
			
			float documentLRMargin = document.getLeftMargin()+document.getRightMargin(); //좌우 마진
			float documentTBMargin = document.getTopMargin()+document.getBottomMargin(); //상하 마진
			float fullWidth = pdf.getDefaultPageSize().getWidth()-documentLRMargin;		 //문서 width
			float fullHeight = pdf.getDefaultPageSize().getHeight()-documentTBMargin;	 //문서 height
			Text newLine = new Text("\n");
			
			if(documentList.size() > 0) {
				for(int i=0; i < documentList.size(); i++) {
					EgovMap submitMap = (EgovMap)documentList.get(i);
					String title = (String)submitMap.get("submitTit");
					String sign = (String)submitMap.get("sign");
					byte[] signBytes = Base64.decode(sign);
					ImageData signData = ImageDataFactory.create(signBytes);
		            Image signImage = new Image(signData);
		            signImage.scaleToFit(50, 50); // 가로 100, 세로 100으로 크기 조절

					if(title.equals("평가위원 위촉 동의 및 보안·청렴각서")) {
						if(i > 0) {
							document.add(new AreaBreak());
						}
						PdfPage page = pdf.addNewPage();
						// 사각형 좌표 지정: (x, y, width, height)
						float x = 50f;
						float y = 50f;
						float width = 500f;
						float height = 750f;
						Rectangle rect = new Rectangle(x, y, width, height);
						
						// 사각형 경로 그리기 및 선 적용(stroke)
						PdfCanvas pdfCanvas = new PdfCanvas(page);
						pdfCanvas.setLineWidth(1f);
						pdfCanvas.setStrokeColor(Color.BLACK);
						pdfCanvas.rectangle(rect);
						pdfCanvas.stroke();
						
						
						Canvas canvas = new Canvas(pdfCanvas, pdf, rect);
						
						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_24, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
						titleParagraph.setMarginBottom(MARGIN);
						canvas.add(titleParagraph);
						
						Paragraph contPar = new Paragraph();
						contPar.setMarginLeft(20);
						contPar.setMarginRight(20);
						Text contText1 = makeText("본인은 " + (String)submitMap.get("dlbrtNm") + " 평가위원으로 위촉됨을 동의하며 다음 사항을 준수할 것을 서약합니다.", FONT_SIZE_15, false);
						Text contText2 = makeText("1. 본인은 " + (String)submitMap.get("dlbrtNm") + " 평가함에 있어 심사 상의 제반 보안사항을 철저히 이행할 것 임", FONT_SIZE_15, false);
						Text contText3 = makeText("2. 본인은 보안사항을 외부에 누설시켜 중대한 문제점을 야기 시켰을 경우에는 보안관계 제 법규에 의거 처벌 받음은 물론 어떠한 제재 조치를 당하여도 이의를 제기하지 않을 것임", FONT_SIZE_15, false);
						Text contText4 = makeText("3. 이유여하를 막록하고 금품, 향응이나 부당한 이익제공을 요구하지 않고 받지 않으며 만약 위반할 시에는 「건설기술진흥법」제84조(벌칙 적용 시의 공무원 의제) 등 관련법령에 따라 처벌받음은 물론 어떠한 제재조치를 당하여도 이의를 제기하지 않을 것임", FONT_SIZE_15, false);
						Text contText5 = makeText("4. 상기 안건과 관련하여 용역수주, 설계자문, 연구 등 어떠한 형태로든 참여치 않았으며, 또한 최근 3년 이내에 당해 평가대상 업체에 재직한 사실이 없음을 확인하며, 만약 제척·기피·회피 대상임에도 미신고 사실이 확인될 경우 GH 평가위원 3년 박탈은 물론 일체의 민·형사상 책임질 것을 확약함", FONT_SIZE_15, false);
						contPar.add(contText1).add(newLine).add(newLine).add(contText2).add(newLine).add(newLine).add(contText3).add(newLine).add(newLine).add(contText4).add(newLine).add(newLine).add(contText5).add(newLine).add(newLine);
						canvas.add(contPar);
						
						Paragraph dayPar = new Paragraph();
						dayPar.setMarginRight(20);
						Text contText6 = makeText((String)submitMap.get("registDt"), FONT_SIZE_15, false);
						
						dayPar.add(new Tab());
						dayPar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						dayPar.add(contText6).add(newLine);
						canvas.add(dayPar);
						
						Paragraph namePar = new Paragraph();
						namePar.setMarginRight(20);
						Text contText7 = makeText("평가위원 " + (String)submitMap.get("mfcmmNm"), FONT_SIZE_15, false);
						namePar.add(new Tab());
						namePar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						namePar.add(contText7).add(signImage).add(newLine);
						canvas.add(namePar);
						
						Paragraph orgPar = new Paragraph();
						orgPar.setMarginLeft(20);
						Text contText8 = makeText("경기주택도시공사 귀중", FONT_SIZE_20, true);
						orgPar.add(newLine).add(contText8);
						canvas.add(orgPar);
						
						canvas.close();
						
					}else if(title.equals("당사자 제척·기피·회피 신청서")) {
						document.add(new AreaBreak());
						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_24, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
						titleParagraph.setMarginBottom(MARGIN);						
						document.add(titleParagraph);
						
						Paragraph dlbrtPar = new Paragraph();
						Text dlbrtText = makeText("□ 안건명 : " + (String)submitMap.get("dlbrtNm"), FONT_SIZE_15, false);
						dlbrtPar.add(dlbrtText).add(newLine).add(newLine);
						document.add(dlbrtPar);
						
						Paragraph depPar = new Paragraph();
						depPar.setMarginLeft(20);
						depPar.setMarginRight(20);
						Text depText11 = makeText("○ 본인은 위 심의안건의 당사자로서 아래에 해당하는 사유가" + (String)submitMap.get("dlbrtNm"), FONT_SIZE_15, false);
						Text depText12;
						String chk = (String)submitMap.get("chk1");
						if(chk.equals("N")) {
							depText12 = makeText("(□있음, ▣없음)을 확인하며, 해당 대상에 포함될 경우 위원", FONT_SIZE_15, false);
						}else {
							depText12 = makeText("(▣있음, □없음)을 확인하며, 해당 대상에 포함될 경우 위원", FONT_SIZE_15, false);
						}
						Text depText13 = makeText("해촉은 물론 일체의 민ㆍ형사상 책임을 질 것을 확약합니다.", FONT_SIZE_15, false);
						Text depText2 = makeText("○ 기피ㆍ회피 사유(있는 경우만 작성) : " + (String)submitMap.get("cont1"), FONT_SIZE_15, false);
						
						depPar.add(depText11).add(newLine).add(depText12).add(depText13).add(newLine).add(newLine).add(depText2).add(newLine);
						document.add(depPar);
						document.add(ls); //문서에 라인 추가
						
						Paragraph contPar = new Paragraph();
						
						Text contText1 = makeText("<위원의 제척ㆍ기피ㆍ회피>", FONT_SIZE_13, true);
						Text contText2 = makeText("1. 위원 또는 그 배우자나 배우자였던 사람이 해당 안건의 당사자가 되거나 그 안건의 당사자와 공동권리자 또는 공동의무자인 경우", FONT_SIZE_13, false);
						Text contText3 = makeText("2. 위원이 해당 안건의 당사자와 친족이거나 친족이었던 경우", FONT_SIZE_13, false);
						Text contText4 = makeText("3. 위원이 해당 심의 대상인 건설공사의 시행으로 이해당사자(대리관계를 포함한다)가 되는 경우", FONT_SIZE_13, false);
						Text contText5 = makeText("4. 위원이나 위원이 속한 법인·단체 등이 해당 안건의 당사자의 대리인이거나 대리인이었던 경우", FONT_SIZE_13, false);
						Text contText6 = makeText("5. 위원이 최근 3년 이내에 해당 심의 대상 업체에 임원 또는 직원으로 재직한 경우", FONT_SIZE_13, false);
						Text contText7 = makeText("6. 위원이 해당 안건에 대하여 자문, 연구, 용역(하도급을 포함한다. 이하 이 항에서 같다), 감정(鑑定) 또는 조사를 한 경우", FONT_SIZE_13, false);
						Text contText8 = makeText("7. 위원이 임원 또는 직원으로 재직하고 있거나 최근 3년 내에 재직하였던 기업 등이 해당 안건에 관하여 자문, 연구, 용역, 감정 또는 조사를 한 경우", FONT_SIZE_13, false);
						Text contText9 = makeText("8. 위원이 최근 2년 이내에 해당 심의 대상 업체와 관련된 자문, 연구, 용역, 감정 또는 조사를 한 경우", FONT_SIZE_13, false);
						Text contText10 = makeText("9. 위원이 公社 해당 사업에 참여 중인 경우", FONT_SIZE_13, false);
						contPar.add(contText1).add(newLine).add(contText2).add(newLine).add(contText3).add(newLine).add(contText4).add(newLine).add(contText5).add(newLine).add(contText6).add(newLine).add(contText7).add(newLine).add(contText8).add(newLine).add(contText9).add(newLine).add(contText10).add(newLine);
						document.add(contPar);
						document.add(ls); //문서에 라인 추가
						
						Paragraph blankPar = new Paragraph();
						blankPar.add(newLine).add(newLine);
						document.add(blankPar);
						
						Paragraph dayPar = new Paragraph();
						dayPar.setMarginRight(20);
						Text dayText = makeText((String)submitMap.get("registDt"), FONT_SIZE_15, false);
						
						dayPar.add(new Tab());
						dayPar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						dayPar.add(dayText).add(newLine);
						document.add(dayPar);
						
						Paragraph namePar = new Paragraph();
						namePar.setMarginRight(20);
						Text nameText = makeText("평가위원 " + (String)submitMap.get("mfcmmNm"), FONT_SIZE_15, false);
						namePar.add(new Tab());
						namePar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						namePar.add(nameText).add(signImage).add(newLine);
						document.add(namePar);
						
						Paragraph orgPar = new Paragraph();
						orgPar.setMarginLeft(20);
						Text orgText = makeText("경기주택도시공사 귀중", FONT_SIZE_20, true);
						orgPar.add(newLine).add(orgText);
						document.add(orgPar);
					}else if(title.equals("사전접촉·설명, 비리·부정행위 여부 확인서")) {
						document.add(new AreaBreak());
						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_24, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
						titleParagraph.setMarginBottom(MARGIN);						
						document.add(titleParagraph);
						
						Paragraph dlbrtPar = new Paragraph();
						Text dlbrtText = makeText("□ 안건명 : " + (String)submitMap.get("dlbrtNm"), FONT_SIZE_15, false);
						dlbrtPar.add(dlbrtText).add(newLine).add(newLine);
						document.add(dlbrtPar);
						
						Paragraph depPar = new Paragraph();						
						Text depText1 = makeText(" 본인은 상기 공모 건의 평가를 하는데 있어서 참여업체에게 (사전접촉, 사전설명, 비리·부정행위)을 받은 사실이 (있음, 없음)을 확인합니다. 만약 사전접촉, 사전설명 위반 미신고 사실이 확인될 경우 GH 평가위원자격 3년 박탈되며, 비리·부정행위시 GH 평가위원 자격 영구박탈은 물론 일체의 민·형사상 책임질 것을 확약합니다.", FONT_SIZE_12, false);
						depPar.add(depText1).add(newLine);
						document.add(depPar);
						//테이블 생성
						String tableText = "- 사전접촉 : 평가위원 선정 이후 직접 또는 전화, 문자, SNS, E-MAIL 등을 활용하여 입찰자를 인식시키는 행위 일체\n"
											+ "- 사전설명 : 평가위원 선정 이전 직접 또는 전화, 문자, SNS, E-MAIL 등을 활용하여 입찰자를 인식시키는 행위 일체\n"
											+ "- 비리행위 : 법률 등 규정에 어긋난 행위(뇌물·금품수수 등)\n"
											+ "- 부정행위 : 자기회사의 이익이나 상대편 회사의 불이익을 목적으로 행하는 부도덕한 행위(담합, 청탁, 의도적 심사방해 등)\n"
											+ "- 입찰자 : 당해평가에 참여한 공동수급체 참여업체의 소속직원(주관사, 부관사 모두 포함)\n"
											+ "  ※ 지인 등 제3자를 통해 간접적으로 한 경우도 사전접촉·설명, 비리·부정행위로 간주";
					    Table depTable = new Table(UnitValue.createPercentArray(new float[] {100})).useAllAvailableWidth().setFixedLayout();
					    
					    Cell depHeader = makeCell(new Paragraph(makeText(tableText,FONT_SIZE_10,false)), 1, 1, true, true); //셀 생성
					    depHeader.setTextAlignment(TextAlignment.LEFT);
					    depTable.addCell(depHeader);
					    depTable.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
					    depTable.setMarginLeft(5);
						document.add(depTable);
						
						Paragraph contPar = new Paragraph();
						Text contText1 = makeText("□ 확인서(해당사항이 있는 경우만 작성)", FONT_SIZE_12, false);
						contPar.add(contText1).add(newLine);
						document.add(contPar);
						
						Table contTable = new Table(UnitValue.createPercentArray(new float[] {25,75})).useAllAvailableWidth().setFixedLayout();
						//contTable.setTextAlignment(TextAlignment.CENTER);
						
						String contTableText11 = "위반사실";
						String contTableText12 = (String)submitMap.get("cont1");
						Cell contHeader11 = makeCell(new Paragraph(makeText(contTableText11,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
						contHeader11.setBackgroundColor(new DeviceRgb(166, 169, 175));
						contHeader11.setTextAlignment(TextAlignment.CENTER);
						Cell contHeader12 = makeCell(new Paragraph(makeText(contTableText12,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성	
						contHeader12.setTextAlignment(TextAlignment.LEFT);
						contTable.addCell(contHeader11).addCell(contHeader12);
						
						String contTableText21 = "업체명";
						String contTableText22 = (String)submitMap.get("cont2");
						Cell contHeader21 = makeCell(new Paragraph(makeText(contTableText21,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
						contHeader21.setBackgroundColor(new DeviceRgb(166, 169, 175));
						contHeader21.setTextAlignment(TextAlignment.CENTER);
						Cell contHeader22 = makeCell(new Paragraph(makeText(contTableText22,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성	
						contHeader22.setTextAlignment(TextAlignment.LEFT);
						contTable.addCell(contHeader21).addCell(contHeader22);
						
						String contTableText31 = "일 시";
						String contTableText32 = (String)submitMap.get("cont3");
						Cell contHeader31 = makeCell(new Paragraph(makeText(contTableText31,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
						contHeader31.setBackgroundColor(new DeviceRgb(166, 169, 175));
						contHeader31.setTextAlignment(TextAlignment.CENTER);
						Cell contHeader32 = makeCell(new Paragraph(makeText(contTableText32,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성	
						contHeader32.setTextAlignment(TextAlignment.LEFT);
						contTable.addCell(contHeader31).addCell(contHeader32);
						
						String contTableText41 = "내 용";
						String contTableText42 = (String)submitMap.get("cont4");
						Cell contHeader41 = makeCell(new Paragraph(makeText(contTableText41,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
						contHeader41.setBackgroundColor(new DeviceRgb(166, 169, 175));
						contHeader41.setTextAlignment(TextAlignment.CENTER);
						Cell contHeader42 = makeCell(new Paragraph(makeText(contTableText42,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성		
						contHeader42.setTextAlignment(TextAlignment.LEFT);
						contTable.addCell(contHeader41).addCell(contHeader42);
						
						String contTableText51 = "증빙자료";
						String contTableText52 = (String)submitMap.get("cont5");
						Cell contHeader51 = makeCell(new Paragraph(makeText(contTableText51,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
						contHeader51.setBackgroundColor(new DeviceRgb(166, 169, 175));
						contHeader51.setTextAlignment(TextAlignment.CENTER);
						Cell contHeader52 = makeCell(new Paragraph(makeText(contTableText52,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성	
						contHeader52.setTextAlignment(TextAlignment.LEFT);
						contTable.addCell(contHeader51).addCell(contHeader52);
						document.add(contTable);
						
						Paragraph lastPar = new Paragraph();						
						Text lastText = makeText("※ 접촉업체(확인대상) 및 확인내용 등을 가급적 상세하게 작성하여 주시기 바라며,\n"
								+ "   대상자의 실명확인이 어렵거나 증빙서류 불충분 등 사실  확인이 어려울 경우\n"
								+ "   접수가 반려될 수 있음을 알려드립니다.", FONT_SIZE_12, false);
						lastPar.add(lastText).add(newLine).add(newLine);
						document.add(lastPar);
						
						Paragraph dayPar = new Paragraph();
						dayPar.setMarginRight(20);
						Text dayText = makeText((String)submitMap.get("registDt"), FONT_SIZE_12, false);
						
						dayPar.add(new Tab());
						dayPar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						dayPar.add(dayText).add(newLine);
						document.add(dayPar);
						
						Paragraph namePar = new Paragraph();
						namePar.setMarginRight(20);
						Text nameText = makeText("평가위원 " + (String)submitMap.get("mfcmmNm"), FONT_SIZE_14, false);
						namePar.add(new Tab());
						namePar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						namePar.add(nameText).add(signImage).add(newLine);
						document.add(namePar);
						
						Paragraph orgPar = new Paragraph();
						orgPar.setMarginLeft(20);
						Text orgText = makeText("경기주택도시공사 귀중", FONT_SIZE_20, true);
						orgPar.add(newLine).add(orgText);
						document.add(orgPar);
						
					}else if(title.equals("심사과정 촬영, 저장 및 실시간 중계 동의서")) {
						document.add(new AreaBreak());
						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_24, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
						titleParagraph.setMarginBottom(MARGIN);						
						document.add(titleParagraph);
						
						Paragraph contNewLine = new Paragraph();
					    contNewLine.add(newLine);
					    
						Paragraph dlbrtPar = new Paragraph();
						Text dlbrtText = makeText("公社는 공정하고 투명한 평가위원회 운영을 위하여 평가 전 과정을 촬영, 저장 및 실시간 중계하여 개인정보 수집 및 이용하고자 하오니, 자세히 읽어보신 후 동의 여부를 결정하여 주시기 바랍니다.", FONT_SIZE_12, false);
						dlbrtPar.add(dlbrtText).add(newLine);
						document.add(dlbrtPar);
						
						//테이블 생성						
					    Table depTable1 = new Table(UnitValue.createPercentArray(new float[] {100})).useAllAvailableWidth().setFixedLayout();
					    depTable1.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
					    
					    String tableHeaderText1 = "개인정보 수집·이용에 대한 동의";
					    Cell tableHeader1 = makeCell(new Paragraph(makeText(tableHeaderText1,FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
					    tableHeader1.setTextAlignment(TextAlignment.CENTER);
					    tableHeader1.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    depTable1.addCell(tableHeader1);
					    
					    Paragraph contPar11 = new Paragraph();
					    Text ContText11 = makeText("[수집·이용 항목]", FONT_SIZE_11, true);
					    Text ContText12 = makeText(" 성명, 성별, 얼굴, 생년월일, 소속, 연락처 등", FONT_SIZE_11, false);
					    Text ContText121 = makeText("[수집·이용 목적]", FONT_SIZE_11, true);
					    Text ContText122 = makeText(" 용역 및 민간사업자 업체선정을 위한 평가 업무 관련 활동", FONT_SIZE_11, false);
					    Text ContText131 = makeText("[이용 및 보유 기간]", FONT_SIZE_11, true);
					    Text ContText132 = makeText(" 평가일 당일로부터 익년 말일까지", FONT_SIZE_11, false);
					    contPar11.add(ContText11).add(ContText12).add(newLine).add(ContText121).add(ContText122).add(newLine).add(ContText131).add(ContText132);
					    Cell tableCont11 = makeCell(contPar11, 1, 1, true, true); //셀 생성
					    tableCont11.setBorderBottom(Border.NO_BORDER);
					    tableCont11.setTextAlignment(TextAlignment.LEFT);
					    tableCont11.setMarginLeft(10f);
					    depTable1.addCell(tableCont11);
					    
					    Paragraph contPar14 = new Paragraph();
					    
					    String chk = (String)submitMap.get("chk1");
					    Text contText14;
						if(chk.equals("Y")) {
							contText14 = makeText("▣ 동의     □ 동의하지 않음", FONT_SIZE_14, true);
						}else {
							contText14 = makeText("□ 동의     ▣ 동의하지 않음", FONT_SIZE_14, true);
						}
						
					    contPar14.add(contText14);
					    Cell tableCont14 = makeCell(contPar14, 1, 1, true, true); //셀 생성
					    tableCont14.setBorderTop(Border.NO_BORDER);
					    tableCont14.setTextAlignment(TextAlignment.CENTER);
					    depTable1.addCell(tableCont14);
					    
					    
					    Cell lineCell = makeCell(new Paragraph(makeText("",FONT_SIZE_1,true)), 1, 1, true, true);
					    lineCell.setMaxHeight(15f);
					    lineCell.setBorderBottom(Border.NO_BORDER);
					    lineCell.setBorderTop(Border.NO_BORDER);
					    lineCell.setBorderLeft(Border.NO_BORDER);
					    lineCell.setBorderRight(Border.NO_BORDER);
					    lineCell.setMaxHeight(25f);
					    depTable1.addCell(lineCell);
					    
					    document.add(depTable1);
					    
					    //테이블 생성						
					    Table depTable2 = new Table(UnitValue.createPercentArray(new float[] {100})).useAllAvailableWidth().setFixedLayout();
					    depTable2.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
					    
					    String tableHeaderText2 = "개인정보의 제3자 제공에 대한 동의";
					    Cell tableHeader2 = makeCell(new Paragraph(makeText(tableHeaderText2,FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
					    tableHeader2.setTextAlignment(TextAlignment.CENTER);
					    tableHeader2.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    
					    depTable2.addCell(tableHeader2);
					    
					    Paragraph contPar21 = new Paragraph();
					    Text ContText211 = makeText("[[제3자 제공 항목]]", FONT_SIZE_11, true);
					    Text ContText212 = makeText(" 성명, 성별, 얼굴 등", FONT_SIZE_11, false);
					    Text ContText221 = makeText("[제3자 제공 목적]", FONT_SIZE_11, true);
					    Text ContText222 = makeText(" 심사과정 실시간 중계", FONT_SIZE_11, false);
					    Text ContText231 = makeText("[제공받는 자]", FONT_SIZE_11, true);
					    Text ContText232 = makeText(" 일반시민", FONT_SIZE_11, false);
					    Text ContText241 = makeText("[보유기간]", FONT_SIZE_11, true);
					    Text ContText242 = makeText(" 개인정보의 제공목적 달성시", FONT_SIZE_11, false);
					    contPar21.add(ContText211).add(ContText212).add(newLine).add(ContText221).add(ContText222).add(newLine).add(ContText231).add(ContText232).add(newLine).add(ContText241).add(ContText242);
					    Cell tableCont21 = makeCell(contPar21, 1, 1, true, true); //셀 생성
					    tableCont21.setBorderBottom(Border.NO_BORDER);
					    
					    tableCont21.setTextAlignment(TextAlignment.LEFT);
					    tableCont21.setMarginLeft(10f);
					    depTable2.addCell(tableCont21);
					    
					    Paragraph contPar25 = new Paragraph();
					    
					    String chk2 = (String)submitMap.get("chk2");
					    Text contText25;
						if(chk2.equals("Y")) {
							contText25 = makeText("▣ 동의     □ 동의하지 않음", FONT_SIZE_14, true);
						}else {
							contText25 = makeText("□ 동의     ▣ 동의하지 않음", FONT_SIZE_14, true);
						}
						
						contPar25.add(contText25);
					    Cell tableCont25 = makeCell(contPar14, 1, 1, true, true); //셀 생성
					    tableCont25.setBorderTop(Border.NO_BORDER);
					    tableCont25.setTextAlignment(TextAlignment.CENTER);
					    depTable2.addCell(tableCont25);
					    
					    Cell lineCell2 = makeCell(new Paragraph(makeText("",FONT_SIZE_1,true)), 1, 1, true, true);
					    lineCell2.setMaxHeight(15f);
					    lineCell2.setBorderBottom(Border.NO_BORDER);
					    lineCell2.setBorderTop(Border.NO_BORDER);
					    lineCell2.setBorderLeft(Border.NO_BORDER);
					    lineCell2.setBorderRight(Border.NO_BORDER);
					    lineCell2.setMaxHeight(25f);
					    depTable2.addCell(lineCell2);
					    
					    document.add(depTable2);
					    
					    //테이블 생성						
					    Table depTable3 = new Table(UnitValue.createPercentArray(new float[] {100})).useAllAvailableWidth().setFixedLayout();
					    depTable3.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
					    
					    String tableHeaderText3 = "개인정보 수집·이용에 대한 동의";
					    Cell tableHeader3 = makeCell(new Paragraph(makeText(tableHeaderText3,FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
					    tableHeader3.setTextAlignment(TextAlignment.CENTER);
					    tableHeader3.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    
					    depTable3.addCell(tableHeader3);
					    
					    Paragraph contPar3 = new Paragraph();
					    Text ContText31 = makeText("본인은 공사가 촬영한 방송영상물 일체에 대하여 촬영자에게 본인의 초상권을 사용하는 것에 대하여 동의합니다.", FONT_SIZE_11, false);
					    Text ContText32 = makeText("또한, 본인은 저작자 및 저작권 이용허락자로서 公社에 실시간 중계를 통해 공개될 수 있는 저작물에 관한 저작재산권을 이용 허락함에 동의합니다.", FONT_SIZE_11, false);
					    Text ContText33 = makeText("본인은 미디어 실시간 중계에 수반되어 공개될 수 있는 그 밖의 일체의 사항에 관한 권리의 침해를 주장하거나 이의를 제기하지 않을 것에 동의합니다.", FONT_SIZE_11, false);
					    contPar3.add(ContText31).add(newLine).add(ContText32).add(newLine).add(ContText33);
					    Cell tableCont31 = makeCell(contPar3, 1, 1, true, true); //셀 생성
					    tableCont31.setBorderBottom(Border.NO_BORDER);
					    tableCont31.setTextAlignment(TextAlignment.LEFT);
					    tableCont31.setMarginLeft(10f);
					    depTable3.addCell(tableCont31);
					    
					    Paragraph contPar35 = new Paragraph();					    
					    String chk3 = (String)submitMap.get("chk3");
					    Text contText35;
						if(chk3.equals("Y")) {
							contText35 = makeText("▣ 동의     □ 동의하지 않음", FONT_SIZE_14, true);
						}else {
							contText35 = makeText("□ 동의     ▣ 동의하지 않음", FONT_SIZE_14, true);
						}
						
						contPar35.add(contText35);
					    Cell tableCont35 = makeCell(contPar35, 1, 1, true, true); //셀 생성
					    tableCont35.setBorderTop(Border.NO_BORDER);
					    tableCont35.setTextAlignment(TextAlignment.CENTER);
					    depTable3.addCell(tableCont35);
					    
					    document.add(depTable3);
					    
					    Paragraph footPar = new Paragraph();
						Text footText = makeText("※ 귀하는 상기 개인 정보 수집·이용 및 제3자 제공, 초상권 및 저작물 활용에 대한 동의를 거부할 권리가 있습니다. 그러나 동의를 거부할 경우 심사 평가 참영에 제한이 있을 수 있습니다.", FONT_SIZE_12, true);
						footPar.add(footText).add(newLine);
						document.add(footPar);
						
						Paragraph lastPar = new Paragraph();						
						Text lastText = makeText("본인은 상기 동의서 내용을 명확히 이해하였으며 이에 동의합니다.", FONT_SIZE_12, false);
						lastPar.add(lastText).add(newLine);
						document.add(lastPar);
						
						Paragraph dayPar = new Paragraph();
						dayPar.setMarginRight(20);
						Text dayText = makeText((String)submitMap.get("registDt"), FONT_SIZE_14, false);
						
						dayPar.add(new Tab());
						dayPar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						dayPar.add(dayText).add(newLine);
						document.add(dayPar);
						
						Paragraph namePar = new Paragraph();
						namePar.setMarginRight(20);
						Text nameText = makeText("평가위원 " + (String)submitMap.get("mfcmmNm"), FONT_SIZE_12, false);
						namePar.add(new Tab());
						namePar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						namePar.add(nameText).add(signImage).add(newLine);
						document.add(namePar);
						
						Paragraph orgPar = new Paragraph();
						orgPar.setMarginLeft(20);
						Text orgText = makeText("경기주택도시공사 귀중", FONT_SIZE_20, true);
						orgPar.add(newLine).add(orgText);
						document.add(orgPar);
						
						
					}else if(title.equals("직무윤리 사전진단서")) {
						//document.add(new AreaBreak());
						PdfPage page = pdf.addNewPage();
						// 사각형 좌표 지정: (x, y, width, height)
						float x = 50f;
						float y = 50f;
						float width = 500f;
						float height = 750f;
						Rectangle rect = new Rectangle(x, y, width, height);
						
						// 사각형 경로 그리기 및 선 적용(stroke)
						PdfCanvas pdfCanvas = new PdfCanvas(page);
						pdfCanvas.setLineWidth(1f);
						pdfCanvas.setStrokeColor(Color.BLACK);
						pdfCanvas.rectangle(rect);
						pdfCanvas.stroke();
						
						
						Canvas canvas = new Canvas(pdfCanvas, pdf, rect);
						
						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_24, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
						titleParagraph.setMarginBottom(MARGIN);
						canvas.add(titleParagraph);
						
						Paragraph namePar = new Paragraph();
						namePar.setMarginRight(20);
						Text contText7 = makeText("성명 : " + (String)submitMap.get("mfcmmNm"), FONT_SIZE_15, false);
						namePar.add(new Tab());
						namePar.addTabStops(new TabStop(1000, TabAlignment.RIGHT));
						namePar.add(contText7).add(signImage).add(newLine);
						canvas.add(namePar);
						
						//테이블 생성						
					    Table depTable1 = new Table(UnitValue.createPercentArray(new float[] {10, 70, 10, 10})).useAllAvailableWidth().setFixedLayout();
					    depTable1.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
					    
					    String tableHeaderText1 = "연번";
					    Cell tableHeader1 = makeCell(new Paragraph(makeText(tableHeaderText1,FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
					    tableHeader1.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    
					    String tableHeaderText2 = "진 단 내 용";
					    Cell tableHeader2 = makeCell(new Paragraph(makeText(tableHeaderText2,FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
					    tableHeader2.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    
					    String tableHeaderText3 = "체크사항";
					    Cell tableHeader3 = makeCell(new Paragraph(makeText(tableHeaderText3,FONT_SIZE_12,true)), 1, 2, true, true); //셀 생성
					    tableHeader3.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    
					    depTable1.addCell(tableHeader1).addCell(tableHeader2).addCell(tableHeader3);
					    
					    String tableContText11 = "1";
					    Cell tableCont11 = makeCell(new Paragraph(makeText(tableContText11,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    String tableContText12 = "위원회의 기능과 직접 관련된 업체를 경영하거나 근무하고 있다.";
					    Cell tableCont12 = makeCell(new Paragraph(makeText(tableContText12,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    tableCont12.setTextAlignment(TextAlignment.LEFT);
					    tableCont12.setMarginLeft(10);
					    
					    String tableContText13 = "";
					    String chk1 = (String)submitMap.get("chk1");
					    
					    if(chk1.equals("Y")) {
					    	tableContText13 = "예\n(∨)";
					    }else {
					    	tableContText13 = "예\n(  )";
					    }
					    Cell tableCont13 = makeCell(new Paragraph(makeText(tableContText13,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    
					    String tableContText14 = "";
					    if(chk1.equals("Y")) {
					    	tableContText14 = "아니오\n(  )";
					    }else {
					    	tableContText14 = "아니오\n(∨)";
					    }
					    Cell tableCont14 = makeCell(new Paragraph(makeText(tableContText14,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont11).addCell(tableCont12).addCell(tableCont13).addCell(tableCont14);
						
					    String tableContText21 = "2";
					    Cell tableCont21 = makeCell(new Paragraph(makeText(tableContText21,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    String tableContText22 = "위워회의 심의·의결 대상사업 관련지역에 부동산 또는 주식을 보유하고 있다.";
					    Cell tableCont22 = makeCell(new Paragraph(makeText(tableContText22,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    tableCont22.setTextAlignment(TextAlignment.LEFT);
					    tableCont22.setMarginLeft(10);
					    String tableContText23 = "";
					    String chk2 = (String)submitMap.get("chk2");
					    
					    if(chk2.equals("Y")) {
					    	tableContText23 = "예\n(∨)";
					    }else {
					    	tableContText23 = "예\n(  )";
					    }
					    Cell tableCont23 = makeCell(new Paragraph(makeText(tableContText23,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    
					    String tableContText24 = "";
					    if(chk2.equals("Y")) {
					    	tableContText24 = "아니오\n(  )";
					    }else {
					    	tableContText24 = "아니오\n(∨)";
					    }
					    Cell tableCont24 = makeCell(new Paragraph(makeText(tableContText24,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont21).addCell(tableCont22).addCell(tableCont23).addCell(tableCont24);
					    
					    String tableContText31 = "3";
					    Cell tableCont31 = makeCell(new Paragraph(makeText(tableContText31,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    String tableContText32 = "위원회의 직접적인 심의·의결 대상이 되는 인가ㆍ허가ㆍ면허ㆍ특허 등의 당사자이다.";
					    Cell tableCont32 = makeCell(new Paragraph(makeText(tableContText32,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    tableCont32.setTextAlignment(TextAlignment.LEFT);
					    tableCont32.setMarginLeft(10);
					    String tableContText33 = "";
					    String chk3 = (String)submitMap.get("chk3");
					    
					    if(chk3.equals("Y")) {
					    	tableContText33 = "예\n(∨)";
					    }else {
					    	tableContText33 = "예\n(  )";
					    }
					    Cell tableCont33 = makeCell(new Paragraph(makeText(tableContText33,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    
					    String tableContText34 = "";
					    if(chk3.equals("Y")) {
					    	tableContText34 = "아니오\n(  )";
					    }else {
					    	tableContText34 = "아니오\n(∨)";
					    }
					    Cell tableCont34 = makeCell(new Paragraph(makeText(tableContText34,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont31).addCell(tableCont32).addCell(tableCont33).addCell(tableCont34);
					    
					    String tableContText41 = "4";
					    Cell tableCont41 = makeCell(new Paragraph(makeText(tableContText41,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    String tableContText42 = "위원회 기능과 직접 관련된 공사ㆍ용역ㆍ계약 또는 연구ㆍ논문 등을 진행 중이거나 진행 할 예정이다.";
					    Cell tableCont42 = makeCell(new Paragraph(makeText(tableContText42,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    tableCont42.setTextAlignment(TextAlignment.LEFT);
					    tableCont42.setMarginLeft(10);
					    String tableContText43 = "";
					    String chk4 = (String)submitMap.get("chk4");
					    
					    if(chk4.equals("Y")) {
					    	tableContText43 = "예\n(∨)";
					    }else {
					    	tableContText43 = "예\n(  )";
					    }
					    Cell tableCont43 = makeCell(new Paragraph(makeText(tableContText43,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    
					    String tableContText44 = "";
					    if(chk4.equals("Y")) {
					    	tableContText44 = "아니오\n(  )";
					    }else {
					    	tableContText44 = "아니오\n(∨)";
					    }
					    Cell tableCont44 = makeCell(new Paragraph(makeText(tableContText44,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont41).addCell(tableCont42).addCell(tableCont43).addCell(tableCont44);
					    
					    String tableContText51 = "5";
					    Cell tableCont51 = makeCell(new Paragraph(makeText(tableContText51,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    String tableContText52 = "위원회 직무와 관련된 사안으로 수사를 받고 있거나 재판ㆍ소송등을 진행 중이다.";
					    Cell tableCont52 = makeCell(new Paragraph(makeText(tableContText52,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    tableCont52.setTextAlignment(TextAlignment.LEFT);
					    tableCont52.setMarginLeft(10);
					    String tableContText53 = "";
					    String chk5 = (String)submitMap.get("chk5");
					    
					    if(chk5.equals("Y")) {
					    	tableContText53 = "예\n(∨)";
					    }else {
					    	tableContText53 = "예\n(  )";
					    }
					    Cell tableCont53 = makeCell(new Paragraph(makeText(tableContText53,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    
					    String tableContText54 = "";
					    if(chk5.equals("Y")) {
					    	tableContText54 = "아니오\n(  )";
					    }else {
					    	tableContText54 = "아니오\n(∨)";
					    }
					    Cell tableCont54 = makeCell(new Paragraph(makeText(tableContText54,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont51).addCell(tableCont52).addCell(tableCont53).addCell(tableCont54);
					    
					    String tableContText61 = "6";
					    Cell tableCont61 = makeCell(new Paragraph(makeText(tableContText61,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    String tableContText62 = "위원회 직무의 공정한 수행에 지장을 줄 우려가 있는 기관ㆍ단체ㆍ타 위원회에서 현재 활동 중이다.";
					    Cell tableCont62 = makeCell(new Paragraph(makeText(tableContText62,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    tableCont62.setTextAlignment(TextAlignment.LEFT);
					    tableCont62.setMarginLeft(10);
					    String tableContText63 = "";
					    String chk6 = (String)submitMap.get("chk6");
					    
					    if(chk6.equals("Y")) {
					    	tableContText63 = "예\n(∨)";
					    }else {
					    	tableContText63 = "예\n(  )";
					    }
					    Cell tableCont63 = makeCell(new Paragraph(makeText(tableContText63,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    
					    String tableContText64 = "";
					    if(chk6.equals("Y")) {
					    	tableContText64 = "아니오\n(  )";
					    }else {
					    	tableContText64 = "아니오\n(∨)";
					    }
					    Cell tableCont64 = makeCell(new Paragraph(makeText(tableContText64,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont61).addCell(tableCont62).addCell(tableCont63).addCell(tableCont64);
					    
					    String tableContText71 = "7";
					    Cell tableCont71 = makeCell(new Paragraph(makeText(tableContText71,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    String tableContText72 = "위원회 기능 관련 정보나 심의·의결 결과가 본인의 권리ㆍ의무 관계 변동, 재산상의 이익 등을 발생시킬 가능성이 크다.";
					    Cell tableCont72 = makeCell(new Paragraph(makeText(tableContText72,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    tableCont72.setTextAlignment(TextAlignment.LEFT);
					    tableCont72.setMarginLeft(10);
					    String tableContText73 = "";
					    String chk7 = (String)submitMap.get("chk7");
					    
					    if(chk6.equals("Y")) {
					    	tableContText73 = "예\n(∨)";
					    }else {
					    	tableContText73 = "예\n(  )";
					    }
					    Cell tableCont73 = makeCell(new Paragraph(makeText(tableContText73,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    
					    String tableContText74 = "";
					    if(chk6.equals("Y")) {
					    	tableContText74 = "아니오\n(  )";
					    }else {
					    	tableContText74 = "아니오\n(∨)";
					    }
					    Cell tableCont74 = makeCell(new Paragraph(makeText(tableContText74,FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont71).addCell(tableCont72).addCell(tableCont73).addCell(tableCont74);
					    
					    canvas.add(depTable1);
					    
					    Paragraph lastPar = new Paragraph();
					    lastPar.setMarginLeft(10);
						Text lastText = makeText("※ ‘예’라고 답변 하였음에도 불구하고 위원회 직무를 공정하게 수행할 수 있는 타당한 사유가 있을 경우 기재하여 주시기 바랍니다.", FONT_SIZE_12, false);
						lastPar.add(lastText).add(newLine).add(newLine).add(newLine);
						canvas.add(lastPar);
						
						Paragraph dayPar = new Paragraph();
						dayPar.setMarginRight(20);
						Text contText6 = makeText((String)submitMap.get("registDt"), FONT_SIZE_12, false);
						
						dayPar.setTextAlignment(TextAlignment.CENTER);
						dayPar.add(contText6).add(newLine).add(newLine).add(newLine);
						canvas.add(dayPar);
						
						Paragraph orgPar = new Paragraph();
						orgPar.setMarginLeft(20);
						Text contText8 = makeText("경기주택도시공사 사장 귀하", FONT_SIZE_20, true);
						orgPar.add(newLine).add(contText8);
						canvas.add(orgPar);
						
						canvas.close();
						
					}
				}
			}
			
			// 문서 닫기
			document.close();
			pdf.close();
		}catch (NullPointerException e) {
			System.out.println(e.getMessage());
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}finally {
			//fileOutputStream.close();
		}
		return lsFncCn;
	}
	
	public String createBillDocumentFile(List<EgovMap> documentList) throws IOException{
		String lsFncCn = "영수증 생성 ";
		
		try {
			String fileRealPath = EgovProperties.getProperty("Globals.FILE_REAL_PATH");
			String jobId = "PD";
			String fileKnd = "pdf";
			String fileId = cmmUtil.getFileId();
			
			String filePath = cmmUtil.filePathBlackList(fileRealPath + File.separator + jobId + File.separator + fileId);		//GlobalsPath로 수정해야함.
			
			File file = new File(filePath);
			
			if (file == null || file.getParentFile() == null) {
				throw new IOException("file.getParentFile() is null");
			}
			
			// 디렉토리 생성
			if (!file.getParentFile().exists()) {
				if(file.getParentFile().mkdirs()){
				}
			}
			baseFont = PdfFontFactory.createFont(
				    getClass().getResource("/font/NanumGothic.ttf").getPath(),
				    PdfEncodings.IDENTITY_H,
				    true   // 폰트 내장(Embed)
				);
			
			// step 1
			PdfWriter writer = new PdfWriter(filePath);
			PdfDocument pdf = new PdfDocument(writer);
			Document document = new Document(pdf,PageSize.A4);
			document.setMargins(30f,50f,50f,50f); //상화좌우 마진
			
			SolidLine line = new SolidLine(1f);
		    LineSeparator ls = new LineSeparator(line);
			
			float documentLRMargin = document.getLeftMargin()+document.getRightMargin(); //좌우 마진
			float documentTBMargin = document.getTopMargin()+document.getBottomMargin(); //상하 마진
			float fullWidth = pdf.getDefaultPageSize().getWidth()-documentLRMargin;		 //문서 width
			float fullHeight = pdf.getDefaultPageSize().getHeight()-documentTBMargin;	 //문서 height
			Text newLine = new Text("\n");
			
			if(documentList.size() > 0) {
				for(int i=0; i < documentList.size(); i++) {
					EgovMap submitMap = (EgovMap)documentList.get(i);
					String title = (String)submitMap.get("receiptTit");
					String sign = (String)submitMap.get("sign");
					byte[] signBytes = Base64.decode(sign);
					ImageData signData = ImageDataFactory.create(signBytes);
		            Image signImage = new Image(signData);
		            signImage.scaleToFit(60, 60); // 가로 100, 세로 100으로 크기 조절

					if(title.equals("영수증")) {
						if(i > 0) {
							document.add(new AreaBreak());
						}
						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_24, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
						titleParagraph.setMarginBottom(MARGIN);						
						document.add(titleParagraph);
						
						Paragraph dlbrtPar = new Paragraph();
						Text dlbrtText = makeText("◈ 건명 : " + (String)submitMap.get("dlbrtNm"), FONT_SIZE_14, false);
						dlbrtPar.add(dlbrtText).add(newLine).add(newLine);
						document.add(dlbrtPar);
						
						Paragraph amtPar = new Paragraph();
						Text amtText = makeText("◈ 금액", FONT_SIZE_14, false);						
						Text unitText = makeText(" <단위: 원>", FONT_SIZE_14, false);
						amtPar.setTextAlignment(TextAlignment.JUSTIFIED);
						amtPar.add(amtText).add(unitText);
						document.add(amtPar);
						
						//테이블 생성						
					    Table depTable1 = new Table(UnitValue.createPercentArray(new float[] {20, 20, 20, 20, 20})).useAllAvailableWidth().setFixedLayout();
					    depTable1.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
					    
					    String tableHeaderText1 = "심의비용(A)";
					    Cell tableHeader1 = makeCell(new Paragraph(makeText(tableHeaderText1,FONT_SIZE_13,true)), 2, 1, true, true); //셀 생성
					    tableHeader1.setBackgroundColor(new DeviceRgb(166, 169, 175));					    
					    String tableHeaderText2 = "공제내역(B)";
					    Cell tableHeader2 = makeCell(new Paragraph(makeText(tableHeaderText2,FONT_SIZE_13,true)), 1, 3, true, true); //셀 생성
					    tableHeader2.setBackgroundColor(new DeviceRgb(166, 169, 175));					    
					    String tableHeaderText3 = "실수령액\n(A-B)";
					    Cell tableHeader3 = makeCell(new Paragraph(makeText(tableHeaderText3,FONT_SIZE_13,true)), 2, 1, true, true); //셀 생성
					    tableHeader3.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    depTable1.addCell(tableHeader1).addCell(tableHeader2).addCell(tableHeader3);					    
					    String tableHeaderText4 = "계";
					    Cell tableHeader4 = makeCell(new Paragraph(makeText(tableHeaderText4,FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    tableHeader4.setBackgroundColor(new DeviceRgb(166, 169, 175));					    
					    String tableHeaderText5 = "소득세";
					    Cell tableHeader5 = makeCell(new Paragraph(makeText(tableHeaderText5,FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    tableHeader5.setBackgroundColor(new DeviceRgb(166, 169, 175));					    
					    String tableHeaderText6 = "주민세";
					    Cell tableHeader6 = makeCell(new Paragraph(makeText(tableHeaderText6,FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    tableHeader6.setBackgroundColor(new DeviceRgb(166, 169, 175));					    
					    depTable1.addCell(tableHeader4).addCell(tableHeader5).addCell(tableHeader6);
					    
					    Cell tableCont1 = makeCell(new Paragraph(makeText((String)submitMap.get("pymntAllwnc"),FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    Cell tableCont2 = makeCell(new Paragraph(makeText((String)submitMap.get("sumTax"),FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    Cell tableCont3 = makeCell(new Paragraph(makeText((String)submitMap.get("incomeTax"),FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    Cell tableCont4 = makeCell(new Paragraph(makeText((String)submitMap.get("residentTax"),FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    Cell tableCont5 = makeCell(new Paragraph(makeText((String)submitMap.get("netSalary"),FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    depTable1.addCell(tableCont1).addCell(tableCont2).addCell(tableCont3).addCell(tableCont4).addCell(tableCont5);
						
						document.add(depTable1);
						
						Paragraph dayPar = new Paragraph();
						dayPar.setMarginRight(20);
						Text dayText = makeText("◈ 영수일자 : " + (String)submitMap.get("registDt"), FONT_SIZE_14, false);						
						dayPar.add(newLine).add(dayText);
						document.add(dayPar);
						
						//테이블 생성						
					    Table depTable2 = new Table(UnitValue.createPercentArray(new float[] {10, 15, 40, 20, 15})).useAllAvailableWidth().setFixedLayout();
					    depTable2.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
					    
					    Cell table2Header1 = makeCell(new Paragraph(makeText("성 명",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    table2Header1.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    Cell table2Header2 = makeCell(new Paragraph(makeText("수령액",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    table2Header2.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    Cell table2Header3 = makeCell(new Paragraph(makeText("주 소\n(은행명, 계좌번호)",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    table2Header3.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    Cell table2Header4 = makeCell(new Paragraph(makeText("주민등록번호",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    table2Header4.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    Cell table2Header5 = makeCell(new Paragraph(makeText("서명란",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
					    table2Header5.setBackgroundColor(new DeviceRgb(166, 169, 175));
					    depTable2.addCell(table2Header1).addCell(table2Header2).addCell(table2Header3).addCell(table2Header4).addCell(table2Header5);
					    
					    Cell table2Cont1 = makeCell(new Paragraph(makeText((String)submitMap.get("mfcmmNm"),FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    Cell table2Cont2 = makeCell(new Paragraph(makeText((String)submitMap.get("pymntAllwnc"),FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    String addr = (String)submitMap.get("adres") + " " + (String)submitMap.get("adresDetail");
					    String bankNm = (String)submitMap.get("bankNm");
					    String bankAccount = (String)submitMap.get("banckAccount");
					    Cell table2Cont3 = makeCell(new Paragraph(makeText("주소 : " + addr + "\n"+"은행명 : " + bankNm + "\n"+"계좌번호 : " + bankAccount,FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
					    table2Cont3.setTextAlignment(TextAlignment.LEFT);
					    String juminNo = (String)submitMap.get("juminNo");
					    String juminNoEn = "";
						if (juminNo != null && juminNo.contains("-")) {
						    String[] parts = juminNo.split("-", 2);
						    String front = parts[0];
						    String back = parts[1];
						    
						    String encryptedBack = cmmUtil.getDecyptString((back));
						    
						    juminNoEn = front + "-" + encryptedBack;						    
						}
						Cell table2Cont4 = makeCell(new Paragraph(makeText(juminNoEn,FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
						
						depTable2.addCell(table2Cont1).addCell(table2Cont2).addCell(table2Cont3).addCell(table2Cont4).addCell(signImage);
						document.add(depTable2);
					}else if(title.equals("심의수당 지급을 위한 개인정보 수집·이용 동의서")) {
						//document.add(new AreaBreak());
						PdfPage page = pdf.addNewPage();
						// 사각형 좌표 지정: (x, y, width, height)
						float x = 50f;
						float y = 50f;
						float width = 500f;
						float height = 750f;
						Rectangle rect = new Rectangle(x, y, width, height);
						
						// 사각형 경로 그리기 및 선 적용(stroke)
						PdfCanvas pdfCanvas = new PdfCanvas(page);
						pdfCanvas.setLineWidth(1f);
						pdfCanvas.setStrokeColor(Color.BLACK);
						pdfCanvas.rectangle(rect);
						pdfCanvas.stroke();
						
						
						Canvas canvas = new Canvas(pdfCanvas, pdf, rect);
						
						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_15, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
						//titleParagraph.setBackgroundColor(new DeviceRgb(166, 169, 175));
						canvas.add(titleParagraph);
						canvas.add(ls); //문서에 라인 추가
						
						Text contText1 = makeText("경기주택도시공사는 심의수당 지급을 위하여 아래와 같이 개인정보를 수집·이용하고자 합니다. 내용을 자세히 읽으신 후 동의 여부를 결정하여 주세요.", FONT_SIZE_14, false);	
						Paragraph contPar1 = new Paragraph();
						contPar1.setMarginLeft(5f);
						contPar1.setMarginRight(5f);
						contPar1.add(newLine).add(contText1).add(newLine);
						canvas.add(contPar1);
						
						Paragraph titleTablePar = new Paragraph(makeText("▶ 개인정보 수집·이용 내역",FONT_SIZE_12, true)); //가운데 정렬
						titleTablePar.setMarginLeft(5f);
						titleTablePar.setMarginRight(5f);
						canvas.add(titleTablePar);
						
						Table Table1 = new Table(UnitValue.createPercentArray(new float[] {30, 40, 30})).useAllAvailableWidth().setFixedLayout();
						Table1.setMarginLeft(5f);
						Table1.setMarginRight(5f);
						
						Cell table1Header1 = makeCell(new Paragraph(makeText("항목",FONT_SIZE_14,true)), 1, 1, true, true); //셀 생성
						Cell table1Header2 = makeCell(new Paragraph(makeText("수집·이용 목적",FONT_SIZE_14,true)), 1, 1, true, true); //셀 생성
						Cell table1Header3 = makeCell(new Paragraph(makeText("보유·이용기간",FONT_SIZE_14,true)), 1, 1, true, true); //셀 생성
						Table1.addCell(table1Header1).addCell(table1Header2).addCell(table1Header3);
						
						Cell table1Cont1 = makeCell(new Paragraph(makeText("은행 계좌번호",FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
						table1Cont1.setHeight(70f);
						Cell table1Cont2 = makeCell(new Paragraph(makeText("심의수당 지급 진행",FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
						table1Cont2.setHeight(70f);
						Cell table1Cont3 = makeCell(new Paragraph(makeText("2년",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
						table1Cont3.setHeight(70f);
						Table1.addCell(table1Cont1).addCell(table1Cont2).addCell(table1Cont3);
						canvas.add(Table1);
						
						Text contText2 = makeText("※ 위의 개인정보 수집·이용에 대한 동의를 거부할 수 있습니다.\n그러나, 동의를 거부할 경우 원활한 심의수당 지급절차 진행을 할 수 없어 지급에 제한을 받을 수 있습니다.", FONT_SIZE_13, false);	
						Paragraph contPar2 = new Paragraph();
						contPar2.setMarginLeft(5f);
						contPar2.setMarginRight(5f);
						contPar2.add(newLine).add(contText2);
						canvas.add(contPar2);
						
						Text contText3 = makeText("☞ 위와 같이 개인정보를 수집·이용하는데 동의하십니까?", FONT_SIZE_12, true);	
						Paragraph contPar3 = new Paragraph();
						contPar3.setMarginLeft(15f);
						contPar3.add(contText3);
						canvas.add(contPar3);
						
						String chk = (String)submitMap.get("chk1");
						if(chk.equals("Y")) {
							Text contText4 = makeText("동의(∨) 미동의(  )", FONT_SIZE_11, true);	
							Paragraph contPar4 = new Paragraph();
							contPar4.setMarginRight(25f);
							contPar4.add(contText4).add(newLine).add(newLine);
							contPar4.setTextAlignment(TextAlignment.RIGHT);
							canvas.add(contPar4);
						}else {
							Text contText4 = makeText("동의(  ) 미동의(∨)", FONT_SIZE_11, true);	
							Paragraph contPar4 = new Paragraph();
							contPar4.setMarginRight(25f);
							contPar4.add(contText4).add(newLine).add(newLine);
							contPar4.setTextAlignment(TextAlignment.RIGHT);
							canvas.add(contPar4);
						}
						
						Table Table2 = new Table(UnitValue.createPercentArray(new float[] {100})).useAllAvailableWidth().setFixedLayout();
						Table2.setMarginLeft(5);
						Table2.setMarginRight(5);
						Cell table2Cont1 = makeCell(new Paragraph(makeText("<기타 고지 사항>",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
						table2Cont1.setPadding(10f);
						table2Cont1.setTextAlignment(TextAlignment.LEFT);
						table2Cont1.setBorderBottom(Border.NO_BORDER);
						Table2.addCell(table2Cont1);
						
						Cell table2Cont2 = makeCell(new Paragraph(makeText("  개인정보 보호법 제15조 제1항 제3호에 따라 정보주체의 동의 없이 개인정보를 수집·이용합니다.",FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
						table2Cont2.setPadding(15f);
						table2Cont2.setTextAlignment(TextAlignment.LEFT);
						table2Cont2.setBorderTop(Border.NO_BORDER);
						table2Cont2.setBorderBottom(Border.NO_BORDER);
						
						Table2.addCell(table2Cont2);
						
						Cell table2Cont3 = makeCell(new Paragraph(makeText("  ",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
						table2Cont3.setPadding(8f);
						table2Cont3.setBorderTop(Border.NO_BORDER);
						table2Cont3.setBorderBottom(Border.NO_BORDER);
						Table2.addCell(table2Cont3);
						
						Cell table2Cont4 = new Cell();
						Table innerTable = new Table(UnitValue.createPercentArray(new float[] {40, 30, 30})).useAllAvailableWidth().setFixedLayout();
						innerTable.setMarginLeft(5f);
						innerTable.setMarginRight(5f);
						
						Cell innerHeader1 = makeCell(new Paragraph(makeText("개인정보 처리사유",FONT_SIZE_14,false)), 1, 1, true, true); //셀 생성
						Cell innerHeader2 = makeCell(new Paragraph(makeText("개인정보항목",FONT_SIZE_14,false)), 1, 1, true, true); //셀 생성
						Cell innerHeader3 = makeCell(new Paragraph(makeText("수집근거",FONT_SIZE_14,false)), 1, 1, true, true); //셀 생성
						innerTable.addCell(innerHeader1).addCell(innerHeader2).addCell(innerHeader3);
						
						Cell innerCont1 = makeCell(new Paragraph(makeText("거주자의 기타소득 원천징수영수증(발행자 보관용) 작성 목적",FONT_SIZE_13,false).setTextAlignment(TextAlignment.LEFT)), 1, 1, true, true); //셀 생성
						Cell innerCont2 = makeCell(new Paragraph(makeText("성명, 주민등록번호, 주소",FONT_SIZE_13,true)), 1, 1, true, true); //셀 생성
						Cell innerCont3 = makeCell(new Paragraph(makeText("소득세법 제145조",FONT_SIZE_13,false)), 1, 1, true, true); //셀 생성
						innerTable.addCell(innerCont1).addCell(innerCont2).addCell(innerCont3);						
						table2Cont4.add(innerTable);
						table2Cont4.setBorderTop(Border.NO_BORDER);
						Table2.addCell(table2Cont4);
						canvas.add(Table2);
						
						Text contText5 = makeText((String)submitMap.get("registDt"), FONT_SIZE_14, false);
						Paragraph contPar5 = new Paragraph();
						contPar5.setTextAlignment(TextAlignment.CENTER);
						contPar5.add(newLine).add(contText5);
						canvas.add(contPar5);
						
						Paragraph namePar = new Paragraph();
						namePar.setMarginRight(20);
						Text nameText = makeText("본인 성명 " + (String)submitMap.get("mfcmmNm"), FONT_SIZE_12, true);
						namePar.setTextAlignment(TextAlignment.RIGHT);
						namePar.add(newLine).add(nameText).add(signImage).add(newLine);
						canvas.add(namePar);
						
						Paragraph orgPar = new Paragraph();
						orgPar.setMarginLeft(20);
						Text contText8 = makeText("경기주택도시공사 귀중", FONT_SIZE_20, true);
						orgPar.setTextAlignment(TextAlignment.CENTER);
						orgPar.add(newLine).add(contText8);
						canvas.add(orgPar);
						canvas.close();
					}
				}
			}
			
			// 문서 닫기
			document.close();
			pdf.close();
		}catch (NullPointerException e) {
			System.out.println(e.getMessage());
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}finally {
			//fileOutputStream.close();
		}
		return lsFncCn;
	}
	
	public String createEvlDocumentFile(EgovMap param) throws IOException{
		String lsFncCn = "영수증 생성 ";
		
		try {
			String fileRealPath = EgovProperties.getProperty("Globals.FILE_REAL_PATH");
			String jobId = "PD";
			String fileKnd = "pdf";
			String fileId = cmmUtil.getFileId();
			
			String filePath = cmmUtil.filePathBlackList(fileRealPath + File.separator + jobId + File.separator + fileId);		//GlobalsPath로 수정해야함.
			
			File file = new File(filePath);
			
			if (file == null || file.getParentFile() == null) {
				throw new IOException("file.getParentFile() is null");
			}
			
			// 디렉토리 생성
			if (!file.getParentFile().exists()) {
				if(file.getParentFile().mkdirs()){
				}
			}
			baseFont = PdfFontFactory.createFont(
				    getClass().getResource("/font/NanumGothic.ttf").getPath(),
				    PdfEncodings.IDENTITY_H,
				    true   // 폰트 내장(Embed)
				);
			
			// step 1
			PdfWriter writer = new PdfWriter(filePath);
			PdfDocument pdf = new PdfDocument(writer);
			Document document = new Document(pdf,PageSize.A4);
			document.setMargins(30f,50f,50f,50f); //상화좌우 마진
			
			SolidLine line = new SolidLine(1f);
		    LineSeparator ls = new LineSeparator(line);
			
			float documentLRMargin = document.getLeftMargin()+document.getRightMargin(); //좌우 마진
			float documentTBMargin = document.getTopMargin()+document.getBottomMargin(); //상하 마진
			float fullWidth = pdf.getDefaultPageSize().getWidth()-documentLRMargin;		 //문서 width
			float fullHeight = pdf.getDefaultPageSize().getHeight()-documentTBMargin;	 //문서 height
			Text newLine = new Text("\n");
			
			//List<EgovMap> trgetEntrprsList = evlService.getTrgetEntrprsList(param);
			//List<EgovMap> realmScoreList = evlService.getRealmScoreList(param);			
			
//			if(realmScoreList.size() > 0) {
//				for(int i=0; i < realmScoreList.size(); i++) {
//					EgovMap realmScoreMap = (EgovMap)realmScoreList.get(i);
//					String title = (String)realmScoreMap.get("evlTit");
//					String sign = (String)realmScoreMap.get("sign");
//					byte[] signBytes = Base64.decode(sign);
//					ImageData signData = ImageDataFactory.create(signBytes);
//		            Image signImage = new Image(signData);
//		            signImage.scaleToFit(60, 60); // 가로 100, 세로 100으로 크기 조절
//					
//					List<EgovMap> scoreList = evlService.getEntrstScoreList(realmScoreMap);
//					if(scoreList != null && scoreList.size() > 0) {
//						if(i > 0) {
//							document.add(new AreaBreak());
//						}
//						Paragraph titleParagraph = new Paragraph(makeText(title,FONT_SIZE_24, true)).setTextAlignment(TextAlignment.CENTER); //가운데 정렬
//						titleParagraph.setMarginBottom(MARGIN);						
//						document.add(titleParagraph);
//						
//						Paragraph dlbrtPar = new Paragraph();
//						Text dlbrtText1 = makeText("◈안 건 명 : " + (String)realmScoreMap.get("dlbrtNm"), FONT_SIZE_14, false);
//						Text dlbrtText2 = makeText("◈평가분야 : " + (String)realmScoreMap.get("realmㅜm"), FONT_SIZE_14, false);
//						dlbrtPar.add(dlbrtText1).add(dlbrtText2);
//						document.add(dlbrtPar);
//						
//						if(title.equals("평가위원별 평가점수표")) {
//							Table contTable;
//						    Cell tableHeader1;
//						    int evlRealmScoreSum = 0;
//						    int evlBlockScoreSum = 0;
//						    int baseCnt = 0;
//						    
//						    String evlRealmElementDetail = (String)scoreList.get(0).get("evlRealmElementDetail");
//						    
//						    if(evlRealmElementDetail.equals("")) {
//						    	contTable = new Table(UnitValue.createPercentArray(new float[] {10, 60, 10, 20})).useAllAvailableWidth().setFixedLayout();
//						    	tableHeader1 = makeCell(new Paragraph(makeText("평가요소",FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
//						    	contTable.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
//						    }else {
//						    	contTable = new Table(UnitValue.createPercentArray(new float[] {10, 15, 45, 10, 20})).useAllAvailableWidth().setFixedLayout();
//						    	tableHeader1 = makeCell(new Paragraph(makeText("평가요소",FONT_SIZE_12,true)), 1, 2, true, true); //셀 생성
//						    	contTable.setWidth(pdf.getDefaultPageSize().getWidth()-(documentLRMargin));
//						    }
//						    tableHeader1.setBackgroundColor(new DeviceRgb(166, 169, 175));					   
//						    Cell tableHeader2 = makeCell(new Paragraph(makeText("평가내용",FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
//						    tableHeader2.setBackgroundColor(new DeviceRgb(166, 169, 175));					    
//						    Cell tableHeader3 = makeCell(new Paragraph(makeText("배점",FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
//						    tableHeader3.setBackgroundColor(new DeviceRgb(166, 169, 175));							    					    
//						    Cell tableHeader4 = makeCell(new Paragraph(makeText("점수",FONT_SIZE_12,true)), 1, 1, true, true); //셀 생성
//						    tableHeader4.setBackgroundColor(new DeviceRgb(166, 169, 175));					    
//						    contTable.addCell(tableHeader1).addCell(tableHeader2).addCell(tableHeader3).addCell(tableHeader4);
//						    
//							for(int j=0; j < scoreList.size(); j++) {
//								EgovMap scoreMap = (EgovMap)scoreList.get(j);	
//								
//								//평가요소 결합 가져오기
//								int elementCnt =  (int)scoreMap.get("elementCnt");
//							    if(baseCnt != elementCnt) {
//								    Cell tableCont1 = makeCell(new Paragraph(makeText((String)scoreMap.get("evlRealmElement"),FONT_SIZE_12,false)), elementCnt, 1, true, true); //셀 생성
//								    contTable.addCell(tableCont1);
//							    }
//							    
//							    Cell tableCont2 = makeCell(new Paragraph(makeText((String)scoreMap.get("evlRealmElementDetail"),FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
//							    Cell tableCont3 = makeCell(new Paragraph(makeText((String)scoreMap.get("evlRealmContent"),FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
//							    Cell tableCont4 = makeCell(new Paragraph(makeText((String)scoreMap.get("evlRealmScore"),FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
//							    Cell tableCont5 = makeCell(new Paragraph(makeText((String)scoreMap.get("evlBlockScore"),FONT_SIZE_12,false)), 1, 1, true, true); //셀 생성
//							    
//							    if(evlRealmElementDetail.equals("")) {
//							    	contTable.addCell(tableCont2).addCell(tableCont4).addCell(tableCont5);
//							    }else {
//							    	contTable.addCell(tableCont2).addCell(tableCont3).addCell(tableCont4).addCell(tableCont5);
//							    }
//							    
//							    baseCnt = elementCnt;
//							    evlRealmScoreSum = evlRealmScoreSum + (int)scoreMap.get("evlRealmScore");
//							    evlBlockScoreSum = evlBlockScoreSum + (int)scoreMap.get("evlBlockScore");
//							    if(j+1 == scoreList.size()) {
//							    	Cell tableSum1;
//							    	 if(evlRealmElementDetail.equals("")) {
//									    tableSum1 = makeCell(new Paragraph(makeText((String)scoreMap.get("소 계"),FONT_SIZE_12,true)), 1, 2, true, true); //셀 생성
//									    tableSum1.setBackgroundColor(new DeviceRgb(166, 169, 175));									    
//								    }else {
//								    	tableSum1 = makeCell(new Paragraph(makeText((String)scoreMap.get("소 계"),FONT_SIZE_12,true)), 1, 3, true, true); //셀 생성
//								    	tableSum1.setBackgroundColor(new DeviceRgb(166, 169, 175));		
//								    }
//							    	Cell tableSum2 = makeCell(new Paragraph(makeText(evlRealmScoreSum+"",FONT_SIZE_12,true)), 1, 2, true, true); //셀 생성
//							    	tableSum2.setBackgroundColor(new DeviceRgb(166, 169, 175));		
//							    	Cell tableSum3 = makeCell(new Paragraph(makeText(evlBlockScoreSum+"",FONT_SIZE_12,true)), 1, 2, true, true); //셀 생성
//							    	tableSum3.setBackgroundColor(new DeviceRgb(166, 169, 175));	
//							    	contTable.addCell(tableSum1).addCell(tableSum2).addCell(tableSum3);
//							    	 
//							    	Cell tableFoot;
//							    	if(evlRealmElementDetail.equals("")) {
//							    		tableFoot = makeCell(new Paragraph(makeText(" \n<채점방법>\n·배점한도 내에서 평가내용에 따라 평가 후 소계 산정\n ",FONT_SIZE_12,false)), 1, 4, true, true); //셀 생성
//							    	}else {
//							    		tableFoot = makeCell(new Paragraph(makeText(" \n<채점방법>\n·배점한도 내에서 평가내용에 따라 평가 후 소계 산정\n ",FONT_SIZE_12,false)), 1, 5, true, true); //셀 생성
//							    	}
//							    	contTable.addCell(tableFoot);
//							    }
//							}
//							    
//							document.add(contTable);
//								
//							Paragraph namePar = new Paragraph();
//							namePar.setMarginRight(20);
//							Text nameText = makeText("평가위원 : " + (String)realmScoreMap.get("mfcmmNm"), FONT_SIZE_14, true);
//							namePar.setTextAlignment(TextAlignment.RIGHT);
//							namePar.add(newLine).add(newLine).add(newLine).add(nameText).add(signImage).add(newLine);
//							document.add(namePar);
//							
//							Paragraph orgPar = new Paragraph();
//							orgPar.setMarginLeft(20);
//							Text contText8 = makeText("경기주택도시공사 귀중", FONT_SIZE_20, true);
//							orgPar.setTextAlignment(TextAlignment.CENTER);
//							orgPar.add(newLine).add(contText8);
//							document.add(namePar);
//						}
//					}
//				}
//			}
			
			// 문서 닫기
			document.close();
			pdf.close();
		}catch (NullPointerException e) {
			System.out.println(e.getMessage());
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}finally {
			//fileOutputStream.close();
		}
		return lsFncCn;
	}
}
