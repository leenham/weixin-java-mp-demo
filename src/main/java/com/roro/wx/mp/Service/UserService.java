package com.roro.wx.mp.Service;

import com.alibaba.fastjson.JSON;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.DateUtils;
import com.roro.wx.mp.utils.JsonUtils;
import com.roro.wx.mp.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 提供用户相关的操作,包括CRUD,以及记录用户状态
 * 用户表以Hash表的形式直接存储在redis里
 * 每个appID都会对应一个用户表.
 */
@Service
@Slf4j
public class UserService {
    @Value("${roconfig.user.table}")
    private String userTableKey;

    @Autowired
    RedisUtils redisUtils;

    Map<String,User> userMap;
    @PostConstruct
    public void init() {
        if(userMap==null)
            userMap = new HashMap<>();
        try {
            Set<Object> keyset = redisUtils.hkeys(userTableKey);
            for(Object key:keyset){
                User value = JsonUtils.json2User((String)redisUtils.hget(userTableKey,key));
                userMap.put((String)key,value);
            }
            return;
        }catch(Exception e){
            log.error("从redis中读取用户表时出错");
        }
    }

    public User getUser(String appID, String ID){
        String userKey = appID+ID;
        if(userMap.containsKey(userKey)){
            return userMap.get(userKey);
        }else{
            User user = new User(appID,ID);
            redisUtils.hset(userTableKey,userKey,JsonUtils.user2Json(user));
            return user;
        }
    }

    public void addUser(User user){

    }

}
