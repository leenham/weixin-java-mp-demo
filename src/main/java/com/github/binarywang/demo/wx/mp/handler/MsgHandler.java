package com.github.binarywang.demo.wx.mp.handler;

import com.github.binarywang.demo.wx.mp.Service.LanternService;
import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.object.LanternEvent;
import com.github.binarywang.demo.wx.mp.object.Option;
import com.github.binarywang.demo.wx.mp.utils.JsonUtils;
import com.github.binarywang.demo.wx.mp.utils.LanternUtils;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) {

        if (!wxMessage.getMsgType().equals(XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }

        String keyword = wxMessage.getContent();
        //由于没有数据库，将日志库当作备份数据库使用，输入指令进行备份
        if(keyword.equals("输出数据库wxec9fbb925a874ef4")){
            lanternService.output2Log();
            String content = "已成功更新日志\n";
            return new TextBuilder().build(content, wxMessage, weixinService);
        }
        //以#号开头，四位数组作为索引查询题库中的记录并返回
        if(keyword.length()==5 && keyword.matches("#[0-9]{4}")){
            int eventIdx = Integer.parseInt(keyword.substring(1,5));
            return new TextBuilder().build(lanternService.getEventByIdx(eventIdx), wxMessage, weixinService);
        }
        //以Json格式的输入，视作更新数据库记录。
        if(keyword.startsWith("{\"event\":") && keyword.matches(".*#[0-9]{4}.*")){
            String content = "";
            try{
                //如果这个语句能顺利执行，说明输入是合法的
                LanternEvent e = LanternUtils.toLanternEvent(keyword);
                boolean msg = LanternUtils.addNewLanternEvent(e);
                content = msg?String.format("已成功更新一条记录"):String.format("格式错误或者标签不为#9999");
            }catch (Exception e2){
                content = String.format("更新记录异常，请检查输入格式");
            }
            return new TextBuilder().build(content, wxMessage, weixinService);
        }

        // 否则，默认交给lanternService进行检索
        String reply = lanternService.retrieval(keyword);
        return new TextBuilder().build(reply, wxMessage, weixinService);

    }

}
