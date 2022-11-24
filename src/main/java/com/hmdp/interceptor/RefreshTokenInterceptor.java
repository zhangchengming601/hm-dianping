package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * 拦截所有请求
 * 如果请求头中带有token，并且该token在redis中有记录，那么刷新该token的时间，然后放行
 * 如果请求头中没有token，或者有token但是在redis中没有该token的记录，则直接放行
 * */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    //由于LoginInterceptor类的对象并不是由Spring容器来创建的，而是我们自己手动new出来的，因此我们这里不能使用@Autowire来自动注入
    //需要我们在LoginInterceptor的构造器中初始化StringRedisTemplate
    private StringRedisTemplate stringRedisTemplate;
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取请求头中的token
        String token = request.getHeader("authorization");
        //判断一下token是否为空
        if(StrUtil.isBlank(token)){
            //如果token为空，则直接放行
            return true;
        }

        // 2. 基于token获取redis中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY+token);
        if(userMap.isEmpty()){
            //如果该token在redis中没有记录，则直接放行
            return true;
        }

        //将Map数据转为java对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);

        //5. 刷新redis中该token的有效期
        String tokenKey=RedisConstants.LOGIN_USER_KEY +token;
        stringRedisTemplate.expire(tokenKey,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;


    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //整个请求执行结束---也就是向前端响应数据成功之后，执行该方法

        //将ThreadLocal中的数据移除，避免内存泄露
        UserHolder.removeUser();
    }
}
