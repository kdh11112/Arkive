package egovframework.example.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import arkive.admin.auth.service.MenuPermission;
import arkive.admin.auth.service.PermissionService;
import arkive.admin.audit.web.AuditInterceptor;
import arkive.admin.auth.web.AuthInterceptor;
import arkive.admin.comm.web.MaskingInterceptor;
import arkive.admin.terms.web.TermsInterceptor;
import arkive.admin.auth.web.LoginController;
import arkive.admin.auth.web.LoginSessionRegistry;
import arkive.admin.auth.web.MenuPermissionInterceptor;
import arkive.admin.board.service.BoardService;
import arkive.admin.system.service.SystemService;
import egovframework.example.pagination.EgovKrdsPaginationRenderer;
import jakarta.annotation.Resource;
import egovframework.example.pagination.EgovPaginationDialect;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionListener;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

@Configuration
@Import({
        EgovConfigAspect.class,
        EgovConfigCommon.class,
        EgovConfigDatasource.class,
        EgovConfigIdGeneration.class,
        EgovConfigMapper.class,
        EgovConfigProperties.class,
        EgovConfigTransaction.class,
        EgovConfigValidation.class
})
public class EgovConfigWeb implements WebMvcConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Resource
    private PermissionService permissionService;

    @Resource
    private BoardService boardService;

    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(this.applicationContext);
        // templateResolver.setPrefix("classpath:/templates/thymeleaf/"); //운영
        templateResolver.setPrefix("file:./src/main/resources/templates/thymeleaf/"); // 개발
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine(EgovKrdsPaginationRenderer egovKrdsPaginationRenderer) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        // add custom tag
        templateEngine.addDialect(new EgovPaginationDialect(egovKrdsPaginationRenderer));
        templateEngine.addDialect(new LayoutDialect());
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver thymeleafViewResolver(EgovKrdsPaginationRenderer egovKrdsPaginationRenderer) {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setCharacterEncoding("UTF-8");
        viewResolver.setTemplateEngine(templateEngine(egovKrdsPaginationRenderer));
        return viewResolver;
    }
    
    // jsonView 방식은 @ResponseBody로 전부 전환되어 BeanNameViewResolver·MappingJackson2JsonView를 제거했다.
    // 구 방식이 필요하면 EgovConfigWeb 변경 이력을 참고한다.

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/img/**").addResourceLocations("classpath:/static/img/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");

        // favicon.ico 처리를 위한 빈 핸들러 (404 오류 방지)
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // .well-known 경로도 처리 (Chrome DevTools 자동 요청)
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/.well-known/");
    }

    @Bean
    public SessionLocaleResolver localeResolver() {
        return new SessionLocaleResolver();
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("language");
        return interceptor;
    }

    public static class MenuInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {

            try {

                SystemService systemService = WebApplicationContextUtils
                        .getWebApplicationContext(request.getServletContext()).getBean(SystemService.class);

                if (systemService != null) {

                    EgovMap egovMap = new EgovMap();

                    List<EgovMap> menuList = systemService.selectMenuList(egovMap);

                    if (menuList != null) {

                        List<EgovMap> normalizedList = new ArrayList<>();

                        for (EgovMap map : menuList) {

                            EgovMap newMap = new EgovMap();

                            Object menuId = map.get("menuId");
                            Object menuNm = map.get("menuNm");
                            Object menuCours = map.get("menuCours");
                            Object grad = map.get("grad");
                            Object parent = map.get("parent");
                            Object ordr = map.get("ordr");

                            if (menuId != null) {
                                menuId = menuId.toString();
                            }

                            if (menuNm != null) {
                                menuNm = menuNm.toString();
                            }

                            if (menuCours != null) {
                                menuCours = menuCours.toString();
                            }

                            if (parent != null) {
                                parent = parent.toString();
                            }

                            if (grad instanceof Number) {
                                grad = ((Number) grad).intValue();
                            }

                            if (ordr instanceof Number) {
                                ordr = ((Number) ordr).intValue();
                            }

                            newMap.put("menuId", menuId);
                            newMap.put("menuNm", menuNm);
                            newMap.put("menuCours", menuCours);
                            newMap.put("grad", grad);
                            newMap.put("parent", parent);
                            newMap.put("ordr", ordr);

                            normalizedList.add(newMap);
                        }

                        // 로그인 사용자는 역할의 조회 권한이 있는 메뉴만 노출한다.
                        // 관리자는 전체 메뉴를 본다. 비로그인은 메뉴를 노출하지 않는다.
                        HttpSession session = request.getSession(false);
                        if (session == null) {
                            normalizedList.clear();
                        } else {
                            Object loginUser = session.getAttribute(LoginController.SESSION_KEY);
                            if (!(loginUser instanceof EgovMap)) {
                                normalizedList.clear();
                            } else if (session.getAttribute(LoginController.ADMIN_KEY) == null) {
                                PermissionService permissionService = WebApplicationContextUtils
                                        .getWebApplicationContext(request.getServletContext())
                                        .getBean(PermissionService.class);
                                String userId = String.valueOf(((EgovMap) loginUser).get("userId"));
                                Map<String, MenuPermission> permissions = permissionService
                                        .selectUserMenuPermissions(userId);
                                Set<String> visible = new HashSet<>();
                                for (EgovMap menu : normalizedList) {
                                    String menuId = String.valueOf(menu.get("menuId"));
                                    MenuPermission permission = permissions.get(menuId);
                                    if (permission != null && permission.read()) {
                                        visible.add(menuId);
                                    }
                                }
                                // 하위 메뉴 권한만 있어도 상위 트리가 보이도록 조상을 포함한다.
                                List<String> queue = new ArrayList<>(visible);
                                for (int i = 0; i < queue.size(); i++) {
                                    String id = queue.get(i);
                                    for (EgovMap menu : normalizedList) {
                                        if (id.equals(String.valueOf(menu.get("menuId")))) {
                                            String parent = menu.get("parent") == null ? null
                                                    : String.valueOf(menu.get("parent"));
                                            if (parent != null && !"#".equals(parent) && visible.add(parent)) {
                                                queue.add(parent);
                                            }
                                            break;
                                        }
                                    }
                                }
                                normalizedList.removeIf(menu -> !visible.contains(String.valueOf(menu.get("menuId"))));
                            }
                        }

                        request.setAttribute(
                                "menuList",
                                normalizedList);
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        // 메뉴는 화면에만 필요하고 정적 파일·API에는 불필요하다.
        // 기존에는 전체 경로에서 매 요청 DB 조회를 때려 페이지당 수십 건 쿼리가 나갔다(렉 원인).
        registry.addInterceptor(new MenuInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/img/**", "/vendor/**",
                        "/favicon.ico", "/error", "/ws/**",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/**");
        // 로그인 검사는 AuthInterceptor 빈으로 둔다(공개 경로는 클래스 안에서 제외).
        registry.addInterceptor(authInterceptor());
        // 로그인 후 필수 약관 미동의 사용자는 동의 화면으로 보낸다.
        registry.addInterceptor(new TermsInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/img/**", "/vendor/**", "/fonts/**",
                        "/favicon.ico", "/error", "/ws/**",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/**");
        // 메뉴 조회·쓰기 권한은 로그인 검사 뒤에 서버에서 강제한다.
        registry.addInterceptor(menuPermissionInterceptor(permissionService, boardService))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/img/**", "/vendor/**",
                        "/favicon.ico", "/error", "/ws/**",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/**");
        // 권한 통과 후 메뉴·개인정보 열람·다운로드 감사 이벤트를 발행한다.
        registry.addInterceptor(auditInterceptor(applicationContext))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/img/**", "/vendor/**",
                        "/favicon.ico", "/error", "/ws/**",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/**");
        // 목록 화면의 개인정보는 표시 직전에 마스킹한다. 수정 화면 단건 모델은 건드리지 않는다.
        registry.addInterceptor(new MaskingInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/img/**", "/vendor/**",
                        "/favicon.ico", "/error", "/ws/**",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/**");
    }

    @Bean
    public AuditInterceptor auditInterceptor(org.springframework.context.ApplicationEventPublisher eventPublisher) {
        return new AuditInterceptor(eventPublisher);
    }

    @Bean
    public AuthInterceptor authInterceptor() {
        return new AuthInterceptor();
    }

    @Bean
    public MenuPermissionInterceptor menuPermissionInterceptor(PermissionService permissionService,
            BoardService boardService) {
        return new MenuPermissionInterceptor(permissionService, boardService);
    }

    /**
     * 세션 만료 시 중복 로그인 레지스트리의 사용자 매핑을 지운다.
     * LoginSessionRegistry는 @Component이지만 HttpSessionListener는 자동 등록되지 않으므로
     * ServletListenerRegistrationBean으로 서블릿 컨테이너에 연결한다.
     */
    @Bean
    public ServletListenerRegistrationBean<HttpSessionListener> loginSessionListener(
            LoginSessionRegistry loginSessionRegistry) {
        return new ServletListenerRegistrationBean<>(loginSessionRegistry);
    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        Properties prop = new Properties();
        prop.setProperty("org.springframework.dao.DataAccessException", "sample/egovSampleError");
        prop.setProperty("org.springframework.transaction.TransactionException", "sample/egovSampleError");
        prop.setProperty("org.egovframe.rte.fdl.cmmn.exception.EgovBizException", "sample/egovSampleError");
        prop.setProperty("org.springframework.security.AccessDeniedException", "sample/egovSampleError");
        // 구체 예외 매핑이 Throwable 기본 매핑보다 우선해 미존재 URL이 404로 응답한다.
        prop.setProperty("org.springframework.web.servlet.resource.NoResourceFoundException", "error/error");
        prop.setProperty("org.springframework.web.servlet.NoHandlerFoundException", "error/error");
        prop.setProperty("java.lang.Throwable", "sample/egovSampleError");

        Properties statusCode = new Properties();
        statusCode.setProperty("sample/egovSampleError", "500");
        statusCode.setProperty("error/error", "404");

        SimpleMappingExceptionResolver smer = new SimpleMappingExceptionResolver();
        smer.setDefaultErrorView("sample/egovSampleError");
        smer.setExceptionMappings(prop);
        smer.setStatusCodes(statusCode);
        resolvers.add(smer);
    }

}
