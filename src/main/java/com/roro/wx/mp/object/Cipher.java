package com.roro.wx.mp.object;

import lombok.Data;


/**
 * Cipher类,英雄杀游戏里的暗号图对象
 */
@Data
public class Cipher{
    private String url;
    private int[] feature; // 取局部图片的像素值作为特征
    public Cipher(String _url,int[] _feature){
        this.url = _url;
        this.feature = _feature;
    }

    @Override
    public int hashCode(){
        int hashval = 0;
        for(int i=0;i<feature.length;i++){
            hashval ^= feature[i];
        }
        return hashval;
    }
    @Override
    public boolean equals(Object obj){
        if(obj==null)return false;
        if(this ==obj )return true;
        if(obj.getClass()!=this.getClass()) return false;
        Cipher c = (Cipher) obj;
        if(this.feature.length!=c.feature.length){
            return false;
        }
        for(int i=0;i<this.feature.length;i++){
            if(this.feature[i]!=c.feature[i]){
                return false;
            }
        }
        return true;
    }

}
