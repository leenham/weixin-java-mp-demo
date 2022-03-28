package com.roro.wx.mp.handler;

import com.roro.wx.mp.Service.CipherService;
import com.roro.wx.mp.Service.QuizService;
import com.roro.wx.mp.Service.UserService;
import com.roro.wx.mp.builder.TextBuilder;
import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.Service.LanternService;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType;

/**
 * @author Binary Wang(https://github.com/binarywang)
 */
@Slf4j
@Component
public class MsgHandler extends AbstractHandler {
    @Autowired
    LanternService lanternService;
    @Autowired
    UserService userService;
    @Autowired
    CipherService cipherService;
    @Autowired
    QuizService quizService;

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
            if(msgType.equals("text")){
                reply = handleText(wxMessage);
            }else if(msgType.equals("image")){
                reply = handleImage(wxMessage);
            }
        }catch(MpException e){
            return new TextBuilder().build(String.format("%s(错误码:%d)",e.getErrorMsg(),e.getErrorCode()), wxMessage, weixinService);
        }catch(Exception e){
            return new TextBuilder().build("发生未知故障.", wxMessage, weixinService);
        }
        return new TextBuilder().build(reply, wxMessage, weixinService);
    }


    private String handleText(WxMpXmlMessage wxMessage){
        String keyword = wxMessage.getContent();
        //处理文本类的消息
        User user = userService.getUser(wxMessage.getToUser(),wxMessage.getFromUser());
        String result = cipherService.checkCipherAnswer(keyword);
        if(result!=null && !result.equals("")){
            //输入如果符合暗号答案格式，就读取该用户最近一次提交的暗号图，并更新暗号池
            Cipher cipher = cipherService.getRecentCommit(user);
            cipherService.addCipherRecord(cipher,result);
            return String.format("已成功更新暗号池：%s",result);
        }
        //否则当做答题检索功能处理.
        String reply = quizService.retrieval(user,keyword);
        return reply;
    }

    //处理图片类的消息
    private String handleImage(WxMpXmlMessage wxMessage){
        try {
            return handleImageAsCipher(wxMessage);
        }catch (MpException me){
            User user = userService.getUser(wxMessage.getToUser(),wxMessage.getFromUser());
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
    private String handleImageAsCipher(WxMpXmlMessage wxMessage) {
        User user = userService.getUser(wxMessage.getToUser(),wxMessage.getFromUser());
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
