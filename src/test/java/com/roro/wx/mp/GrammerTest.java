package com.roro.wx.mp;

import org.junit.jupiter.api.Test;

public class GrammerTest {
    @Test
    public void test(){
        String keyword1 = "题干 asdaz";
        String keyword2 = "选项1 是什么呢";
        String keyword3 = "选项2";
        String keyword4 = "选项五";
        String keyword5 = "选项题干 ";
        System.out.println(keyword1.matches("^(选项. |题干 ).*"));
        System.out.println(keyword2.matches("^(选项. |题干 ).*"));
        System.out.println(keyword3.matches("^(选项. |题干 ).*"));
        System.out.println(keyword4.matches("^(选项. |题干 ).*"));
        System.out.println(keyword5.matches("^(选项. |题干 ).*"));
    }
}
