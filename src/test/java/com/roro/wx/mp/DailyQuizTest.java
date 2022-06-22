package com.roro.wx.mp;

import com.roro.wx.mp.Service.DailyQuizService;
import com.roro.wx.mp.Service.QuizService;
import com.roro.wx.mp.Service.UserService;
import com.roro.wx.mp.object.DailyQuiz;
import com.roro.wx.mp.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class DailyQuizTest
{
    @Autowired
    DailyQuizService dailyQuizService;
    @Autowired
    RedisUtils redisUtils;
    @Autowired
    UserService userService;

    @Test
    public void mytest(){
        Map<String,DailyQuiz> map = dailyQuizService.getDailyQuizMap();
        for(String key : map.keySet()){
            System.out.println(map.get(key).toFormatString());

        }
        dailyQuizService.delQuiz(map.get("Q414"));
        dailyQuizService.delQuiz(map.get("q414"));
        dailyQuizService.delQuiz(map.get("Q187"));

    }
}
