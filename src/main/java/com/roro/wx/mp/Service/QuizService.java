package com.roro.wx.mp.Service;

import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.Quiz;
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

    @Value(value="${roconfig.quiz.emptyDB}")
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
            log.error("从Redis中读取答题活动题库时出错.");
        }
    }

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
            if(keyword.matches("^(添题|新题|加题|添加新题目|[Nn]ew|[Aa]dd)$")){
                if(!AuthUtils.isRoot(user.getAuthCode())){
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                //优先寻找题库中有没有没任何内容的编号,给其分配旧的标签
                /*for(String key:quizMap.keySet()){
                    Quiz quiz = quizMap.get(key);
                    if(quiz.getBody().equals("") && quiz.getTitle().equals("") && quiz.getOptionList().size()==0){
                        recentCommit.put(user.getKey(),quiz);//添加新题目时,默认将其作为最近一次提交,方便直接修改
                        return quiz.toFormatString();
                    }
                }*/
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
                if(!recentCommit.containsKey(user.getKey()) || recentCommit.get(user.getKey())==null){
                    throw new MpException(ErrorCodeEnum.NO_RECENT_COMMIT_QUIZ);
                }
                String[] splitArr = keyword.split("\\s+");
                int choiceIdx = getOptionNumberInCommand(splitArr[0]);
                Quiz q = recentCommit.get(user.getKey());
                if(splitArr.length==2){
                    //选项一 内容 ==> 如果对应的选项为空,则覆盖选项;否则,给该选项添加新结果
                    q.setOption(choiceIdx,splitArr[1]);
                }else if(splitArr.length==3){
                    //选项一 内容1 内容2 ==> 内容1覆盖选项,内容2覆盖结果
                    q.setOption(choiceIdx,splitArr[1],splitArr[2]);
                }else{
                    throw new MpException(ErrorCodeEnum.QUIZ_WRONG_COMMAND);
                }
                recentCommit.put(user.getKey(),q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //修改题干/标题
            if(keyword.matches("^(题干|标题|tg|bt|0|9)\\s+\\S+")){
                if(!AuthUtils.isRoot(user.getAuthCode())) {
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                String[] splitArr = keyword.split("\\s+");
                Quiz q = recentCommit.get(user.getKey());
                if(splitArr[1].equals("清空")){
                    splitArr[1] = "";
                }
                if(splitArr[0].equals("题干") || splitArr[0].equals("tg") || splitArr[0].equals("0")){
                    q.setBody(splitArr[1]);
                }else{
                    q.setTitle(splitArr[1]);
                }
                recentCommit.put(user.getKey(),q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            //清空指定编号的题
            if(keyword.matches("^((清空|删除)\\s+[0-9]{4})|([0-9]{4}\\s+(清空|删除))$")){
                if(!AuthUtils.isRoot(user.getAuthCode())) {
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                String[] splitArr = keyword.split("\\s+");
                if(splitArr[1].equals("清空") || splitArr[1].equals("删除") ){
                    splitArr[1] = splitArr[0];
                }
                String label = '#'+splitArr[1];
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
            //给指定选项 清空 或者 添加广告标志
            if(keyword.matches("^(清空|删除|广告)\\s+(1|2|3|4|5|一|二|三|四|五)$")){
                if(!AuthUtils.isRoot(user.getAuthCode())) {
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                if(!recentCommit.containsKey(user.getKey()) || recentCommit.get(user.getKey())==null){
                    throw new MpException(ErrorCodeEnum.NO_RECENT_COMMIT_QUIZ);
                }
                String[] splitArr = keyword.split("\\s+");
                int choiceIdx = getOptionNumberInCommand(splitArr[1]);
                Quiz q = recentCommit.get(user.getKey());
                q.setOption(choiceIdx,splitArr[0]);
                recentCommit.put(user.getKey(),q);//将更新后的quiz提交并更新
                addQuiz(q);
                return q.toFormatString();
            }
            throw new MpException(ErrorCodeEnum.UNHANDLED);
        }catch(MpException me){
            //指令没有处理的，则顺序往下执行。否则向上抛出。
            if(me.getErrorCode()!=ErrorCodeEnum.UNHANDLED.getCode()){
                throw me;
            }
        }
        //如果输入中间带空格，会被视作指令异常
        if(keyword.contains(" ")){
            throw new MpException(ErrorCodeEnum.QUIZ_WRONG_COMMAND);
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
            recentCommit.put(user.getKey(),null);
            return replyWhenNoFound(user,keyword);
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

    //当找不到检索结果时，随机回复一条很皮的消息
    public String replyWhenNoFound(User user,String keyword){
        String name = user.getName();
        Random r = new Random();
        if(name==null || name.equals("")){
            if(keyword.length()>=7){
                String[] reply_arr = new String[]{
                    "简单点，搜索的方式再短点~",
                    "关键词太长了，唉呀妈呀脑瓜疼！"
                };
                return reply_arr[r.nextInt(reply_arr.length)];
            }else{
                String[] reply_arr = new String[]{
                    "臣妾搜不到啊!",
                    "这道题，我不会做!",
                    "这可真是难为小生了，可否换个关键词。",
                    "为妾不懂，官人再换个关键词吧~",
                    "唉呀妈呀脑瓜疼!",
                    "这题不会，另请高明吧!",
                    "别闹，这个词搜不到",
                    "对方答不上来并向你请求换个词",
                    "对方已放弃思考，不换个词回复不了",
                    "搜索失败，大侠换个关键词重新来过",
                };
                return reply_arr[r.nextInt(reply_arr.length)];
            }
        }else {
            String[] reply_arr = new String[]{
                "搜不到，%s。别问了，再问自杀。",
                "%s,你已经是个成熟的管理员了，你应该学会自己换关键词了！",
                "%s,你老让我搜不到，这让我很难办啊！",
                "%s,臣妾搜不到啊!",
                "%s,这道题，我不会做！不！会！做！",
                "为妾不懂，%s大官人，再换个关键词吧~",
                "唉呀妈呀，%s,我脑瓜疼!",
                "别闹了，%s，这个词搜不到",
                "%s,能不能不要皮？"
            };
            return String.format(reply_arr[r.nextInt(reply_arr.length)],name);
        }

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
}
