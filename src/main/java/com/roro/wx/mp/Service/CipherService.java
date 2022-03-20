package com.roro.wx.mp.Service;


import com.alibaba.fastjson.JSON;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.*;
import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    @Value("${roconfig.cipher.table}")
    private String cipherTableKey;

    @Value("${link.jump.cipher}")
    private String authCipherLink;

    @Value("${roconfig.cipher.commit}")
    private String cipherCommitKey;

    @Value("${roconfig.cipher.links}")
    private String cipherlinks;

    @Value("${roconfig.cipher.threshold}")
    private String thresholdStr;

    private int threshold;

    HashMap<String,Cipher> cipherTable;
    HashMap<String,Cipher> commitTable;

    @Autowired
    RedisUtils redisUtils;

    @PostConstruct
    public void init(){
        cipherTable = new HashMap<>();
        commitTable = new HashMap<>();
        threshold = Integer.valueOf(thresholdStr);
        try{
            Set<Object> keyset = redisUtils.hkeys(cipherTableKey);
            for(Object key:keyset){
                Cipher value = JsonUtils.json2Cipher((String)redisUtils.hget(cipherTableKey,key));
                cipherTable.put((String)key,value);
            }
            return;
        }catch(Exception e){
            log.error("从Redis中读取暗号池时出错.");
        }
    }
    public String checkCipherAnswer(String text){
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
            cipherTable.put(answer,cipher);
            redisUtils.hset(cipherTableKey, answer,JsonUtils.cipher2Json(cipher));
        }catch (Exception e){
            log.error(String.format("添加暗号:%s(%s)时出错",answer,JsonUtils.cipher2Json(cipher)));
            throw new MpException(ErrorCodeEnum.FAIL_UPDATE_CIPHER_POOL);
        }
    }
    /**
     * 将url,转化成cipher格式. 如果转换失败，返回null
     * @param url
     * @return
     * 处理一张图片大概要900+ms
     */
    public Cipher url2Cipher(String url){
        BufferedImage image;
        String link = "";
        try {
            //String filename = "cipher/"+String.valueOf(DateUtils.now().getTime())+".png";
            //File file = FileUtils.saveUrlAs(url,filename);
            image = ImageIO.read(new URL(url));
            BufferedImage qrcodeRegion = ImageUtils.truncate(image,image.getHeight()-150,150,0,150);
            LuminanceSource source = new BufferedImageLuminanceSource(qrcodeRegion);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);//解码
            link = result.getText();
            if(link.equals(authCipherLink)){
                //截去头部50像素和底端二维码部分,提取压缩成16*16像素的rgb值作为特征;
                int[] f = ImageUtils.getFeatures(image);
                return new Cipher(url,f);
            }else{
                throw new MpException(ErrorCodeEnum.CIPHER_WRONG_QRCODE);
            }
        } catch (IOException e) {
            //e.printStackTrace();
            throw new MpException(ErrorCodeEnum.CIPHER_ILLEGAL_PIC);
        } catch (NotFoundException e) {
            //e.printStackTrace();
            throw new MpException(ErrorCodeEnum.CIPHER_WITHOUT_QRCODE);
        } catch (MpException e){
            throw e;
        } catch (Exception e){
            throw new MpException(ErrorCodeEnum.CIPHER_UNKNOWN_ERROR);
        }
    }

    /**
     * 根据提供的cipher特征，检索取回结果; 无论是否检索得到，都将其记录为该用户最近提交
     * 检索时需提供用户信息，用于记录其检索行为，等用户输入答案时有用。
     * @param cipher
     * @return
     */
    public String retrieval(User user, Cipher cipher)throws MpException{
        try {
            String answer = "";
            int[] target = cipher.getFeature();
            OUTER:
            for(String key:cipherTable.keySet()){
                int[] origin = cipherTable.get(key).getFeature();
                int diffCnt = 0;
                for(int i=0;i<origin.length;i++){
                    if(Math.abs(origin[i]-target[i])>threshold){
                        diffCnt++;
                        if(diffCnt>=3)
                            continue  OUTER;
                    }
                }
                answer = key;
                break OUTER;
            }
            commitTable.put(user.getKey(),cipher);
            return answer;
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
        String key = user.getKey();
        if(commitTable.containsKey(key)){
            return commitTable.get(key);
        }else{
            throw new MpException(ErrorCodeEnum.NO_COMMIT_CIPHER);
        }
    }

}
