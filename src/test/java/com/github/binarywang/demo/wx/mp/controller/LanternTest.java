package com.github.binarywang.demo.wx.mp.controller;

import com.alibaba.fastjson.JSON;
import com.github.binarywang.demo.wx.mp.Service.LanternService;
import com.github.binarywang.demo.wx.mp.object.LanternEvent;
import com.github.binarywang.demo.wx.mp.object.Option;
import com.github.binarywang.demo.wx.mp.utils.JsonUtils;
import com.github.binarywang.demo.wx.mp.utils.LanternUtils;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LanternTest {



    @Autowired
    LanternService lanternService;
    @Test
    public void lanternTest(){
        int a = Integer.parseInt("0109");

        String keyword = "{\"event\":\"火焰形态改变，元宵以肉眼可见的速度变熟\",\"label\":\"#0012\",\"options\":[{\"option\":[\"直接煮好\",\"煮好所有元宵\"]},{\"option\":[\"放任不管就不不不不不管\",\"无奖惩\"]}]}";
        System.out.println(keyword.matches(".*#[0-9]{4}.*"));
        System.out.println(keyword.startsWith("{\"event\":"));

    }

}
