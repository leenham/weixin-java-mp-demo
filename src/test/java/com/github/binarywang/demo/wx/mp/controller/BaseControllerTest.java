package com.github.binarywang.demo.wx.mp.controller;

import com.github.binarywang.demo.wx.mp.Service.LanternService;
import com.github.binarywang.demo.wx.mp.object.LanternEvent;
import com.thoughtworks.xstream.XStream;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeTest;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公共测试方法和参数.
 *
 * @author Binary Wang
 * @date 2019-06-14
 */
public abstract class BaseControllerTest {
    private static final String ROOT_URL = "http://127.0.0.1:8080/";

    @BeforeTest
    public void setup() {
        RestAssured.baseURI = ROOT_URL;
    }

}
