package com.roro.wx.mp.handler;

import com.roro.wx.mp.Service.CipherService;
import com.roro.wx.mp.Service.QuizService;
import com.roro.wx.mp.Service.UserService;
import com.roro.wx.mp.builder.TextBuilder;
import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.User;

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

import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
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
    @Value("${roconfig.appId}")
    String roAppId;

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
            if(AuthUtils.isBlackList(user.getAuthCode())){
                //拒绝让黑名单用户访问
                throw new MpException(ErrorCodeEnum.DENY_BLACKLIST_USER);
            }
            if(msgType.equals("text")){
                reply = handleText(user,wxMessage);
            }else if(msgType.equals("image")){
                reply = handleImage(user,wxMessage);
            }
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
    private String handleText(User user,WxMpXmlMessage wxMessage){
        String keyword = wxMessage.getContent().trim();//去除首尾的空格
        try{
            return handleSpecialCommand(user,keyword);
        }catch(MpException e){
            //说明未能处理该消息,应该交给之后的其他功能去处理,否则就向外抛
            if(e.getErrorCode()!=ErrorCodeEnum.UNHANDLED.getCode()){
                throw e;//如果已经受理但是还是报错,那么就把这个错误向外抛.
            }
        }
        String reply = "";
        //否则当做答题检索功能处理.
        //reply = quizService.retrieval(user,keyword);
        /*活动下线,不再处理检索相关功能;仅第一次输入通知活动下线,之后不再提醒.
        * 已将数据库中当前所有用户的status刷为1,新用户默认status是0
        * 只有当前数据库中的用户会受到活动下线提示.
        * */
        LocalDateTime now = LocalDateTime.now();
        if(AuthUtils.isRoot(user) || now.getMonth().compareTo(Month.MAY)>0){
            reply = quizService.retrieval(user,keyword);
        }else{
            reply = "";//自动接入的闲聊系统会再回复,此处不要回复即可.
        }
        return reply;
    }
    //处理一些特殊的指令
    private String handleSpecialCommand(User user,String keyword){
        /* 允许他们自行查看自己的appID和ID,这样方便我将他们和具体的人对应起来*/
        if(keyword.equals("#查看信息")){
            String authDesc = AuthUtils.getAuthDesc(user.getAuthCode());
            return String.format("appID:%s\nID:%s\nauthCode:%s",user.getAppID(),user.getID(),authDesc);
        }
        /* 方便我在线将内存中的表和数据库进行同步*/
        if(keyword.equals("#刷新")){
            if(!AuthUtils.isSuperRoot(user.getAuthCode())){
                throw new MpException(ErrorCodeEnum.NO_AUTH);
            }
            userService.init();
            cipherService.init();
            quizService.init();
            return "刷新成功";
        }
        //"授权 appID ID authCode" 给指定用户赋予管理员权限,该权限可用于提交在线修改指令
        if(keyword.matches("^授权\\s+\\S+\\s+[0-9]+$")){
            //只有超级管理员,也就是我自己才能随意赋权. 对于授权码的格式,就不做限制了,我自己知道怎么攻击自己(ૢ˃ꌂ˂ૢ)
            if((user.getAuthCode() & AuthUtils.SUPERROOT)==0)
                throw new MpException(ErrorCodeEnum.NO_AUTH);
            try {
                String[] arr = keyword.split("\\s+");
                String appID = roAppId;
                String ID = arr[1];
                int authCode = Integer.valueOf(arr[2]);
                if(!userService.hasUser(appID,ID)){
                    throw new MpException(ErrorCodeEnum.USER_UNEXISTED);
                }else{
                    User target = userService.getUser(appID,ID);
                    userService.authorize(target,authCode);
                    return String.format("给ID:%s授权%d成功.",target.getID(),authCode);
                }
            }catch(MpException me){
                throw me;
            }
            catch (Exception e){
                throw new MpException(ErrorCodeEnum.SPECIAL_COMMAND_ERROR);
            }
        }
        //处理 答案:四字成语 格式的输入,视作更新暗号图的答案,管理员才能修改
        if(keyword.matches("答案[\\s,:.]+.*")){
            if(!AuthUtils.isRoot(user.getAuthCode())) {
                throw new MpException(ErrorCodeEnum.NO_AUTH);
            }
            String[] arr = keyword.split("[\\s,:.：]+",2);
            if(arr.length!=2 || arr[1].length()<2 || arr[1].length()>=6){
                throw new MpException(ErrorCodeEnum.CIPHER_ILLEGAL_ANSWER);
            }
            String answer = arr[1];
            //读取该用户最近一次提交的暗号图，并更新暗号池
            Cipher cipher = cipherService.getRecentCommit(user);
            cipherService.addCipherRecord(cipher,answer);
            //更新的同时,将图片保存到服务器
            try {
                ImageUtils.write(ImageUtils.read(cipher.getUrl()),String.format("cipher/%s.jpg",answer));
            }catch(Exception e){
                throw new MpException(ErrorCodeEnum.FAIL_ADD_NEW_CIPHER);
            }
            return String.format("已成功更新暗号池：%s",answer);
        }
        //默认会报未处理该消息的异常
        throw new MpException(ErrorCodeEnum.UNHANDLED);
    }
    //处理图片类的消息

    private String handleImage(User user,WxMpXmlMessage wxMessage){
        try {
            return handleImageAsCipher(user,wxMessage);
        }catch (MpException me){
            //提交图片过程中发生异常,则清空最近一次提交,防止用户无意间修改了答案
            cipherService.clearRecentCommit(user);
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
    private String handleImageAsCipher(User user,WxMpXmlMessage wxMessage) {
        Cipher c = cipherService.url2Cipher(wxMessage.getPicUrl());
        String result = cipherService.retrieval(user,c);
        if(result==null || result.equals("")){
            //表示无法检索到暗号图
            return "当前暗号池暂未收录该暗号图!";
        }else {
            return result;
        }
    }

}
