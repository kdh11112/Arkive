package arkive.admin.lecture.service.impl;

import java.util.List;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.egovframe.rte.psl.dataaccess.util.EgovMap;

/**
 * 강의형 콘텐츠 MyBatis 매퍼. SQL은 Lecture_SQL.xml에 있다.
 */
@EgovMapper("lectureMapper")
public interface LectureMapper {

	List<EgovMap> selectLectureList(EgovMap egovMap) throws Exception;

	int selectLectureListTotCnt(EgovMap egovMap) throws Exception;

	List<EgovMap> selectAvailableLectureList(EgovMap egovMap) throws Exception;

	EgovMap selectLectureDetail(String lectureId) throws Exception;

	EgovMap selectLectureForView(EgovMap egovMap) throws Exception;

	void insertLecture(EgovMap egovMap) throws Exception;

	int updateLecture(EgovMap egovMap) throws Exception;

	int deleteLecture(String lectureId) throws Exception;

	int openLecture(String lectureId) throws Exception;

	int closeLecture(String lectureId) throws Exception;

	EgovMap selectProgress(EgovMap egovMap) throws Exception;

	void insertProgress(EgovMap egovMap) throws Exception;

	int updateProgress(EgovMap egovMap) throws Exception;

	int updateProgressCompleted(EgovMap egovMap) throws Exception;

	List<EgovMap> selectCompletionList(EgovMap egovMap) throws Exception;

	int selectCompletionListTotCnt(EgovMap egovMap) throws Exception;
}
