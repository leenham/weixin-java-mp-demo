package com.roro.wx.mp.utils;

import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.object.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.util.StringUtils;

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
    public static Quiz json2Quiz(String json){ return (Quiz)JsonUtils.fromJson(json,Quiz.class);}
}
