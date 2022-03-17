package com.github.binarywang.demo.wx.mp.Service;

import com.github.binarywang.demo.wx.mp.object.User;
import com.github.binarywang.demo.wx.mp.utils.JsonUtils;
import com.github.binarywang.demo.wx.mp.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 提供用户相关的操作,包括CRUD,以及记录用户状态
 * 用户表以Hash表的形式直接存储在redis里
 * 每个appID都会对应一个用户表.
 */
@Service
@Slf4j
public class UserService {
    @Autowired
    RedisUtils redisUtils;
    public User getUser(String appID,String ID){
        if(redisUtils.hexists(appID,ID)){
            return JsonUtils.json2User((String)redisUtils.hget(appID,ID));
        }else{
            User user = new User(appID,ID);
            redisUtils.hset(appID,ID,JsonUtils.user2Json(user));
            return user;
        }
    }

}
