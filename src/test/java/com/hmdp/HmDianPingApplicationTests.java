package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.hmdp.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@SpringBootTest
class HmDianPingApplicationTests {

    @Test
    public void test(){
        com.hmdp.entity.User user = new com.hmdp.entity.User();
        user.setId(1111111111L);
        user.setNickName("user_tskdfs");
        user.setIcon("qfasfsa");


     /*  BiFunction biF= new BiFunction<String,Object,String>(){
           @Override
           public String apply(String fieldName, Object fieldValue) {
               if(fieldValue!=null){
                   return fieldValue.toString();
               }
               return null;
           }

       };

        Map<String, Object> map = BeanUtil.beanToMap(user,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor(biF));*/

        Map<String, Object> map = BeanUtil.beanToMap(user,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)->{
                    if(fieldValue!=null){
                        return fieldValue.toString();
                    }
                    return null;
                }));
        System.out.println(map);


    }
}
