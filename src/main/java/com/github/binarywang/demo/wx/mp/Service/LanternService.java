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
 * 用于处理英雄杀元宵节活动提问的service
 */
@Slf4j
@Component
public class LanternService {
    @Value(value="${link.jump.yingxiongsha}")
    public String jumplink;

    //返回结果超过这个值,会导致响应过慢,而无法响应.已知7条时会不予响应.
    public int MAX_RETURN_RESULT = 6;
    public String retrieval(String keyword){
        //由于没有数据库，将日志库当作备份数据库使用，输入指令进行备份
        if(keyword.equals("输出数据库wxec9fbb925a874ef4")){
            output2Log();
            String content = "已成功更新日志\n";
            return content;
        }
        //以#号开头，四位数组作为索引查询题库中的记录并返回
        if(keyword.length()==5 && keyword.matches("#[0-9]{4}")){
            int eventIdx = Integer.parseInt(keyword.substring(1,5));
            return getEventByIdx(eventIdx);
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
            return content;
        }

        // 否则，默认交给lanternService进行默认检索
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
    private void output2Log(){
        List<String> eventlist = LanternUtils.getEventList();
        log.info("===日志库中备份题库===\n");
        for(int i=0;i<eventlist.size();i++){
            log.info(eventlist.get(i));
        }
    }

}
