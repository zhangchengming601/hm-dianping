package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //注册第一个拦截器---拦截所有请求
        InterceptorRegistration refreshTokenIntercrptor = registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate));
        refreshTokenIntercrptor.excludePathPatterns("/**");
        refreshTokenIntercrptor.order(1);

        //注册第二个拦截器
        InterceptorRegistration interceptor = registry.addInterceptor(new LoginInterceptor());
        interceptor.excludePathPatterns(
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login"
        );
        interceptor.order(2);

    }
}
