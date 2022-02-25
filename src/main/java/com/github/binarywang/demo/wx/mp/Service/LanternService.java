package com.github.binarywang.demo.wx.mp.Service;

import com.github.binarywang.demo.wx.mp.builder.TextBuilder;
import com.github.binarywang.demo.wx.mp.object.LanternEvent;
import com.github.binarywang.demo.wx.mp.utils.LanternUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: roro
 * 用于处理英雄杀元宵节提问
 */
@Slf4j
@Component
public class LanternService {
    @Value(value="${link.jump.yingxiongsha}")
    public String jumplink;

    //返回结果超过这个值,会导致响应过慢,而无法响应.已知7条时会不予响应.
    public int MAX_RETURN_RESULT = 6;
    public String retrieval(String keyword){
        List<String> eventlist = LanternUtils.getEventList();
        List<LanternEvent> selected = new ArrayList<>();
        for(int i=0;i<eventlist.size();i++){
            String eventstr = eventlist.get(i);
            if(eventstr.contains(keyword)){
                selected.add(LanternUtils.toLanternEvent(eventstr));
            }
        }
        if(selected.size()>=6 || selected.size()<=0) {
            String content = "";
            if(selected.size()<=0){
                if(keyword.length()>=5){
                    content = "请尝试用更短的词语进行检索~\n";
                }else{
                    content = "请尝试更换关键词!或协助完善题库~\n";
                }
            }else {
                content = "检索到的条目过多,请尝试更换关键词!\n";
            }
            return content;
        }
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<selected.size();i++){
            LanternEvent e = selected.get(i);
            sb.append("### "+String.format("%04d",e.getLabel())+"."+e.getEvent()+"\n");
            for(int j=0;j<e.getOptions().size();j++){
                sb.append("\t> ");
                sb.append(e.getOptions().get(j).toString()+"\n");
            }
            sb.append("\n");
        }
        sb.append(jumplink);
        return sb.toString();
    }
    public String getEventByIdx(int idx){
        List<String> eventlist = LanternUtils.getEventList();
        if(idx<eventlist.size()){
            return eventlist.get(idx);
        }else{
            return String.format("无法检索到 #%04d 条记录~",idx);
        }
    }
    public void output2Log(){
        List<String> eventlist = LanternUtils.getEventList();
        log.info("===日志库中备份题库===\n");
        for(int i=0;i<eventlist.size();i++){
            log.info(eventlist.get(i));
        }
    }
}
