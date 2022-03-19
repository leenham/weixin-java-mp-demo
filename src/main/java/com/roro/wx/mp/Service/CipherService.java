package com.roro.wx.mp.Service;


import com.alibaba.fastjson.JSON;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.DateUtils;
import com.roro.wx.mp.utils.RedisUtils;
import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.roro.wx.mp.utils.FileUtils;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理暗号图相关的请求,主要分两部分:
 * 1. 只发送图片,视作查询请求
 * 2. 发送图片后,在指定时间间隔内发送答案,视作更新暗号池请求
 */
@Service
@Slf4j
public class CipherService {
    @Value("${roconfig.cipher.latency}")
    private String latency;

    @Value("${roconfig.cipher.pool}")
    private String cipherPoolKey;

    @Value("${link.jump.cipher}")
    private String authCipherLink;

    @Value("${roconfig.cipher.commit}")
    private String cipherCommitKey;

    @Value("${roconfig.cipher.links}")
    private String cipherlinks;

    @Autowired
    RedisUtils redisUtils;


    public String handleCipherAnswer(String text){
        //符合"答案:四字成语"格式的,被认为是合法输入
        if(text.matches("答案[\\s,:.]+.*")){
            String[] arr = text.split("[\\s,:.：]+",2);
            if(arr.length!=2 || arr[1].length()<3 || arr[1].length()>5){
                throw new MpException(ErrorCodeEnum.CIPHER_ILLEGAL_ANSWER);
            }
            String answer = arr[1];
            return answer;
        }else{
            //不是合法的答案输入,不予处理
            return null;
        }
    }
    public void addCipherRecord(Cipher cipher, String answer){
        try {
            redisUtils.hset(cipherPoolKey, JSON.toJSONString(cipher.getFeature()), answer);
        }catch (Exception e){
            throw new MpException(ErrorCodeEnum.FAIL_UPDATE_CIPHER_POOL);
        }
    }
    /**
     * 将url,转化成cipher格式. 如果转换失败，返回null
     * @param url
     * @return
     */
    public Cipher url2Cipher(String url){
        BufferedImage image;
        String link = "";
        try {
            String filename = "cipher/"+String.valueOf(DateUtils.now().getTime())+".jpg";
            File file = FileUtils.saveUrlAs(url,filename);
            image = ImageIO.read(file);
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);//解码
            link = result.getText();
            if(link.equals(authCipherLink)){
                //从指定位置各取10*10像素作为特征
                int w = image.getWidth();
                int h = image.getHeight();
                int[] arr = new int[100];
                for(int i=0;i<10;i++){
                    for(int j=0;j<10;j++){
                        arr[i*10+j] = image.getRGB(10+i*(w-20)/10,10+j*(h-20)/10);
                    }
                }
                return new Cipher(url,arr);
            }else{
                throw new MpException(ErrorCodeEnum.CIPHER_WRONG_QRCODE);
            }
        } catch (IOException e) {
            //e.printStackTrace();
            throw new MpException(ErrorCodeEnum.CIPHER_ILLEGAL_PIC);
        } catch (NotFoundException e) {
            //e.printStackTrace();
            throw new MpException(ErrorCodeEnum.CIPHER_WITHOUT_QRCODE);
        } catch (Exception e){
            throw new MpException(ErrorCodeEnum.CIPHER_UNKNOWN_ERROR);
        }
    }

    /**
     * 根据提供的cipher特征，检索取回结果; 如果检索不到，那么同样将其加入暗号池，并返回空字符串
     * 检索时需提供用户信息，用于记录其检索行为，在其输入答案时有用。
     * @param cipher
     * @return
     */
    public String retrieval(User user, Cipher cipher)throws MpException{
        try {
            String ret = "";
            String cipher_str = JSON.toJSONString(cipher.getFeature());
            if (redisUtils.hexists(cipherPoolKey, cipher_str)) {
                ret = (String) redisUtils.hget(cipherPoolKey, cipher_str);
            } else {
                redisUtils.add(cipherlinks, cipher.getUrl());
            }
            redisUtils.hset(cipherCommitKey, JSON.toJSONString(user), JSON.toJSONString(cipher.getFeature()));
            return ret;
        }catch (Exception e){
            throw new MpException(ErrorCodeEnum.CIPHER_RETRIVAL_ERROR);
        }
    }

    /**
     * 获取user最近一次提交的暗号图，如果没有返回null
     * 每次查询/更新,都会更新提交表（hash表，key为appID+ID,value为cipher
     * @param user
     * @return
     */
    public Cipher getRecentCommit(User user){
        Date now = DateUtils.now();
        /*if(now.getTime()-user.getLastvisit()>Long.valueOf(latency)){
            throw new MpException(ErrorCodeEnum.NO_RECENT_CIPHER);
        }*/
        String key = JSON.toJSONString(user);
        if(redisUtils.hexists(cipherCommitKey,key)){
            int[] f = JSON.parseObject((String)redisUtils.hget(cipherCommitKey,key),int[].class);
            return new Cipher("",f);
        }else{
            throw new MpException(ErrorCodeEnum.NO_COMMIT_CIPHER);
        }
    }
}