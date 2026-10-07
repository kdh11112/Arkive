package arkive.admin.category.service;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 카테고리 서비스. 순환참조 방지와 하위 포함 삭제 차단을 보장한다.
 */
public interface CategoryService {

	List<EgovMap> selectCategoryAll() throws Exception;

	List<EgovMap> selectCategoryChildren(String upCatId) throws Exception;

	EgovMap selectCategoryDetail(String catId) throws Exception;

	void insertCategory(EgovMap egovMap) throws Exception;

	void updateCategory(EgovMap egovMap) throws Exception;

	void deleteCategory(String catId) throws Exception;
}
