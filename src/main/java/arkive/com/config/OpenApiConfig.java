package arkive.com.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Swagger(SpringDoc) API 문서 설정.
 * application.properties의 springdoc.packages-to-scan이 egovframework,arkive를 보도록 둔다.
 * 문서는 /swagger-ui.html, 스펙 JSON은 /v3/api-docs 에서 본다.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
		title = "Arkive API",
		description = "게시판·파일·메뉴·공통코드 JSON API. 타 프로젝트 이식용 레퍼런스다.",
		version = "1.0.0"))
public class OpenApiConfig {
}
