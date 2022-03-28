package com.roro.wx.mp.utils;

import com.roro.wx.mp.object.User;

/**
 * 用于权限认证,32个权限位应该够用了
 * bit位置    对应权限
 * 0     ---  超级管理员
 * 1     ---  管理员
 * 2     ---  是否是黑名单用户
 * 3     ---  ...
 */

public class AuthUtils {
    public final static int SUPERROOT = 1;
    public final static int ROOT = 1<<1;
    public final static int BLACKLIST = 1<<2;

    public static boolean isRoot(int authCode){
        return (authCode & ROOT)>0 || (authCode & SUPERROOT)>0;
    }
    public static boolean isSuperRoot(int authCode){
        return (authCode & SUPERROOT)>0;
    }
    public static boolean isBlackList(int authCode){
        return (authCode & BLACKLIST)>0;
    }

}
