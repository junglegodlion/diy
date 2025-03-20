package com.jungo.diy.config;

import com.jungo.diy.interceptor.ExtInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<Filter> contextCleanupFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() { // 改用Spring提供的基类
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain)
                    throws IOException, ServletException {

                try {
                    chain.doFilter(request, response);
                } finally {
                    if (!response.isCommitted()) { // 关键检查
                        RequestContext.clear();
                    }
                }
            }
        });
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 1); // 确保最后执行
        return registration;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ExtInterceptor());
    }
}