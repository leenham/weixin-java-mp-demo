package com.roro.wx.mp;

import com.roro.wx.mp.Service.QuizService;
import com.roro.wx.mp.Service.UserService;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.AuthUtils;
import com.roro.wx.mp.utils.JsonUtils;
import com.roro.wx.mp.utils.RedisUtils;
import com.roro.wx.mp.utils.TestUtils;
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
        /*Map<String,User> userMap = userService.getUserMap();
        for(String key:userMap.keySet()){
            //String key = String.format("#%04d",i);
            User user = userMap.get(key);
            user.setStatus(1);
            userService.updateUser(user);
            System.out.println(String.format("%s:%02d",user.getID(),user.getStatus()));
        }*/
        Map<String,User> userMap = userService.getUserMap();
        //修改指定用户信息

        //User user = userMap.get("gh_e14b7dc2719doPTW659_noDaroyZ2M55UmkAt9n4");
        //System.out.println(JsonUtils.user2Json(user));
        User user;
        /*for(String key:userMap.keySet()){
            user = userMap.get(key);
            if(AuthUtils.isRoot(user)){
                System.out.println(JsonUtils.user2Json(user));
            }
        }*/
        user = userMap.get("gh_e14b7dc2719doPTW658ZFaMB46IKzXPzx6PBmils");
        user.setName("小尾巴");
        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW659mS4hTqy6bJHNUguA8VDgg");
//        user.setName("电电");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW65675LhREqTvjyMXdjeNvpho");
//        user.setName("倩倩");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW65zNjNUaQEkBbHbgVqYMD4RE");
//        user.setName("白与黑");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW65yGizU_2PjdxYDL2YY5mEHQ");
//        user.setName("左左");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW650JfS3UsMGsBTKCk5Zv3g4A");
//        user.setName("如梦");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW659d8TsA1UdVDDHUUmed_DRY");
//        user.setName("花柯");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW656Ol5Xjv2GKkbib9tYVaCNo");
//        user.setName("电电");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW65wpPnIDvUov4kWtiIoR1lP4");
//        user.setName("satie");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW656_eyyFQQ9RCSABtICihOf8");
//        user.setName("小超人");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW652Mt5AIS_EF-sEubOu_FK3I");
//        user.setName("米花糖");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW65_IfhY4ay0yLJH8xEc480Yg");
//        user.setName("瞬间");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW650nADaYTYRzIOWsRuHs1UN4");
//        user.setName("围城");
//        userService.updateUser(user);
//        user = userMap.get("gh_e14b7dc2719doPTW658eqHCF17R9NsPSyMI1pre0");
//        user.setName("开心果");
//        userService.updateUser(user);


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


    //打印当前题库
    @Test
    public void printQuizDB(){
        Map<String,Quiz> map = quizService.getQuizMap();
        for(int i=1;i<map.keySet().size();i++){
            String key = String.format("#%04d",i);
            System.out.println(map.get(key).toJsonString());
        }
    }

    //
    //统计当前没有结果的选项.
    @Test
    public void printOptionWithoutResult(){
        Map<String,Quiz> map = quizService.getQuizMap();
        int index = 1;
        for(int i=1;i<map.keySet().size();i++){
            String key = String.format("#%04d",i);
            Quiz q = map.get(key);
            List<Quiz.Option> list = q.getOptionList();
            for(int j=0;j<list.size();j++){
                Quiz.Option opt = list.get(j);
                if(!opt.getChoice().equals("") && opt.getResult().equals("")){
                    System.out.println(String.format("%d:[%s]%s",index++,q.getLabel(),opt.getChoice()));
                }
            }
        }
    }


    //删除被清空了的题目
    @Test
    public void deleteQuizDB(){
        Map<String,Quiz> map = quizService.getQuizMap();
        quizService.delQuiz(map.get("#0156"));
        quizService.delQuiz(map.get("#0155"));
        quizService.delQuiz(map.get("#0154"));
        quizService.delQuiz(map.get("#0153"));
        quizService.delQuiz(map.get("#0152"));
        quizService.delQuiz(map.get("#0151"));
        quizService.delQuiz(map.get("#0150"));

        System.out.println("DELETE SUCCESS");
        printQuizDB();
    }
}
