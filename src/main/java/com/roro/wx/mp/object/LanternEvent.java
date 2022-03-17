package com.roro.wx.mp.object;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class LanternEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    String event;
    String label;

    List<Option> options;//列表第一个是选项，后续为结果
    public void addOption(String s1){
        Option option = new Option();
        option.add(s1);
        options.add(option);
    }
    public void addOption(String s1,String s2){
        Option option = new Option();
        option.add(s1);
        option.add(s2);
        options.add(option);
    }
    public int getLabel(){
        if(label.length()==5 && label.charAt(0)=='#'){
            return Integer.parseInt(label.substring(1,5));
        }
        return 9999;
    }
    public void addOption(String s1,String s2,String s3){
        Option option = new Option();
        option.add(s1);
        option.add(s2);
        option.add(s3);
        options.add(option);
    }
    public void addOption(String s1,String s2,String s3,String s4){
        Option option = new Option();
        option.add(s1);
        option.add(s2);
        option.add(s3);
        option.add(s4);
        options.add(option);
    }
    public LanternEvent(){
        options = new ArrayList<>();
        event = "";
        label = "";
    }
}
