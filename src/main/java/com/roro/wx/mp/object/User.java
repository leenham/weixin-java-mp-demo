package com.roro.wx.mp.object;

import lombok.Data;

import java.util.Date;

/**
 * User 类
 * 将公众号面向的用户抽象成User类
 *
 */
@Data
public class User {
    private String ID;    //每个用户,对于每个公众号(appID),会有唯一的openID
    private String appID; //因此appID + ID 可以锁定唯一的用户
    private String name;  //用户昵称
    private Integer status; //模拟会话,用于判断用户当前的状态.
    private int authCode;   //标识当前用户权限，默认是0000
    //private long lastvisit; //记录用户上次提交信息的时间
    public User(String _appID,String _ID){
        this.appID = _appID;
        this.ID = _ID;
    }
    public String getKey(){
        return appID+ID;
    }
    public String toTestString(){
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("app:%s | %s",appID,ID));
        return sb.toString();
    }

}
