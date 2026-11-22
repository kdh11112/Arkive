package arkive.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import egovframework.example.EgovBootApplication;

/**
 * Arkive 통합 테스트 공통 설정.
 * 모든 통합 테스트가 같은 Spring 컨텍스트를 공유해 격리 PostgreSQL 테스트 DB를 한 번만 띄운다.
 * 테스트 클래스마다 @SpringBootTest를 직접 붙이면 설정 차이로 컨텍스트가 갈라져
 * 같은 DB를 두 번 준비하려고 하므로 이 애노테이션을 사용한다.
 * DB 기동·스키마·시드 적용은 PostgresTestDatabaseInitializer가 컨텍스트 시작 전에 수행한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = EgovBootApplication.class)
@AutoConfigureMockMvc
@TestPropertySource("classpath:/application-test.properties")
@ContextConfiguration(initializers = PostgresTestDatabaseInitializer.class)
public @interface ArkiveIntegrationTest {
}
