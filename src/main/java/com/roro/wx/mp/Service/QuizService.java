package com.roro.wx.mp.Service;

import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.LanternEvent;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.AuthUtils;
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

    @Autowired
    UserService userService;

    int LIMITSIZE = 5;
    Map<String,Quiz> quizMap;  //题库在内存中的备份,每次部署会从数据库中读取.
    Map<String,Quiz> recentCommit; //用来记录用户最近的一次提交,用于识别他们修改的对象,就不写入数据库了
    @PostConstruct
    public void init(){
        quizMap = new HashMap<>();
        recentCommit = new HashMap<>();
        try{
            Set<Object> keyset = redisUtils.hkeys(quizDBKey);
            for(Object key:keyset){
                String quizText = (String)(redisUtils.hget(quizDBKey,key));
                quizMap.put((String)key,JsonUtils.json2Quiz(quizText));
            }
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
        /* 优先处理修改指令的请求 */
        try{
            //处理添加新题目的请求
            if(keyword.matches("^(添题|新题|加题|添加新题目)$")){
                if(!AuthUtils.isRoot(user.getAuthCode())){
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                //优先寻找题库中有没有没任何内容的编号,给其分配旧的标签
                for(String key:quizMap.keySet()){
                    Quiz quiz = quizMap.get(key);
                    if(quiz.getBody().equals("") && quiz.getTitle().equals("") && quiz.getOptionList().size()==0){
                        recentCommit.put(user.getKey(),quiz);//添加新题目时,默认将其作为最近一次提交,方便直接修改
                        return quiz.toFormatString();
                    }
                }
                Quiz q = new Quiz();
                q.setLabel("#9999");
                addQuiz(q);
                recentCommit.put(user.getKey(),q);//添加新题目时,默认将其作为最近一次提交,方便直接修改
                return q.toFormatString();
            }
            //修改/更新/添加 选项
            if(keyword.matches("^(选项)?(1|2|3|4|5|一|二|三|四|五)(\\s+\\S+){1,2}")){
                if(!AuthUtils.isRoot(user.getAuthCode())) {
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                String[] splitArr = keyword.split("\\s+");
                int choiceIdx = getOptionNumberInCommand(splitArr[0]);
                if(!recentCommit.containsKey(user.getKey()) || recentCommit.get(user.getKey())==null){
                    throw new MpException(ErrorCodeEnum.NO_RECENT_COMMIT_QUIZ);
                }
                Quiz q = recentCommit.get(user.getKey());
                while(choiceIdx>=q.getOptionList().size()){
                    q.getOptionList().add(new Quiz.Option());
                }
                Quiz.Option option = q.getOptionList().get(choiceIdx);
                if(splitArr.length==2){
                    //选项一 内容 ==> 如果对应的选项为空,则覆盖选项;否则,给该选项添加新结果
                    if(splitArr[1].equals("清空")) {
                        //如果内容是特殊指令:清空,则清空该选项
                        option.setChoice("");
                        option.setResult("");
                    }else if(splitArr[1].equals("广告")){
                        if(!option.getChoice().startsWith("(广告)")){
                            option.setChoice("(广告)"+option.getChoice());
                        }
                    }else if(option.getChoice()==null || option.getChoice().equals("")) {
                        option.setChoice(splitArr[1]);
                    }else {
                        if(option.getResult()==null || option.getResult().equals("")){
                            option.setResult(splitArr[1]);
                        }else{
                            option.setResult(option.getResult() +'/' + splitArr[1]);
                        }
                    }
                }else if(splitArr.length==3){
                    //选项一 内容1 内容2 ==> 内容1覆盖选项,内容2覆盖结果
                    option.setChoice(splitArr[1]);
                    if(splitArr[2].equals("空") || splitArr[2].equals("清空")) {
                        splitArr[2] = "";
                    }
                    option.setResult(splitArr[2]);
                }else{
                    throw new MpException(ErrorCodeEnum.QUIZ_WRONG_COMMAND);
                }
                recentCommit.put(user.getKey(),q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //修改题干/标题
            if(keyword.matches("^(题干|标题)\\s+\\S+")){
                if(!AuthUtils.isRoot(user.getAuthCode())) {
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                String[] splitArr = keyword.split("\\s+");
                Quiz q = recentCommit.get(user.getKey());
                if(splitArr[1].equals("清空")){
                    splitArr[1] = "";
                }
                if(splitArr[0].equals("题干")){
                    q.setBody(splitArr[1]);
                }else{
                    q.setTitle(splitArr[1]);
                }
                recentCommit.put(user.getKey(),q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //清空指定编号的题
            if(keyword.matches("^清空 [0-9]{4}$")){
                if(!AuthUtils.isRoot(user.getAuthCode())) {
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                String label = '#'+keyword.substring(3,7);
                if(quizMap.containsKey(label)){
                    Quiz q = new Quiz();
                    q.setLabel(label);
                    recentCommit.put(user.getKey(),q);
                    addQuiz(q);
                    return q.toFormatString();
                }else{
                    return String.format("编号%s 不存在",label);
                }
            }
            throw new MpException(ErrorCodeEnum.UNHANDLED);
        }catch(MpException me){
            if(me.getErrorCode()!=ErrorCodeEnum.UNHANDLED.getCode()){
                throw me;
            }
        }

        //否则遍历题库,寻找匹配项
        StringBuffer sb = new StringBuffer();
        List<Quiz> selected = new ArrayList<>();
        for(String key:quizMap.keySet()){
            if(quizMap.get(key).toJsonString().contains(keyword)){
                selected.add(quizMap.get(key));
            }
        }
        if(selected.size()>LIMITSIZE){
            sb.append(String.format("共检索到%d条记录,仅返回前%d条记录\n",selected.size(),LIMITSIZE));
        }else if(selected.size()==0){
            throw new MpException(ErrorCodeEnum.NO_QUIZ_FOUND);
        }
        if(selected.size()==1){
            recentCommit.put(user.getKey(),selected.get(0));
        }else{
            recentCommit.put(user.getKey(),null);
        }
        //包装返回结果
        for(int i=0;i<Math.min(LIMITSIZE,selected.size());i++){
            Quiz qz = selected.get(i);
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
        }
        quizMap.put(q.getLabel(),q);
        redisUtils.hset(quizDBKey,q.getLabel(),q.toJsonString());
    }

    public Map<String,Quiz> getQuizMap(){
        return quizMap;
    }

    //从诸如"选项1" 等指令中,获取对应的编号或下标
    private int getOptionNumberInCommand(String str){
        char ch = str.charAt(str.length()-1);
        if(ch=='一' || ch=='1')return 0;
        if(ch=='二' || ch=='2')return 1;
        if(ch=='三' || ch=='3')return 2;
        if(ch=='四' || ch=='4')return 3;
        if(ch=='五' || ch=='5')return 4;
        throw new MpException(ErrorCodeEnum.QUIZ_WRONG_COMMAND);
    }
}
