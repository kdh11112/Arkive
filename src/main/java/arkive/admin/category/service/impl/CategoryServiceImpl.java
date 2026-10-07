package arkive.admin.category.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;

import arkive.admin.category.service.CategoryService;
import jakarta.annotation.Resource;

/**
 * 카테고리 서비스 구현.
 * 정책: 순환참조(자기 자신·자기 자손을 상위로) 금지, 하위 포함 삭제 금지.
 */
@Service("categoryService")
public class CategoryServiceImpl extends EgovAbstractServiceImpl implements CategoryService {

	@Resource(name = "categoryMapper")
	private CategoryMapper categoryMapper;

	@Override
	public List<EgovMap> selectCategoryAll() throws Exception {
		return categoryMapper.selectCategoryAll();
	}

	@Override
	public List<EgovMap> selectCategoryChildren(String upCatId) throws Exception {
		return categoryMapper.selectCategoryChildren(upCatId);
	}

	@Override
	public EgovMap selectCategoryDetail(String catId) throws Exception {
		return categoryMapper.selectCategoryDetail(catId);
	}

	@Override
	public void insertCategory(EgovMap egovMap) throws Exception {
		categoryMapper.insertCategory(egovMap);
	}

	@Override
	public void updateCategory(EgovMap egovMap) throws Exception {
		String catId = String.valueOf(egovMap.get("catId"));
		Object up = egovMap.get("upCatId");
		String upCatId = up == null ? null : String.valueOf(up);
		if (upCatId != null && !upCatId.isEmpty()) {
			if (upCatId.equals(catId)) {
				throw new IllegalStateException("자기 자신을 상위로 지정할 수 없습니다.");
			}
			if (collectDescendants(catId).contains(upCatId)) {
				throw new IllegalStateException("자기 하위를 상위로 지정할 수 없습니다(순환참조).");
			}
		}
		categoryMapper.updateCategory(egovMap);
	}

	@Override
	public void deleteCategory(String catId) throws Exception {
		if (categoryMapper.countCategoryChildren(catId) > 0) {
			throw new IllegalStateException("하위 카테고리가 있어 삭제할 수 없습니다. 하위부터 삭제하세요.");
		}
		categoryMapper.deleteCategory(catId);
	}

	private Set<String> collectDescendants(String catId) throws Exception {
		Set<String> result = new HashSet<>();
		collectChildren(catId, result);
		return result;
	}

	private void collectChildren(String parentId, Set<String> result) throws Exception {
		List<EgovMap> children = categoryMapper.selectCategoryChildren(parentId);
		for (EgovMap child : children) {
			String id = String.valueOf(child.get("catId"));
			if (result.add(id)) {
				collectChildren(id, result);
			}
		}
	}
}
