package com.roro.wx.mp.Service;

import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.Quiz;
import com.roro.wx.mp.object.Session;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.AuthUtils;
import com.roro.wx.mp.utils.JsonUtils;
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

    //@Value(value="${roconfig.quiz.emptyDB}")
    public String quizDBKey;  //存储当前题库的哈希表所对应的键值

    @Value(value="${roconfig.quiz.configKey}")
    public String configKey;

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    UserService userService;

    int LIMITSIZE = 5;
    Map<String,Quiz> quizMap;  //题库在内存中的备份,每次部署会从数据库中读取.
    class QuizServiceConfig{
        public boolean isActive; //活动是否
        public boolean hasJumpLink; // 是否添加跳转链接？
        public String quizDBKey; //活动题库对应的键.
        List<String> autoReply;


        public QuizServiceConfig(){
            this.isActive = true;
            this.hasJumpLink = true;
            this.quizDBKey = "emptyDB";
            this.autoReply = new ArrayList<>();
        }
    }
    public QuizServiceConfig config;

    @PostConstruct
    public void init(){
        quizMap = new HashMap<>();
        Object obj= redisUtils.get(configKey);
        if(obj==null)
            config = new QuizServiceConfig();
        else
            config = (QuizServiceConfig)JsonUtils.fromJson((String)obj,QuizServiceConfig.class);
        this.quizDBKey = config.quizDBKey;
        try {
            Set<Object> keyset = redisUtils.hkeys(quizDBKey);
            for (Object key : keyset) {
                String quizText = (String) (redisUtils.hget(quizDBKey, key));
                quizMap.put((String) key, JsonUtils.json2Quiz(quizText));
            }
        }catch(Exception e) {
            log.error("不存在的题库键值%s".format(quizDBKey));
        }

    }

    public boolean setQuizDBKey(String dbkey){
        config.quizDBKey = dbkey;
        saveConfig();
        init();
        return true;
    }
    public void activate(){
        if(!config.isActive) {
            config.isActive = true;
            saveConfig();
        }
    }
    public void deactivate(){
        if(config.isActive){
            config.isActive = false;
            saveConfig();
        }
    }
    public boolean isActive(){
        return config.isActive;
    }
    public void showJumpLink(){
        config.hasJumpLink = true;
    }
    public void hideJumpLink(){
        config.hasJumpLink = false;
    }
    /**
     *
     * @param ss
     * @return
     * 用来 检索 / 修改 /更新 / 添加 题目
     * 用户信息主要用来记录并判断是否有权限能够修改题目
     */
    public String retrieval(Session ss){
        if(!this.isActive()){
            return "";
        }
        String keyword = ss.getMsg();
        User user = ss.getUser();
        //通过是否包含空格来区分是否是指令,(TODO : 判断逻辑待修改).
        if(keyword.contains(" ")){
            //权限控制
            if(!user.isRoot()) {
                throw new MpException(ErrorCodeEnum.NO_AUTH);
            }
            //如果找不到最近一次提交的结果,则直接驳回.(过期视作不存在)
            if(ss.getRecentQuiz()==null){
                throw new MpException(ErrorCodeEnum.NO_RECENT_COMMIT_QUIZ);
            }

            //修改/更新/添加 选项
            if(keyword.matches("^(选项)?(1|2|3|4|5|一|二|三|四|五)(\\s+\\S+){1,2}")){

                String[] splitArr = keyword.split("\\s+");
                int choiceIdx = getOptionNumberInCommand(splitArr[0]);
                Quiz q = ss.getRecentQuiz();
                if(splitArr.length==2){
                    //选项一 内容 ==> 如果对应的选项为空,则覆盖选项;否则,给该选项添加新结果
                    q.setOption(choiceIdx,splitArr[1]);
                }else if(splitArr.length==3){
                    //选项一 内容1 内容2 ==> 内容1覆盖选项,内容2覆盖结果
                    q.setOption(choiceIdx,splitArr[1],splitArr[2]);
                }else{
                    throw new MpException(ErrorCodeEnum.QUIZ_WRONG_COMMAND);
                }
                ss.setRecentQuiz(q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //修改题干/标题
            if(keyword.matches("^(题干|标题|tg|bt|0|9)\\s+\\S+")){
                String[] splitArr = keyword.split("\\s+");
                Quiz q = ss.getRecentQuiz();
                if(splitArr[1].equals("清空")){
                    splitArr[1] = "";
                }
                if(splitArr[0].equals("题干") || splitArr[0].equals("tg") || splitArr[0].equals("0")){
                    q.setBody(splitArr[1]);
                }else{
                    q.setTitle(splitArr[1]);
                }
                ss.setRecentQuiz(q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //清空指定编号的题
            if(keyword.matches("^((清空|删除)\\s+[0-9]{4})|([0-9]{4}\\s+(清空|删除))$")){
                String[] splitArr = keyword.split("\\s+");
                if(splitArr[1].equals("清空") || splitArr[1].equals("删除") ){
                    splitArr[1] = splitArr[0];
                }
                String label = '#'+splitArr[1];
                if(quizMap.containsKey(label)){
                    Quiz q = new Quiz();
                    q.setLabel(label);
                    ss.setRecentQuiz(q);
                    addQuiz(q);
                    return q.toFormatString();
                }else{
                    return String.format("编号%s 不存在",label);
                }
            }
            //给指定选项 清空 或者 添加广告标志
            if(keyword.matches("^(清空|删除|广告)\\s+(1|2|3|4|5|一|二|三|四|五)$")){
                String[] splitArr = keyword.split("\\s+");
                int choiceIdx = getOptionNumberInCommand(splitArr[1]);
                Quiz q = ss.getRecentQuiz();
                q.setOption(choiceIdx,splitArr[0]);
                ss.setRecentQuiz(q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //添加随机回复 TODO: 删除回复的功能
            if(keyword.matches("^(回复)\\s+\\S$")){
                String[] splitArr = keyword.split("\\s+");
                config.autoReply.add(splitArr[1]);
                saveConfig();
            }
            throw new MpException(ErrorCodeEnum.UNHANDLED);
        }

        //否则遍历题库,寻找匹配项
        StringBuffer sb = new StringBuffer();
        List<Quiz> selected = new ArrayList<>();
        for(String key:quizMap.keySet()){
            Quiz target = quizMap.get(key);
            if(target.toJsonString().contains(keyword)){
                //将标题匹配的放在前面
                if(target.getTitle().contains(keyword)){
                    selected.add(0,target);
                }else{
                    selected.add(target);
                }
            }
        }
        if(selected.size()>LIMITSIZE){
            sb.append(String.format("共检索到%d条记录,仅返回前%d条记录,若搜不到请更换关键词~\n",selected.size(),LIMITSIZE));
        }else if(selected.size()==0){
            ss.setRecentQuiz(null);
            return replyWhenNoFound(user,keyword);
        }
        if(selected.size()==1){
            ss.setRecentQuiz(selected.get(0));
        }else{
            ss.setRecentQuiz(null);
        }
        //包装返回结果
        for(int i=0;i<Math.min(LIMITSIZE,selected.size());i++){
            Quiz qz = selected.get(i);
            sb.append(qz.toFormatString());
        }
        if(config.hasJumpLink)
            sb.append(jumplink);//最后添加跳转回游戏的链接
        return sb.toString();
    }

    //当找不到检索结果时，随机回复一条很皮的消息
    public String replyWhenNoFound(User user,String keyword){
        String name = user.getName();
        Random r = new Random();
        List<String> replylist = config.autoReply;
        if(replylist==null)
            replylist = new ArrayList<>();
        if(replylist.size()==0)
            replylist.add("找不到匹配的题目,请尝试换个检索词吧~");
        String reply = replylist.get(r.nextInt(replylist.size()));
        if(user.getName()!=null && !user.getName().equals("")){
            reply = user.getName()+','+reply;
        }
        return reply;
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

    public void delQuiz(Quiz q){
        try {
            if (quizMap.containsKey(q.getLabel())) {
                quizMap.remove(q.getLabel());
                redisUtils.hdel(quizDBKey, q.getLabel());
                //System.out.println("删除%s成功");
            }
        }catch(Exception e){
            throw new MpException(ErrorCodeEnum.QUIZ_DELETE_ERROR);
        }
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


    public void saveConfig(){
        redisUtils.set(configKey,JsonUtils.toJson(config));
    }
}
