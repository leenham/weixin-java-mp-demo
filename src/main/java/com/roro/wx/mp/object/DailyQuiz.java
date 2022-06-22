package com.roro.wx.mp.object;

import com.roro.wx.mp.utils.JsonUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DailyQuiz {
    @Data
    public static class Option{
        public String choice;
        public String result;
        public Option(){
            this.choice = "";
            this.result = "";
        }
        public Option(String _choice,String _answer){
            this.choice = _choice;
            this.result = _answer;
        }
    }
    String body;      //题干
    String label;    //题目编号/标签
    String provider; //本题由谁谁谁提供
    List<DailyQuiz.Option> optionList;
    public DailyQuiz(){
        this.optionList = new ArrayList<>();
        this.body = "";
        this.provider = "";
        this.label = "";
    }
    public void setLabel(Integer idx){
        if(idx!=null){
            label = String.format("Q%d",idx);
            return ;
        }
    }
    public void setLabel(String _label){
        this.label = _label;
    }
    public String toFormatString(){
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("(%s)",this.getLabel()));
        if(provider!=null && !provider.equals(""))
            sb.append(String.format("(本题由%s提供)",this.provider));
        sb.append(this.getBody()+'\n');
        List<DailyQuiz.Option> optionList = this.getOptionList();
        for(int i=0;i<optionList.size();i++) {
            DailyQuiz.Option option = this.getOptionList().get(i);
            if (option.getResult().equals("true")) {
                sb.append("[√] " + option.getChoice() + '\n');
            } else if (option.getResult().equals("false")){
                sb.append("[×] " + option.getChoice() + '\n');
            }else{
                sb.append("[ ] " + option.getChoice() + '\n');
            }
        }
        return sb.toString();
    }

    public Integer getIndex(){
        if(this.label.matches("^[Qq][1-9]{1,4}$")){
            return Integer.valueOf(label.substring(1,label.length()));
        }else{
            return null;
        }
    }

    public String toJson(){
        return JsonUtils.toJson(this);
    }
    public static DailyQuiz fromJson(String json){
        return (DailyQuiz)JsonUtils.fromJson(json,DailyQuiz.class);
    }

    public void setOption(int idx,String content){
        while (idx >= optionList.size()) {
            this.getOptionList().add(new DailyQuiz.Option());
        }
        DailyQuiz.Option option = optionList.get(idx);
        if(content.equals("清空") || content.equals("删除")){
            option.setResult("");
            option.setChoice("");
            return;
        }else if(content.matches("^([Tt]rue|[Tt]|对|正确)$")) {
            option.setResult("true");
            for(int i=0;i<optionList.size();i++){
                if(optionList.get(i).getResult().equals("") && i!=idx)
                    optionList.get(i).setResult("false");
            }
        }else if(content.matches("^([Ff]alse|[Ff]|错|错误)$")) {
            option.setResult("false");
        }else{
            option.setChoice(content);
        }
        return;
    }

}
