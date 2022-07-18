package com.roro.wx.mp.Service;


import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.DailyQuiz;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.object.Session;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.JsonUtils;
import com.roro.wx.mp.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;


@Slf4j
@Service
public class DailyQuizService{
    @Value(value="${roconfig.quiz.daily}")
    public String quizDBKey;

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    UserService userService;

    Map<String, DailyQuiz> quizMap;  //题库在内存中的备份,每次部署会从数据库中读取.
    @PostConstruct
    public void init(){
        quizMap = new HashMap<>();
        try{

            Set<Object> keyset = redisUtils.hkeys(quizDBKey);
            for(Object key:keyset){
                String quizText = (String)(redisUtils.hget(quizDBKey,key));
                quizMap.put((String)key, DailyQuiz.fromJson(quizText));
            }
            return;
        }catch(Exception e){
            log.error("从Redis中读取答题活动题库时出错.");
        }
    }
    int LIMITSIZE = 5;

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

    public String retrieval(Session ss){
        String keyword = ss.getMsg();
        User user = ss.getUser();
        //通过是否包含空格来区分是否是指令,(TODO : 判断逻辑待修改).
        if(keyword.contains(" ")){
            //权限控制
            if(!user.isRoot()) {
                throw new MpException(ErrorCodeEnum.NO_AUTH);
            }
            //如果找不到最近一次提交的结果,则直接驳回.(过期视作不存在)
            if(ss.getRecentDailyQuiz()==null){
                throw new MpException(ErrorCodeEnum.NO_RECENT_COMMIT_QUIZ);
            }

            if(keyword.matches("^(选项)?(1|2|3|4|5|一|二|三|四|五)(\\s+\\S+)")){
                String[] splitArr = keyword.split("\\s+");
                int choiceIdx = getOptionNumberInCommand(splitArr[0]);
                DailyQuiz q = ss.getRecentDailyQuiz();
                q.setOption(choiceIdx,splitArr[1]);
                ss.setRecentDailyQuiz(q);
                addQuiz(q);
                return q.toFormatString();
            }
            //修改题干/标题
            if(keyword.matches("^(题干|题目|提供|提供者|9|0)\\s+\\S+")){
                String[] splitArr = keyword.split("\\s+");
                DailyQuiz q = ss.getRecentDailyQuiz();
                if(splitArr[1].equals("清空")){
                    splitArr[1] = "";
                }
                if(splitArr[0].equals("题干") || splitArr[0].equals("题目") || splitArr[0].equals("0")){
                    q.setBody(splitArr[1]);
                }else{
                    q.setProvider(splitArr[1]);
                }
                ss.setRecentDailyQuiz(q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //清空指定编号的题
            if(keyword.matches("^((清空|删除)\\s+[Qq]?[1-9][0-9]{0,3})|([Qq]?[1-9][0-9]{0,3}\\s+(清空|删除))$")){
                String[] splitArr = keyword.split("\\s+");
                if(splitArr[1].equals("清空") || splitArr[1].equals("删除") ){
                    splitArr[1] = splitArr[0];
                }
                String label = splitArr[1].replace('q','Q');
                //即便不存在,也返回一个清空后的题目
                DailyQuiz q = new DailyQuiz();
                q.setLabel(label);
                ss.setRecentDailyQuiz(q);
                addQuiz(q);
                return q.toFormatString();
            }
            //给指定选项 清空 或者 添加广告标志
            if(keyword.matches("^(清空|删除)\\s+(1|2|3|4|5|一|二|三|四|五)$")){
                String[] splitArr = keyword.split("\\s+");
                int choiceIdx = getOptionNumberInCommand(splitArr[1]);
                DailyQuiz q = ss.getRecentDailyQuiz();
                q.setOption(choiceIdx,splitArr[0]);
                ss.setRecentDailyQuiz(q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            throw new MpException(ErrorCodeEnum.UNHANDLED);
        }

        if(!keyword.matches("^[Qq][1-9][0-9]{0,3}")){
            throw new MpException(ErrorCodeEnum.UNHANDLED);
        }
        keyword = keyword.replace('q','Q');
        if(quizMap.containsKey(keyword)){
            DailyQuiz q = quizMap.get(keyword);
            ss.setRecentDailyQuiz(q);
            return q.toFormatString();
        }else if(user.isRoot()) {
            //管理员查询时,若题目不存在,则直接创建.
            DailyQuiz q = new DailyQuiz();
            q.setLabel(keyword);
            addQuiz(q);
            ss.setRecentDailyQuiz(q);
            return q.toFormatString();
        }else{
            ss.setRecentDailyQuiz(null);
            return String.format("暂未收录%s,请在反馈群或者茶馆评论区反馈!",keyword);
        }
    }

    public void addQuiz(DailyQuiz q){
        //只有符合格式的标签(编号)才会被添加
        if(q.getIndex()==null)
            throw new MpException(ErrorCodeEnum.QUIZ_WRONG_INDEX);
        quizMap.put(q.getLabel(),q);
        redisUtils.hset(quizDBKey,q.getLabel(),q.toJson());
    }

    public void delQuiz(DailyQuiz q){
        try {
            if (q!=null && quizMap.containsKey(q.getLabel())) {
                quizMap.remove(q.getLabel());
                redisUtils.hdel(quizDBKey, q.getLabel());
                //System.out.println("删除%s成功");
            }
        }catch(Exception e){
            throw new MpException(ErrorCodeEnum.QUIZ_DELETE_ERROR);
        }
    }

    public Map<String,DailyQuiz> getDailyQuizMap(){
        return this.quizMap;
    }

}
