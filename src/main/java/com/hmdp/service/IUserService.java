package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {


    /**
     * 主要功能：发送验证码
     * 参数：获取前端传来的手机号； 以及tomcat容器中保存的session信息
     */
    Result sendCode(String phone, HttpSession session);


    /**
     * 验证登录过程中的验证码是否正确
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

}
