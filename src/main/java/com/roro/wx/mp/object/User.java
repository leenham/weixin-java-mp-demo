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
    public final static int NAIVE = 0;
    public final static int SUPERROOT = 1;
    public final static int ROOT = 1<<1;
    public final static int BLACKLIST = 1<<2;
    private String ID;    //每个用户,对于每个公众号(appID),会有唯一的openID
    private String appID; //因此appID + ID 可以锁定唯一的用户
    private String name;  //用户昵称
    private Integer status; //模拟会话,用于判断用户当前的状态.
    private int authCode;   //标识当前用户权限，默认是0000
    //private long lastvisit; //记录用户上次提交信息的时间
    public User(String _appID,String _ID){
        this.appID = _appID;
        this.ID = _ID;
        this.status = 0;
        this.name = "";
    }
    public String getKey(){
        return appID+ID;
    }
    public String toTestString(){
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("app:%s\nid:%s\nauth:%x",appID,ID,authCode));
        return sb.toString();
    }
    public boolean isRoot(){
        return (authCode & ROOT)>0 || (authCode & SUPERROOT)>0;
    }
    public boolean isSuperRoot(){
        return (authCode & SUPERROOT)>0;
    }
    public boolean inBlackList(){
        return (authCode & BLACKLIST)>0;
    }
    public String getAuthDesc(){
        StringBuilder sb = new StringBuilder();
        if(isSuperRoot()){
            sb.append("|超管");
        }
        if(isRoot()){
            sb.append("|管理员");
        }
        if(inBlackList()){
            sb.append("|黑名单");
        }
        if(!sb.equals("")){
            sb.append("|");
            return sb.toString();
        }else {
            return "普通用户";
        }
    }
}
