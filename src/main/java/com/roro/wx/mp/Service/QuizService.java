package com.roro.wx.mp.Service;

import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.LanternEvent;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.object.User;
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
    Map<String,Quiz> recentCommit; //用来记录用户最近的一次提交,用于识别他们修改的对象,就不写入数据库了
    Set<String> commandList;
    Map<Character,Integer> choiceIdxMap; //用于处理中文和数字下标到int之间的映射
    @PostConstruct
    public void init(){
        quizMap = new HashMap<>();
        commandList = new HashSet<>();
        recentCommit = new HashMap<>();
        choiceIdxMap = new HashMap<>();
        try{
            Set<Object> keyset = redisUtils.hkeys(quizDBKey);
            for(Object key:keyset){
                String quizText = (String)(redisUtils.hget(quizDBKey,key));
                quizMap.put((String)key,quizText);
            }
            commandList.add("题干");
            commandList.add("选项1");
            commandList.add("选项2");
            commandList.add("选项3");
            commandList.add("选项4");
            commandList.add("选项5");
            commandList.add("选项一");
            commandList.add("选项二");
            commandList.add("选项三");
            commandList.add("选项四");
            commandList.add("选项五");
            choiceIdxMap.put('一',0);
            choiceIdxMap.put('二',1);
            choiceIdxMap.put('三',2);
            choiceIdxMap.put('四',3);
            choiceIdxMap.put('五',4);
            choiceIdxMap.put('1',0);
            choiceIdxMap.put('2',1);
            choiceIdxMap.put('3',2);
            choiceIdxMap.put('4',3);
            choiceIdxMap.put('5',4);

            return;
        }catch(Exception e){
            log.error("从Redis中读取赏春踏青活动题库时出错.");
        }
    }
    //返回结果超过这个值,会导致响应过慢,而无法响应.已知7条时会不予响应.
    public int MAX_RETURN_RESULT = 6;

    /**
     *
     * @param user   发起查询的用户
     * @param keyword  发起查询使用的关键词
     * @return
     * 用来 检索 / 修改 /更新 / 添加 题目
     * 用户信息主要用来记录并判断是否有权限能够修改题目
     */
    public String retrieval(User user, String keyword){

        //处理特殊的请求"添加新题目",添加一条空的quiz记录,并写入到该用户的最近提交记录里
        if(keyword.equals("添加新题目")){
            Quiz q = new Quiz();
            q.setLabel("#9999");
            addQuiz(q);
            recentCommit.put(user.getKey(),q);
            return q.toFormatString();
        }
        //处理中文简单的指令.方便大家一起来修改题目
        if(keyword.matches("^(选项. |题干 ).*")){
            if(!recentCommit.containsKey(user.getKey()) || recentCommit.get(user.getKey())==null){
                throw new MpException(ErrorCodeEnum.NO_RECENT_COMMIT_QUIZ);
            }else{
                Quiz q = recentCommit.get(user.getKey());
                if(keyword.startsWith("题干")){
                    if(keyword.charAt(2)!=' ' && keyword.charAt(2)!=':'){
                        throw new MpException(ErrorCodeEnum.QUIZ_WRONG_COMMAND);
                    }
                    String content = keyword.substring(3,keyword.length());
                    q.setBody(content);
                    recentCommit.put(user.getKey(),q);
                    addQuiz(q);
                    return q.toFormatString();
                }else if(keyword.startsWith("选项")){
                    char choiceIdx = keyword.charAt(2);
                    if(choiceIdxMap.containsKey(choiceIdx)){
                        int idx = choiceIdxMap.get(choiceIdx); //修改的选项编号 ?
                        String content = keyword.substring(4,keyword.length());
                        String[] splitArr = content.split(" ");//用单个空格区分
                        if(splitArr.length==1){
                            //如果只有一段输入,默认直接添加到answer后边
                            Quiz.Option opt = q.getOptionList().get(idx);
                            if(opt.getResult()==null || opt.getResult().equals(""))
                                opt.setResult(splitArr[0]);
                            else
                                opt.setResult(opt.getResult()+'/'+splitArr[0]);
                            //q.getOptionList().set(idx,opt);
                        }else if(splitArr.length==2){
                            //否则视作第一次添加,覆盖该选项的choice,并覆盖answer
                            List<Quiz.Option> optionList = q.getOptionList();
                            while(idx>=optionList.size()){
                                optionList.add(new Quiz.Option());
                            }
                            Quiz.Option opt = optionList.get(idx);
                            opt.setChoice(splitArr[0]);
                            opt.setResult(splitArr[1]);
                            //optionList.set(idx,opt);
                            //q.setOptionList(optionList);
                        }else{
                            throw new MpException(ErrorCodeEnum.QUIZ_WRONG_CHOICE);
                        }
                        recentCommit.put(user.getKey(),q);
                        addQuiz(q);
                        return q.toFormatString();
                    }else{
                        throw new MpException(ErrorCodeEnum.QUIZ_WRONG_COMMAND);
                    }
                }
            }
        }

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
        if(selected.size()==1){
            recentCommit.put(user.getKey(),JsonUtils.json2Quiz(selected.get(0)));
        }else{
            recentCommit.put(user.getKey(),null);
        }

        //包装返回结果
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
