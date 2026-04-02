package egovframework.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
//(exclude = SecurityAutoConfiguration.class) 시큐리티 기본 로그인 화면 끄기
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
//이걸 추가하지 않으면 arkive 아래에 있는 모든 파일을 실행되지않음
//EgovBootApplication 가 egovframewor k패키지에 있어서 arkive는 못읽음 패키지가 다름
@ComponentScan(basePackages = {"egovframework", "arkive"})
public class EgovBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(EgovBootApplication.class, args);
	}

}
