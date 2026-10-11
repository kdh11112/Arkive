package arkive;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import egovframework.example.EgovBootApplication;

/**
 * 스모크 테스트. 컨텍스트·MyBatis 배선이 살아있는지만 본다.
 * DB는 ./target/test-db 격리 경로를 쓴다(application-test가 아니라
 * application.properties override 방식. 운영 설정 파일을 직접 참조하므로
 * 키 추가 시 테스트에도 자동 반영된다).
 * 주의: 9001 파일 서버를 띄우므로 앱 실행 중에는 실패한다.
 */
// 테스트 클래스가 egovframework 밖에 있어 @SpringBootConfiguration 탐색이 안 되므로
// 진입 클래스를 직접 지정한다.
// DB 경로만 application-test.properties로 덮어쓴다. 동명 application.properties를
// test에 두면 main 파일 전체가 가려지므로(Boot도 중복 파일을 합치지 않는다)
// 키가 다른 별도 파일 + @TestPropertySource 조합을 쓴다.
@SpringBootTest(classes = EgovBootApplication.class)
@TestPropertySource("classpath:/application-test.properties")
class ArkiveSmokeTest {

	@Autowired(required = false)
	private SqlSessionFactory sqlSessionFactory;

	@Test
	void contextLoads() {
		assertThat(sqlSessionFactory).isNotNull();
	}
}
