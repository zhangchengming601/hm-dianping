package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;


import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 注入容器中的StringRedisTemplate
     * */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 主要功能：发送验证码
     * 参数：获取前端传来的手机号； 以及tomcat容器中保存的session信息
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 验证手机号格式
        //调用utils包中的RegexUtils类中的方法验证手机号格式
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式不正确");
        }

        //2. 手机号格式符合，则生成验证码
        String code = RandomUtil.randomNumbers(6);

       /* //3. 保存验证码到session
        session.setAttribute("code",code);*/

        //保存验证码到redis中
        // key就是login:code:手机号
        // value就是验证码
        // 参数 RedisConstants.LOGIN_CODE_TTL  表示有效期的时间
        // 参数 TimeUnit.MINUTES表示 的是分钟
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY +phone,code,
                RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //4. 模拟发送验证码
        log.info("发送验证码成功，验证码为{}",code);
        //返回ok
        return Result.ok();

    }


    /**
     * 验证登录过程中的验证码是否正确
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }

        //校验验证码
        //从redis中获得phone对应的验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode==null||!cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }

        //如果验证码一致，则根据手机号查询数据库中的用户信息
        //select * from tb_user where phone=?
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,phone);
        User user = this.getOne(queryWrapper);

        //如果数据库中没有该用户，则创建用户，并将用户信息保存到数据库
        if(user==null){
            user=createUserWithPhone(phone);
        }

        //7.保存用户信息到redis中
        //随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
        //将user转为userDTO---节省服务器资源占用；隐藏敏感信息
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        //将userDTO这个java对象转化为Map类型

        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO,new HashMap<String,Object>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)->{
                    if(fieldValue!=null){
                        return fieldValue.toString();
                    }
                    return null;
                }));
        //将以 token - User（hash结构）的key-value的键值对存储在redis中
        String tokenKey=RedisConstants.LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userDTOMap);
        //设置redis中的token的有效期
        stringRedisTemplate.expire(tokenKey,RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);

        //将token封装到Result中进行返回
        return Result.ok(token);
    }


    /**
     * 如果登录的用户不存在，则通过手机号创建用户
     * */
    private User createUserWithPhone(String phone){
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));

        this.save(user);
        return user;
    }
}
