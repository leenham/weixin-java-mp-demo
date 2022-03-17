package com.roro.wxmp.object;


import java.util.ArrayList;
import java.util.List;

public class Option{
    List<String> option;
    public Option(){
        option = new ArrayList<>();
    }
    public void add(String str){
        option.add(str);
    }
    @Override
    public String toString(){
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<option.size();i++){
            if(i==option.size()-1){
                sb.append(option.get(i));
            }else {
                sb.append(option.get(i) + "/");
            }
        }
        return sb.toString();
    }
}
