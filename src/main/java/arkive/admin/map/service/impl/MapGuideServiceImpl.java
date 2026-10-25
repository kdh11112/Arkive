package arkive.admin.map.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import arkive.admin.comm.web.CommUtil;
import arkive.admin.map.service.MapGuideService;
import jakarta.annotation.Resource;

/**
 * 약도관리 서비스 구현.
 *
 * <p>주소와 위·경도는 사람이 입력한다. 좌표 변환(지오코딩)은 외부 API 계약이
 * 필요한 별도 기능이므로 포함하지 않는다. 위·경도는 둘 다 있거나 둘 다 없어야 한다.</p>
 */
@Service("mapGuideService")
public class MapGuideServiceImpl extends EgovAbstractServiceImpl implements MapGuideService {

	private static final int MAX_TITLE = 200;
	private static final int MAX_ADDRESS = 300;
	private static final int MAX_DESCRIPTION = 2000;
	private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
	private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
	private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
	private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

	@Resource(name = "mapGuideMapper")
	private MapGuideMapper mapGuideMapper;

	@Override
	public List<EgovMap> selectMapGuideList(EgovMap egovMap) throws Exception {
		return mapGuideMapper.selectMapGuideList(egovMap);
	}

	@Override
	public int selectMapGuideListTotCnt(EgovMap egovMap) throws Exception {
		return mapGuideMapper.selectMapGuideListTotCnt(egovMap);
	}

	@Override
	public EgovMap selectMapGuideDetail(String mapId) throws Exception {
		return isBlank(mapId) ? null : mapGuideMapper.selectMapGuideDetail(mapId.trim());
	}

	@Override
	@Transactional
	public void insertMapGuide(EgovMap egovMap) throws Exception {
		validate(egovMap);
		egovMap.put("mapId", "MP" + CommUtil.getFileId());
		mapGuideMapper.insertMapGuide(egovMap);
	}

	@Override
	@Transactional
	public void updateMapGuide(EgovMap egovMap) throws Exception {
		String mapId = trim(egovMap.get("mapId"));
		if (selectMapGuideDetail(mapId) == null) {
			throw new IllegalArgumentException("약도가 없습니다.");
		}
		validate(egovMap);
		egovMap.put("mapId", mapId);
		if (mapGuideMapper.updateMapGuide(egovMap) == 0) {
			throw new IllegalStateException("약도가 없습니다. 새로고침 후 다시 시도하세요.");
		}
	}

	@Override
	@Transactional
	public void deleteMapGuide(String mapId) throws Exception {
		if (selectMapGuideDetail(mapId) == null) {
			throw new IllegalArgumentException("약도가 없습니다.");
		}
		mapGuideMapper.deleteMapGuide(mapId.trim());
	}

	private void validate(EgovMap egovMap) {
		String title = trim(egovMap.get("title"));
		String address = trim(egovMap.get("address"));
		String description = trim(egovMap.get("description"));
		String useYn = trim(egovMap.get("useYn"));
		String latitudeText = trim(egovMap.get("latitude"));
		String longitudeText = trim(egovMap.get("longitude"));

		if (title.isEmpty() || title.length() > MAX_TITLE) {
			throw new IllegalArgumentException("약도명은 1~200자로 입력하세요.");
		}
		if (address.length() > MAX_ADDRESS) {
			throw new IllegalArgumentException("주소는 300자 이내로 입력하세요.");
		}
		if (description.length() > MAX_DESCRIPTION) {
			throw new IllegalArgumentException("설명은 2000자 이내로 입력하세요.");
		}
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			throw new IllegalArgumentException("사용여부가 올바르지 않습니다.");
		}
		if (latitudeText.isEmpty() != longitudeText.isEmpty()) {
			throw new IllegalArgumentException("위도와 경도는 함께 입력하세요.");
		}
		BigDecimal latitude = null;
		BigDecimal longitude = null;
		if (!latitudeText.isEmpty()) {
			latitude = parseCoordinate(latitudeText, "위도");
			longitude = parseCoordinate(longitudeText, "경도");
			if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
				throw new IllegalArgumentException("위도는 -90~90 사이로 입력하세요.");
			}
			if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
				throw new IllegalArgumentException("경도는 -180~180 사이로 입력하세요.");
			}
		}
		egovMap.put("title", title);
		egovMap.put("address", address);
		egovMap.put("description", description);
		egovMap.put("useYn", useYn);
		egovMap.put("latitude", latitude);
		egovMap.put("longitude", longitude);
	}

	private BigDecimal parseCoordinate(String value, String label) {
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(label + "는 숫자로 입력하세요.");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String trim(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
