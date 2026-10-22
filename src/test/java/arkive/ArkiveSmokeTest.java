package arkive;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import arkive.support.ArkiveIntegrationTest;

/**
 * 스모크 테스트. 컨텍스트·MyBatis 배선이 살아있는지만 본다.
 * DB는 target/test-db 격리 경로, 포트는 9002를 쓴다(application-test.properties).
 */
@ArkiveIntegrationTest
class ArkiveSmokeTest {

	@Autowired(required = false)
	private SqlSessionFactory sqlSessionFactory;

	@Test
	void contextLoads() {
		assertThat(sqlSessionFactory).isNotNull();
	}
}
