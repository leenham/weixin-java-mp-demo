package com.github.binarywang.demo.wx.mp.utils;

import com.github.binarywang.demo.wx.mp.object.LanternEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.alibaba.fastjson.*;
/**
 * @author Binary Wang(https://github.com/binarywang)
 */
public class JsonUtils {
    public static Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    public static LanternEvent fromJson(String obj){
        return gson.fromJson(obj,LanternEvent.class);
    }
}
