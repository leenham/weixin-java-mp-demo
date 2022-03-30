package com.roro.wx.mp;

import org.junit.jupiter.api.Test;

public class GrammerTest {
    @Test
    public void test(){
        String keyword1 = "授权 gh_e14b7dc2719d oPTW655tUQjQik_am-J4SUwMCAWc";
        System.out.println(keyword1.matches("^授权 \\S* \\S*$"));
    }

    @Test
    public void regTest(){
        //String reg = "(选项)?(1|2|3|4|5|一|二|三|四|五)(\\s+\\S+){1,2}";
        String reg = "#(添|加|\\+|添加)(题|新题)?";
        String keyword = "选项1 啥东西";
        System.out.println(keyword.matches(reg));

    }
}
