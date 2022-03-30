package com.roro.wx.mp.object;

import com.roro.wx.mp.utils.JsonUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Quiz {
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
    String title;    //小标题
    List<Option> optionList;

    public Quiz(){
        this.optionList = new ArrayList<>();
        this.body = "";
        this.title = "";
    }

    public String toJsonString(){
        return JsonUtils.toJson(this);
    }

    //将quiz以便于阅读的形式输出
    public String toFormatString(){
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("【%04d.%s】\n",this.getIndex(),this.getTitle()));
        sb.append("$$ "+this.getBody()+'\n');
        List<Quiz.Option> optionList = this.getOptionList();
        for(int i=0;i<optionList.size();i++){
            Quiz.Option option = this.getOptionList().get(i);
            sb.append("> "+option.getChoice()+' '+option.getResult()+'\n');
        }
        return sb.toString();
    }
    //测试时用的输出格式
    public String toTestString(){
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("[%s]%04d. ",this.getTitle(),this.getIndex()));
        sb.append(this.getBody());sb.append('\n');
        List<Quiz.Option> optionList = this.getOptionList();
        for(int i=0;i<optionList.size();i++){
            Quiz.Option option = this.getOptionList().get(i);
            sb.append('-'+option.getChoice()+' '+option.getResult()+'\n');
        }
        return sb.toString();
    }
    //从标签中读取label
    public Integer getIndex(){
        try {
            return label==null||label.length()!=5?null:Integer.valueOf(label.substring(1,5));
        }catch(Exception e){
            return null;
        }
    }

    public void setLabel(Integer idx){
        if(idx!=null){
            label = String.format("#%04d",idx);
            return ;
        }
    }
    public void setLabel(String _label){
        this.label = _label;
    }
}
