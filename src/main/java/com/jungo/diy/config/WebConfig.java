package com.jungo.diy.config;

import com.jungo.diy.interceptor.ExtInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.*;
import java.io.IOException;


@Configuration
public class WebConfig implements WebMvcConfigurer {
   
    @Bean
    public FilterRegistrationBean<Filter> contextCleanupFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response,
                                 FilterChain chain) throws IOException, ServletException {
                try {
                    chain.doFilter(request, response);
                } finally {
                    RequestContext.clear();  // 确保每次请求后清理
                }
            }
        });
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ExtInterceptor());
    }
}