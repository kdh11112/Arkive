package arkive.admin.category.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import arkive.admin.category.service.CategoryService;
import arkive.admin.comm.web.CommUtil;
import egovframework.com.cmm.util.EgovDoubleSubmitHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 카테고리관리. jstree형(tree)과 컬럼 드릴다운형(drill)이 같은 CATEGORY 테이블을 공유한다.
 * jstree 라이브러리 없이 네이티브 트리로 구현해 이식 의존성을 없앴다.
 * 참고: BLMS categorySetList/saveCategorySet.json + process_SQL.xml#13~45, menuList.jsp jstree.
 */
@Tag(name = "카테고리", description = "카테고리 트리·드릴다운 조회, 등록·수정·이동·삭제")
@Controller
@RequestMapping("/category")
public class CategoryController {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "categoryService")
	private CategoryService categoryService;

	private CommUtil cmmUtil = new CommUtil();

	@Operation(summary = "카테고리 트리 화면 (jstree형)")
	@RequestMapping(value = "/tree", method = { RequestMethod.GET, RequestMethod.POST })
	public String tree(HttpServletRequest request, ModelMap model) throws Exception {
		model.put("treeHtml", buildTreeHtml(categoryService.selectCategoryAll()));
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_CATEGORY"));
		return "category/categoryTree";
	}

	@Operation(summary = "카테고리 드릴다운 화면 (컬럼형)")
	@RequestMapping(value = "/drill", method = { RequestMethod.GET, RequestMethod.POST })
	public String drill(HttpServletRequest request, ModelMap model) throws Exception {
		String parentId = cmmUtil.convertHtml(request, "parentId");
		List<EgovMap> path = new ArrayList<>();
		if (parentId != null && !parentId.isEmpty()) {
			String cursor = parentId;
			while (cursor != null && !cursor.isEmpty()) {
				EgovMap node = categoryService.selectCategoryDetail(cursor);
				if (node == null) {
					break;
				}
				path.add(0, node);
				Object up = node.get("upCatId");
				cursor = up == null ? null : String.valueOf(up);
			}
		}
		model.put("parentId", parentId == null ? "" : parentId);
		model.put("path", path);
		model.put("children", categoryService.selectCategoryChildren(parentId));
		model.put("doubleSubmitToken", EgovDoubleSubmitHelper.setToken(request.getSession(), "BOARD_CATEGORY"));
		return "category/categoryDrill";
	}

	@Operation(summary = "카테고리 전체 JSON (jstree id/parent/text 규격)")
	@RequestMapping(value = "/tree.json", method = { RequestMethod.GET, RequestMethod.POST })
	@ResponseBody
	public List<Map<String, Object>> treeJson() {
		List<Map<String, Object>> result = new ArrayList<>();
		try {
			for (EgovMap r : categoryService.selectCategoryAll()) {
				Map<String, Object> node = new LinkedHashMap<>();
				node.put("id", String.valueOf(r.get("catId")));
				Object up = r.get("upCatId");
				node.put("parent", up == null || String.valueOf(up).isEmpty() ? "#" : String.valueOf(up));
				node.put("text", String.valueOf(r.get("catNm")));
				result.add(node);
			}
		} catch (Exception e) {
			logger.error("treeJson failed", e);
		}
		return result;
	}

	private int parseOrdr(String ordr) {
		try {
			return Integer.parseInt(ordr);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Operation(summary = "카테고리 저장 (등록·수정 겸용)")
	@RequestMapping(value = "/saveCategory.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> saveCategory(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean valid;
			try {
				valid = EgovDoubleSubmitHelper.checkAndSaveToken("BOARD_CATEGORY");
			} catch (RuntimeException e) {
				valid = false;
			}
			if (!valid) {
				throw new IllegalStateException("중복 제출이 감지되었습니다. 화면을 새로고침하세요.");
			}
			String catId = cmmUtil.convertHtml(request, "catId");
			String catNm = cmmUtil.convertHtml(request, "catNm");
			String upCatId = cmmUtil.convertHtml(request, "upCatId");
			if (catNm == null || catNm.trim().isEmpty()) {
				throw new IllegalArgumentException("카테고리 이름을 입력하세요.");
			}
			if (upCatId != null && upCatId.isEmpty()) {
				upCatId = null;
			}
			EgovMap param = new EgovMap();
			param.put("catNm", catNm.trim());
			param.put("upCatId", upCatId);
			param.put("ordr", parseOrdr(cmmUtil.convertHtml(request, "ordr")));
			if (catId == null || catId.isEmpty()) {
				param.put("catId", "CT" + CommUtil.getFileId());
				param.put("register", "SYSTEM");
				categoryService.insertCategory(param);
				result.put("catId", param.get("catId"));
			} else {
				param.put("catId", catId);
				param.put("updusr", "SYSTEM");
				categoryService.updateCategory(param);
				result.put("catId", catId);
			}
			result.put("success", true);
		} catch (Exception e) {
			logger.error("saveCategory failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	@Operation(summary = "카테고리 삭제 (하위 있으면 차단)")
	@RequestMapping(value = "/deleteCategory.json", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteCategory(HttpServletRequest request) {
		Map<String, Object> result = new HashMap<>();
		try {
			categoryService.deleteCategory(cmmUtil.convertHtml(request, "catId"));
			result.put("success", true);
		} catch (Exception e) {
			logger.error("deleteCategory failed", e);
			result.put("success", false);
			result.put("message", e.getMessage());
		}
		return result;
	}

	private String buildTreeHtml(List<EgovMap> all) {
		Map<String, List<EgovMap>> byParent = new LinkedHashMap<>();
		for (EgovMap r : all) {
			Object up = r.get("upCatId");
			String key = up == null || String.valueOf(up).isEmpty() ? "" : String.valueOf(up);
			byParent.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
		}
		StringBuilder sb = new StringBuilder();
		appendTreeNodes(sb, byParent, "");
		return sb.toString();
	}

	private void appendTreeNodes(StringBuilder sb, Map<String, List<EgovMap>> byParent, String parentId) {
		List<EgovMap> children = byParent.get(parentId);
		if (children == null || children.isEmpty()) {
			return;
		}
		sb.append("<ul>");
		for (EgovMap c : children) {
			String id = escape(String.valueOf(c.get("catId")));
			String nm = escape(String.valueOf(c.get("catNm")));
			Object up = c.get("upCatId");
			String upId = up == null ? "" : escape(String.valueOf(up));
			String ordr = escape(String.valueOf(c.get("ordr")));
			sb.append("<li><details><summary>").append(nm)
				.append(" <button type=\"button\" class=\"krds-btn xsmall secondary\" data-id=\"").append(id)
				.append("\" data-up=\"").append(upId).append("\" data-nm=\"").append(nm)
				.append("\" data-ord=\"").append(ordr)
				.append("\" onclick=\"fnEditCat(this)\">수정</button>")
				.append(" <button type=\"button\" class=\"krds-btn xsmall secondary\" data-id=\"").append(id)
				.append("\" onclick=\"fnAddChild(this)\">하위추가</button>")
				.append(" <button type=\"button\" class=\"krds-btn xsmall danger\" data-id=\"").append(id)
				.append("\" onclick=\"fnDeleteCat(this)\">삭제</button></summary>");
			appendTreeNodes(sb, byParent, String.valueOf(c.get("catId")));
			sb.append("</details></li>");
		}
		sb.append("</ul>");
	}

	private String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
