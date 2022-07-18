package com.roro.wx.mp.object;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class Session
{
    String key;
    User user;
    DailyQuiz recentDailyQuiz;
    Quiz recentQuiz;
    Cipher recentCipher;
    LocalDateTime startTime; //记录会话诞生的时间
    String msg;
    public Session(User user){
        this.key = user.getKey();
        this.user = user;
        this.recentDailyQuiz = null;
        this.recentQuiz = null;
        this.recentCipher = null;
        this.startTime = LocalDateTime.now();
        this.msg = "";
    }
    //将会话时间设定到当前
    public void setNow(){
        this.startTime = LocalDateTime.now();
    }
    //判断会话是否过期,以20分钟为期
    public boolean isExpired(){
        LocalDateTime nowtime = LocalDateTime.now();
        Duration duration = Duration.between(startTime,nowtime);
        return duration.toMinutes()>=20L;
    }

    public void setRecentDailyQuiz(DailyQuiz q){
        this.recentDailyQuiz = q;
        this.recentQuiz = null; //避免每日答题和活动答题相互冲突
    }

    public void setRecentQuiz(Quiz q){
        this.recentQuiz = q;
        this.recentDailyQuiz = null; //避免每日答题和活动答题相互冲突
    }
    public DailyQuiz getRecentDailyQuiz(){
        return isExpired()?null:recentDailyQuiz;
    }
    public Quiz getRecentQuiz(){
        return isExpired()?null:recentQuiz;
    }
    public Cipher getRecentCipher(){
        return isExpired()?null:recentCipher;
    }
}
