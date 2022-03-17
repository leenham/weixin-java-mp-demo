package com.roro.wxmp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUtils {
    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    public boolean hasKey(String key){
        return redisTemplate.hasKey(key);
    }
    public void set(String key,Object val){
        redisTemplate.opsForValue().set(key,val);
    }
    public Object get(String key){
        return redisTemplate.opsForValue().get(key);
    }

    public void hset(String key,Object hashkey,Object val){
        redisTemplate.opsForHash().put(key,hashkey,val);
    }
    public Object hget(String key,Object hashkey){
        return redisTemplate.opsForHash().get(key,hashkey);
    }

    public boolean hexists(String key,Object hashkey){
        return redisTemplate.opsForHash().hasKey(key,hashkey);
    }
    public long add(String key,Object obj){
        return redisTemplate.opsForSet().add(key,obj);
    }
}
