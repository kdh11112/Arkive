package arkive.admin.category.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 카테고리 MyBatis 매퍼. SQL은 Category_SQL.xml에 있다.
 */
@EgovMapper("categoryMapper")
public interface CategoryMapper {

	List<EgovMap> selectCategoryAll() throws Exception;

	List<EgovMap> selectCategoryChildren(String upCatId) throws Exception;

	int countCategoryChildren(String catId) throws Exception;

	EgovMap selectCategoryDetail(String catId) throws Exception;

	void insertCategory(EgovMap egovMap) throws Exception;

	void updateCategory(EgovMap egovMap) throws Exception;

	void deleteCategory(String catId) throws Exception;
}
