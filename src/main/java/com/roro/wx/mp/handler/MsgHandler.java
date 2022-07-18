package com.roro.wx.mp.handler;

import com.roro.wx.mp.Service.CipherService;
import com.roro.wx.mp.Service.DailyQuizService;
import com.roro.wx.mp.Service.QuizService;
import com.roro.wx.mp.Service.UserService;
import com.roro.wx.mp.builder.TextBuilder;
import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.*;

import com.roro.wx.mp.utils.AuthUtils;
import com.roro.wx.mp.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType;

/**
 * @author Binary Wang(https://github.com/binarywang)
 */
@Slf4j
@Component
public class MsgHandler extends AbstractHandler {
    @Autowired
    UserService userService;
    @Autowired
    CipherService cipherService;
    @Autowired
    QuizService quizService;
    @Autowired
    DailyQuizService dailyQuizService;

    @Value("${roconfig.appId}")
    String roAppId;

    Map<String, Session> sessionMap = new HashMap<>();

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) {

        if (!wxMessage.getMsgType().equals(XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }

        String msgType = wxMessage.getMsgType();
        String reply = "";
        try{
            User user = userService.getUser(wxMessage.getToUser(),wxMessage.getFromUser());
            Session session = sessionMap.getOrDefault(user.getKey(),new Session(user));

            if(user.inBlackList()){
                //拒绝让黑名单用户访问
                throw new MpException(ErrorCodeEnum.DENY_BLACKLIST_USER);
            }
            if(msgType.equals("text")){
                session.setMsg(wxMessage.getContent());
                reply = handleText(session);
            }else if(msgType.equals("image")){
                //对于图片格式的输入,关键信息在于getPicUrl()
                session.setMsg(wxMessage.getPicUrl());
                reply = handleImage(session);
            }else{}

            session.setNow();
            sessionMap.put(session.getKey(),session);

            if(reply.equals("")){
                //此处返回值为null,在上层代码会进行判断,并最终返回空字符串(即不做任何回复
                //如果直接返回content为""的结果,公众号会显示故障.
                return null;
            }else {
                return new TextBuilder().build(reply, wxMessage, weixinService);
            }
        }catch(MpException e){
            return new TextBuilder().build(String.format("%s",e.getErrorMsg()), wxMessage, weixinService);
        }catch(Exception e){
            return new TextBuilder().build("系统发生未知故障.", wxMessage, weixinService);
        }
    }

    //处理文本类的消息
    private String handleText(Session ss){
        String keyword = ss.getMsg().trim();//去除首尾的空格
        User user = ss.getUser();
        String reply = handleSpecialCommand(ss);
        if(reply!=null && !reply.equals("")) {
            //如果指令被受理,直接返回指令结果.
            return reply;
        }
        try {
            reply = dailyQuizService.retrieval(ss);
            if(reply!=null && !reply.equals("")) {
                return reply;
            }
        }catch(MpException me){
            if(me.getErrorCode()==ErrorCodeEnum.UNHANDLED.getCode() || me.getErrorCode()==ErrorCodeEnum.NO_RECENT_COMMIT_QUIZ.getCode()){
                //表示每日答题没有捕获该指令,属于预料中的报错
            }else{
                throw me; //意料之外的错误继续向上抛
            }
        }
        //否则交给活动答题去检索.
        reply = quizService.retrieval(ss);
        return reply;
        //活动期间可以考虑对Q123格式的查询,返回每日答题结果;而其他格式,则不调用dailyquizService,单独调用quizService
        /*LocalDateTime now = LocalDateTime.now();
        if(AuthUtils.isRoot(user) || now.getMonth().compareTo(Month.MAY)>0){
            reply = quizService.retrieval(user,keyword);
        }else{
            reply = "";//自动接入的闲聊系统会再回复,此处不要回复即可.
        }*/
        //return reply;
    }
    /**
     * @desc 处理一些特殊的指令
     * @return 返回空字符串表示没能处理，否则表示指令受理后的反馈
     */
    private String handleSpecialCommand(Session ss){
        String keyword = ss.getMsg();
        User user = ss.getUser();
        //优先处理超级管理员(也就是我自己)的指令
        if(user.isSuperRoot()){
            if(keyword.equals("#刷新")){
                if(!AuthUtils.isSuperRoot(user.getAuthCode())){
                    throw new MpException(ErrorCodeEnum.NO_AUTH);
                }
                userService.init();
                cipherService.init();
                quizService.init();
                dailyQuizService.init();
                return "刷新成功";
            }
            //"授权 appID ID authCode" 给指定用户赋予管理员权限,该权限可用于提交在线修改指令
            if(keyword.matches("^授权\\s+\\S+\\s+[0-9]+$")) {
                String[] arr = keyword.split("\\s+");
                String appID = roAppId;
                String ID = arr[1];
                int authCode = Integer.valueOf(arr[2]);
                if (!userService.hasUser(appID, ID)) {
                    throw new MpException(ErrorCodeEnum.USER_UNEXISTED);
                } else {
                    User target = userService.getUser(appID, ID);
                    userService.authorize(target, authCode);
                    return String.format("给ID:%s授权<%d>成功.", target.getID(), target.getAuthDesc());
                }
            }
            if(keyword.matches("^活动答题\\s+\\S+$")){
                String[] arr = keyword.split("\\s+");
                if(arr[1].equals("开启") || arr[1].equals("开始")){
                    quizService.activate();
                    return "答题活动已开启";
                }else if(arr[1].equals("关闭") || arr[1].equals("结束")){
                    quizService.deactivate();
                    return "答题活动已关闭";
                }else if(arr[1].matches("[a-zA-z]+")){
                    quizService.setQuizDBKey(arr[1]);
                    return String.format("数据库主键已设置为%s,当前状态:%s,共有题目%d条",arr[1],quizService.isActive()?"开启":"关闭",quizService.getQuizMap().size());
                }else{
                    return "指令格式错误,活动答题主键应使用英文字母";
                }
            }
            if(keyword.matches("显示跳转链接")){
                quizService.showJumpLink();
                return "已设置为显示跳转链接";
            }
            if(keyword.matches("隐藏跳转链接")){
                quizService.hideJumpLink();
                return "已设置为不显示跳转链接";
            }
        }
        /* 允许他们自行查看自己的appID和ID,这样方便我将他们和具体的人对应起来*/
        if(keyword.equals("#查看信息")){
            String authDesc = AuthUtils.getAuthDesc(user.getAuthCode());
            return String.format("appID:%s\nID:%s\nauthCode:%s",user.getAppID(),user.getID(),authDesc);
        }
        //处理 答案:四字成语 格式的输入,视作更新暗号图的答案,管理员才能修改
        if(keyword.matches("答案[\\s,:.]+.*")){
            if(!user.isRoot()) {
                throw new MpException(ErrorCodeEnum.NO_AUTH);
            }
            if(ss.getRecentCipher()==null){
                throw new MpException(ErrorCodeEnum.NO_COMMIT_CIPHER);
            }
            String[] arr = keyword.split("[\\s,:.：]+",2);
            if(arr.length!=2 || arr[1].length()<2 || arr[1].length()>=6){
                throw new MpException(ErrorCodeEnum.CIPHER_ILLEGAL_ANSWER);
            }
            String answer = arr[1];
            //读取该用户最近一次提交的暗号图，并更新暗号池
            Cipher cipher = ss.getRecentCipher();
            cipherService.addCipherRecord(cipher,answer);
            //更新的同时,将图片下载保存到服务器
            try {
                ImageUtils.write(ImageUtils.read(cipher.getUrl()),String.format("cipher/%s.jpg",answer));
            }catch(Exception e){
                throw new MpException(ErrorCodeEnum.FAIL_ADD_NEW_CIPHER);
            }
            return String.format("已成功更新暗号池：%s",answer);
        }
        //处理添加新题目的请求
        if(keyword.matches("^(添题|新题|加题|添加新题目|[Nn]ew|[Aa]dd)$")){
            if(!user.isRoot()){
                throw new MpException(ErrorCodeEnum.NO_AUTH);
            }
            Quiz q = new Quiz();
            q.setLabel("#9999");
            quizService.addQuiz(q);
            ss.setRecentQuiz(q);//添加新题目时,默认将其作为最近一次提交,方便直接修改
            return q.toFormatString();
        }
        return "";//返回空字符串表示没能处理。
    }

    //处理图片类的消息
    private String handleImage(Session session){
        try {
            return handleImageAsCipher(session);
        }catch (MpException me){
            //提交图片过程中发生异常,则清空最近一次提交,防止用户无意间修改了答案
            session.setRecentCipher(null);
            //cipherService.clearRecentCommit(user);
            if(me.getErrorCode()== ErrorCodeEnum.CIPHER_ILLEGAL_PIC.getCode() ||
                me.getErrorCode()==ErrorCodeEnum.CIPHER_WITHOUT_QRCODE.getCode() ||
                me.getErrorCode()==ErrorCodeEnum.CIPHER_WRONG_QRCODE.getCode()
            ){
                //说明不是暗号图,那么应该当做普通图片处理
                //当前为了调试功能,继续向上抛. 如果后续加了其他图片处理功能,考虑修改这个地方
                throw me;
            }else {
                throw me;//否则继续向上抛异常
            }
        }
        /* TODO:其他处理图片输入的逻辑 */
    }
    //默认将图片消息当做暗号图处理.
    private String handleImageAsCipher(Session session) {
        String result = cipherService.retrieval(session);
        if(result==null || result.equals("")){
            //表示无法检索到暗号图
            return "当前暗号池暂未收录该暗号图!";
        }else {
            return result;
        }
    }

}
