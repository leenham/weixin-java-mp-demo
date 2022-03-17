package com.github.binarywang.demo.wx.mp.utils;

import com.github.binarywang.demo.wx.mp.object.Cipher;
import com.github.binarywang.demo.wx.mp.object.LanternEvent;
import com.github.binarywang.demo.wx.mp.object.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.alibaba.fastjson.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * @author Binary Wang(https://github.com/binarywang)
 */
public class JsonUtils {
    public static Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public static String toJson(Object obj) {
        return StringUtils.trimAllWhitespace(gson.toJson(obj));
    }

    public static Object fromJson(String obj,Class cls){
        return gson.fromJson(obj,cls);
    }

    public static String cipher2Json(Cipher cipher){
        return JsonUtils.toJson(cipher);
    }
    public static Cipher json2Cipher(String json){
        return (Cipher)JsonUtils.fromJson(json,Cipher.class);
    }
    public static String user2Json(User user){
        return JsonUtils.toJson(user);
    }
    public static User json2User(String json){
        return (User)JsonUtils.fromJson(json,User.class);
    }

}
