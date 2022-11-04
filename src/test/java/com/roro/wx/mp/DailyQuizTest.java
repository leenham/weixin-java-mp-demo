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
        //dailyQuizService.delQuiz(map.get("Q414"));
        //dailyQuizService.delQuiz(map.get("q414"));
        //dailyQuizService.delQuiz(map.get("Q187"));

    }

    @Test
    public void printDailyQuizDB(){
        int cnt = 0;
        Map<String,DailyQuiz> map = dailyQuizService.getDailyQuizMap();
        for(int i=1;i<1000;i++){
            String label = String.format("Q%d",i);
            if(map.containsKey(label)){
                if(!map.get(label).getBody().equals("")) {
                    System.out.println(map.get(label).toFormatString());
                    cnt++;
                }
            }
        }
        System.out.println(String.format("当前共计录入%d道题",cnt));
    }
}
