package com.roro.wx.mp;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

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
    @Test
    public void checkReplace(){
        String answer = "丧131心病狂";
        if(answer.matches(".*[0-9]")){
            //如果以0-9数字结尾,则视作备选答案,以增加命中概率,但是需要把末位数字对外隐藏
            //故在输出之前做处理.
            answer = answer.replaceAll("[0-9]","ww");
        }
        System.out.println(answer);
    }
    @Test
    public void datetest(){
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now);
        System.out.println(now.getDayOfMonth());
        System.out.println(now.getDayOfYear());
        System.out.println(now.getMonth().compareTo(Month.JULY));
        System.out.println(now.getYear());
    }
}
