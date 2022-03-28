package com.roro.wx.mp;

import org.junit.jupiter.api.Test;

public class GrammerTest {
    @Test
    public void test(){
        String keyword1 = "授权 gh_e14b7dc2719d oPTW655tUQjQik_am-J4SUwMCAWc";
        System.out.println(keyword1.matches("^授权 \\S* \\S*$"));
    }
}
