package egovframework.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
//(exclude = SecurityAutoConfiguration.class) 시큐리티 기본 로그인 화면 끄기
@SpringBootApplication(exclude = SecurityAutoConfiguration.class) 
public class EgovBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(EgovBootApplication.class, args);
	}

}
