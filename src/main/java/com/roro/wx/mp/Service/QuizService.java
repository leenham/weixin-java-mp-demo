package com.roro.wx.mp.Service;

import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.LanternEvent;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.utils.JsonUtils;
import com.roro.wx.mp.utils.LanternUtils;
import com.roro.wx.mp.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 处理英雄杀答题类活动,根据关键字,执行检索功能.
 */
@Slf4j
@Service
public class QuizService {
    @Value(value="${link.jump.yingxiongsha}")
    public String jumplink;

    @Value(value="${roconfig.quiz.butterfly}")
    public String quizDBKey;  //存储当前题库的哈希表所对应的键值

    @Autowired
    RedisUtils redisUtils;

    int LIMITSIZE = 5;
    Map<String,String> quizMap;  //题库在内存中的备份,每次部署会从数据库中读取.

    @PostConstruct
    public void init(){
        if(quizMap==null){
            quizMap = new HashMap<>();
        }
        try{
            Set<Object> keyset = redisUtils.hkeys(quizDBKey);
            for(Object key:keyset){
                String quizText = (String)(redisUtils.hget(quizDBKey,key));
                quizMap.put((String)key,quizText);
            }
            return;
        }catch(Exception e){
            log.error("从Redis中读取赏春踏青活动题库时出错.");
        }
    }
    //返回结果超过这个值,会导致响应过慢,而无法响应.已知7条时会不予响应.
    public int MAX_RETURN_RESULT = 6;
    public String retrieval(String keyword){
        //以json格式作为输入的,当做更新记录处理
        if(keyword.startsWith("{\"body\":") && keyword.matches(".*#[0-9]{4}.*")) {
            String content = "";
            try{
                //如果这个语句能顺利执行，说明输入是合法的
                Quiz quiz = JsonUtils.json2Quiz(keyword);
                addQuiz(quiz);
                return String.format("已成功更新一条记录:%s",quiz.getLabel());
            }catch (Exception e2){
                throw new MpException(ErrorCodeEnum.QUIZ_ILLEGAL_UPDATE);
            }
        }
        //以# + 四位数字作为输入的,视作请求查看json源码(用作更新记录)
        if(keyword.matches("#[0-9]{4}")){
            Integer idx = Integer.parseInt(keyword.substring(1,5));
            if(quizMap.containsKey(keyword)){
                return quizMap.get(keyword);
            }else{
                throw new MpException(ErrorCodeEnum.QUIZ_UNEXIST_LABEL);
            }
        }

        //否则遍历题库,寻找匹配项
        StringBuffer sb = new StringBuffer();
        List<String> selected = new ArrayList<>();
        for(String key:quizMap.keySet()){
            if(quizMap.get(key).contains(keyword)){
                selected.add(quizMap.get(key));
            }
        }
        if(selected.size()>LIMITSIZE){
            sb.append(String.format("共检索到%d条记录,仅返回前%d条记录\n",selected.size(),LIMITSIZE));
        }else if(selected.size()==0){
            throw new MpException(ErrorCodeEnum.NO_QUIZ_FOUND);
        }
        for(int i=0;i<Math.min(LIMITSIZE,selected.size());i++){
            Quiz qz = JsonUtils.json2Quiz(selected.get(i));
            sb.append(qz.toFormatString());
        }
        sb.append(jumplink);//最后添加跳转回游戏的链接
        return sb.toString();
    }



    //添加一个新的quiz.如果quiz的标签过大,则将其修正到quizMap.size()大小
    public void addQuiz(Quiz q){
        Integer idx = q.getIndex();
        if(idx==null || idx>=quizMap.size()){
            q.setLabel(quizMap.size());
            idx = q.getIndex();
        }
        quizMap.put(q.getLabel(),q.toJsonString());
        redisUtils.hset(quizDBKey,q.getLabel(),q.toJsonString());
    }

    public Map<String,String> getQuizMap(){
        return quizMap;
    }
}
