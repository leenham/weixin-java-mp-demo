package com.roro.wx.mp;

import com.roro.wx.mp.Service.QuizService;
import com.roro.wx.mp.Service.UserService;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.AuthUtils;
import com.roro.wx.mp.utils.JsonUtils;
import com.roro.wx.mp.utils.RedisUtils;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class QuizTest {
    @Autowired
    QuizService quizService;
    @Autowired
    RedisUtils redisUtils;
    @Autowired
    UserService userService;


    @Autowired
    WxMpService weixinService;
    @Test
    public void myTest() throws WxErrorException {
        Map<String,Quiz> quizMap = quizService.getQuizMap();
        for(int i=0;i<quizMap.size();i++){
            String key = String.format("#%04d",i);
            System.out.println(quizMap.get(key).toFormatString());
        }
        return;
    }

    @Test
    public void myTest2(){

    }

    @Test
    public void myTest3(){

        /*String ID = "";
        Map<String,User> userMap = userService.getUserMap();
        for(String key:userMap.keySet()){
            if(key.contains(ID)){
                System.out.println(userMap.get(key).toTestString());
            }
        }*/
        String appID = "gh_e14b7dc2719d";
        String ID = "oPTW655tUQjQik_am-J4SUwMCAWc";
        User user = userService.getUser(appID,ID);
        //System.out.println(userService.getUserMap().size());
        userService.authorize(user, AuthUtils.SUPERROOT | AuthUtils.ROOT);
        System.out.println(user.toTestString());
        System.out.println("授权成功");

        //授权普通管理的代码
        /*String appID = "gh_e14b7dc2719d";
        String ID = "";
        User user = userService.getUser(appID,ID);
        userService.authorize(user, AuthUtils.SUPERROOT);
        System.out.println(user.toTestString());
        System.out.println("授权成功");*/
    }
    @Test
    public void addQuizTest(){
        Quiz quiz = new Quiz();
        List<Quiz.Option> list = new ArrayList<>();
        list.add(new Quiz.Option("选项一","结果1"));
        list.add(new Quiz.Option("选项二","结果2"));
        list.add(new Quiz.Option("选项三","结果3"));
        quiz.setLabel(9000);
        quiz.setBody("这是一个测试题干,这是一个测试题干.");
        quiz.setOptionList(list);

        System.out.println(quiz.toJsonString());
        System.out.println(quiz.toFormatString());
    }

    @Test
    public void printAllUser(){
        Map<String,User> map = userService.getUserMap();
        for(String key:map.keySet()){
            User user = map.get(key);
            System.out.println(String.format("ID:%s.",user.getID()));
        }
    }
}
