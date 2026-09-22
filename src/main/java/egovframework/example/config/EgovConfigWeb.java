package egovframework.example.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.egovframe.rte.psl.dataaccess.util.EgovMap;
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
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import arkive.admin.system.service.SystemService;
import egovframework.example.pagination.EgovKrdsPaginationRenderer;
import egovframework.example.pagination.EgovPaginationDialect;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        registry.addInterceptor(new MenuInterceptor());
    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        Properties prop = new Properties();
        prop.setProperty("org.springframework.dao.DataAccessException", "sample/egovSampleError");
        prop.setProperty("org.springframework.transaction.TransactionException", "sample/egovSampleError");
        prop.setProperty("org.egovframe.rte.fdl.cmmn.exception.EgovBizException", "sample/egovSampleError");
        prop.setProperty("org.springframework.security.AccessDeniedException", "sample/egovSampleError");
        prop.setProperty("java.lang.Throwable", "sample/egovSampleError");

        Properties statusCode = new Properties();
        statusCode.setProperty("sample/egovSampleError", "400");
        statusCode.setProperty("sample/egovSampleError", "500");

        SimpleMappingExceptionResolver smer = new SimpleMappingExceptionResolver();
        smer.setDefaultErrorView("sample/egovSampleError");
        smer.setExceptionMappings(prop);
        smer.setStatusCodes(statusCode);
        resolvers.add(smer);
    }

    @Bean
    public MappingJackson2JsonView jsonView() {
        return new MappingJackson2JsonView();
    }

}
