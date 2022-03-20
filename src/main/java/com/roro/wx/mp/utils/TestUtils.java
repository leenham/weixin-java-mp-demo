package com.roro.wx.mp.utils;

import com.roro.wx.mp.object.Cipher;

import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * 为了老是写重复的测试用的代码,将这些代码统统放这里,方便调用.
 */
public class TestUtils {
    public static void compareFeatures(int[] f1,int[] f2){
        int maxdiff = 0;
        int totaldiff = 0;
        int total1 = 0,total2 = 0;
        for(int i=0;i<f1.length;i++){
            if(f1[i]==0 && f2[i]==0)
                continue;
            System.out.println(String.format("f1[%d]=%d f2[%d]=%d diff=%d",i,f1[i],i,f2[i],Math.abs(f1[i]-f2[i])));
            //System.out.println(String.format("diff:%.2f",Math.abs(f1[i]-f2[i])/(f1[i]+0.0)*100.0));
            maxdiff = Math.max(maxdiff,Math.abs(f1[i]-f2[i]));
            totaldiff += Math.abs(f1[i]-f2[i]);
            total1+=f1[i];
            total2+=f2[i];
        }
        System.out.println(String.format("maxdiff:%d totaldiff:%d",maxdiff,totaldiff));
        System.out.println(String.format("total1:%d,total2:%d",total1,total2));
    }
    public static void compareImage(BufferedImage img1,BufferedImage img2){
        if(img1.getWidth()!=img2.getWidth() || img1.getHeight()!=img2.getHeight()){
            System.out.println("两张图像尺寸不同,无法对比!");
        }
        for(int i=0;i<img1.getWidth();i++){
            for(int j=0;j<img1.getHeight();j++){
                int p1 = img1.getRGB(i,j);
                int r1 = (p1>>16) & 255;
                int g1 = (p1>>8) & 255;
                int b1 = p1 & 255;
                int p2 = img2.getRGB(i,j);
                int r2 = (p2>>16) & 255;
                int g2 = (p2>>8) & 255;
                int b2 = p2 & 255;
                System.out.println(String.format("[%d,%d]|(%d,%d,%d),(%d,%d,%d)",i,j,r1,g1,b1,r2,g2,b2));
            }
        }
        System.out.println(String.format("IMAGE:%d X %d",img1.getWidth(),img1.getHeight()));
    }
    public static void checkDatabase(HashMap<String,Cipher> map){
        for(String key:map.keySet()){
            System.out.println(String.format("%s:%s",key,map.get(key).getUrl()));
        }
    }
}
