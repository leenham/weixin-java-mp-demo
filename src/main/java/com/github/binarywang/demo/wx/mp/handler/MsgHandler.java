package com.github.binarywang.demo.wx.mp.handler;

import com.alibaba.fastjson.JSON;
import com.github.binarywang.demo.wx.mp.Service.CipherService;
import com.github.binarywang.demo.wx.mp.Service.LanternService;
import com.github.binarywang.demo.wx.mp.Service.UserService;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.object.Cipher;
import com.github.binarywang.demo.wx.mp.object.LanternEvent;
import com.github.binarywang.demo.wx.mp.object.Option;
import com.github.binarywang.demo.wx.mp.object.User;
import com.github.binarywang.demo.wx.mp.utils.DateUtils;
import com.github.binarywang.demo.wx.mp.utils.JsonUtils;
import com.github.binarywang.demo.wx.mp.utils.LanternUtils;
import java.util.*;

import com.github.binarywang.demo.wx.mp.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) {

        if (!wxMessage.getMsgType().equals(XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }
        User user = userService.getUser(wxMessage.getToUser(),wxMessage.getFromUser());
        String reply = "";
        //处理文本类的消息
        if(wxMessage.getMsgType().equals("text")) {
            String keyword = wxMessage.getContent();
            String result = cipherService.handleCipherAnswer(keyword);
            if(result!=""){
                //如果符合暗号答案格式，就读取该用户最近一次提交的暗号图，更新暗号池
                Cipher cipher = cipherService.getRecentCommit(user);
                if(cipher==null){
                    reply = "请先提交暗号图再发送答案！";
                }else {
                    cipherService.addCipherRecord(cipher,result);
                    reply = String.format("已成功更新暗号：%s",result);
                }
            }else {
                reply = "答案格式不对";
            }
            return new TextBuilder().build(reply, wxMessage, weixinService);
        }

        //处理图片类的消息
        if(wxMessage.getMsgType().equals("image")){
            Cipher c = cipherService.url2Cipher(wxMessage.getPicUrl());
            String result = cipherService.retrieval(user,c);
            if(result==null || result.equals("")){
                reply = "当前暗号池暂未收录该题目";
            }else{
                reply = result;
            }
            //reply += wxMessage.getPicUrl();
            //reply += String.format("特征:%s\n", JsonUtils.toJson(c.getFeature()));
            //reply += String.format("链接:%s\n",c.getUrl());
            return new TextBuilder().build(reply, wxMessage, weixinService);
        }

        //String reply = keyword+"copy that!\n";
        //System.out.println(wxMessage.getPicUrl());

        return new TextBuilder().build(reply, wxMessage, weixinService);
    }

}
