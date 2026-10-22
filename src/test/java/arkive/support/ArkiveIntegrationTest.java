package arkive.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import egovframework.example.EgovBootApplication;

/**
 * Arkive 통합 테스트 공통 설정.
 * 모든 통합 테스트가 같은 Spring 컨텍스트를 공유해 HSQLDB 테스트 서버(9002)를 한 번만 띄운다.
 * 테스트 클래스마다 @SpringBootTest를 직접 붙이면 설정 차이로 컨텍스트가 갈라져
 * 같은 포트를 두 번 열려고 하므로 이 애노테이션을 사용한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = EgovBootApplication.class)
@AutoConfigureMockMvc
@TestPropertySource("classpath:/application-test.properties")
public @interface ArkiveIntegrationTest {
}
