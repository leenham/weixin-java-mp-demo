package com.roro.wx.mp;

import com.roro.wx.mp.Service.QuizService;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.utils.JsonUtils;
import com.roro.wx.mp.utils.RedisUtils;
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
    @Test
    public void myTest(){
        Quiz quiz = new Quiz();
        quiz.setLabel(2);
        quiz.setBody("你遇到了一个美丽的女子,她自称是蝴蝶仙子,可以交给你一些秘籍,帮你抓更多蝴蝶");
        List<Quiz.Option> list = new ArrayList<>();
        list.add(new Quiz.Option("彬彬有礼,坦然处之","花蝴蝶*5"));
        list.add(new Quiz.Option("好奇上前一探究竟",""));
        list.add(new Quiz.Option("越漂亮的女人越危险",""));
        quiz.setOptionList(list);
        System.out.println(quiz.toFormatString());

        String json = quiz.toJsonString();
        System.out.println(json);

        Quiz nquiz = JsonUtils.json2Quiz(json);
        System.out.println(nquiz);
        quizService.addQuiz(quiz);
        return;
    }

    @Test
    public void myTest2(){
        //get QUIZ database
        Map<String,String> quizmap = quizService.getQuizMap();
        Quiz[] qarr = new Quiz[quizmap.size()];
        for(String key:quizmap.keySet()){
            Quiz q = JsonUtils.json2Quiz(quizmap.get(key));
            qarr[q.getIndex()] = q;
        }
        for(int i=0;i<qarr.length;i++){
            String  text = StringUtils.trimLeadingWhitespace(qarr[i].toTestString());
            System.out.println(text);
        }
    }

    @Test
    public void myTest3(){


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
}
