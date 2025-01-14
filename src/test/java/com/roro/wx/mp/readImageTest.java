package com.roro.wx.mp;


import com.alibaba.fastjson.JSON;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.roro.wx.mp.Service.CipherService;
import com.roro.wx.mp.Service.UserService;
import com.roro.wx.mp.object.Cipher;
import com.roro.wx.mp.object.User;
import com.roro.wx.mp.utils.ImageUtils;
import com.roro.wx.mp.utils.JsonUtils;
import com.roro.wx.mp.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class readImageTest {

    @Autowired
    private CipherService cipherService;
    @Autowired
    private UserService userService;
    @Test
    public void myTest() throws IOException {
        User user = new User("abcd","1234");
        User user2 = new User("roro","5678");
        String url = "http://mmbiz.qpic.cn/mmbiz_jpg/3iaSGNqIeD0chtgBj9VuVv8bUNKwI3pMo4k2P2pXLcia8JVJuxhOPNIO8vFcDHiczXFGgKOV2K6kjm7Llgeic1b1nA/0";
        String url2 = "http://mmbiz.qpic.cn/mmbiz_jpg/ryzvh3l1zlhTvvgUKjbjFJnvNd8iaJvpmt4qlc3NIz86HqJWibibF7Tuu7XKDl8YYia8nAne9hzXuicJDnDCYicpUo3w/0";
        /*BufferedImage image1 = ImageIO.read(new URL(url));
        BufferedImage image2 = ImageIO.read(new URL(url2));*/
        BufferedImage image1 = ImageIO.read(new File("image1.jpg"));
        BufferedImage image2 = ImageIO.read(new File("image2.jpg"));
        int[] h1 = new int[256];
        int[] h2 = new int[256];
        System.out.println(String.format("图片1,宽度:%d,高度:%d\n 图片2,宽度:%d,高度:%d",image1.getWidth(),image1.getHeight(),image2.getWidth(),image2.getHeight()));
        int cnt = 0;
        for(int i=0;i<image1.getWidth();i++){
            for(int j=0;j<image1.getHeight();j++){
                int p1 = image1.getRGB(i,j);
                int p2 = image2.getRGB(i,j);
                int gray1 = (int)(((p1>>16)&255) * 0.3 + ((p1>>8)&255) * 0.59 + (p1&255) * 0.11);
                int gray2 = (int)(((p2>>16)&255) * 0.3 + ((p2>>8)&255) * 0.59 + (p2&255) * 0.11);
                h1[gray1]++;h2[gray2]++;
            }
        }
        int diff = 0;
        int total = 0;
        int[] diff_arr = new int[16];
        for(int i=0;i<16;i+=16){
            int sum1 = 0,sum2 = 0;
            for(int j=0;j<16;j++){
                sum1 += h1[i*16+j];
                sum2 += h2[i*16+j];
            }
            diff += Math.abs(sum1-sum2);
            diff_arr[i] = sum1-sum2;
            total += sum2;
        }
        System.out.println(String.format("直方图误差为%d / %d",diff,total));
        System.out.println(JSON.toJSONString(diff_arr));
    }

    @Test
    public void testNewFeature() throws IOException {
        BufferedImage img1 = ImageIO.read(new File("tianluodiwang1.jpg"));
        BufferedImage img2 = ImageIO.read(new File("tianluodiwang2.jpg"));
        int[] f1 = ImageUtils.getFeatures(img1);
        int[] f2 = ImageUtils.getFeatures(img2);
        TestUtils.compareFeatures(f1,f2);
        //TestUtils.compareImage(img1,img2);
        //ImageUtils.write(img1,"png","img1.png");
        //ImageUtils.write(img2,"png","img2.png");

    }
    @Test
    public void truncate() throws  IOException{
        int head = 50;
        int tail = 150;
        BufferedImage img1 = ImageIO.read(new File("jibuzeshi.jpg"));
        int w = img1.getWidth();
        int h = img1.getHeight();
        BufferedImage img2 = ImageUtils.truncate(img1,head,0,tail,100);
        //img2 = ImageUtils.resize(img2,32,32);
        ImageIO.write(img2,"png",new File("output.png"));
    }

    @Test
    public void ciphertest() throws IOException {
        //BufferedImage image = ImageIO.read(new File("xiaoqiao.png"));
        //System.out.println(ImageUtils.parseQRCode(image));
    }

    @Test
    public void checkCHP() throws IOException {
        HashMap<String,Cipher> map = cipherService.getCipherTable();
        //TestUtils.checkDatabase(map);
        int idx = 0;
        for(String key:map.keySet()){
            BufferedImage img = ImageIO.read(new URL(map.get(key).getUrl()));
            ImageUtils.write(img,"jpg",String.format("cipher/%s.jpg",key));
            //System.out.println(String.format("Load image:...%d",++idx));
        }
    }

    @Test
    public void deleteCipherMap(){
        //删除特定的键值
        HashMap<String,Cipher> map = cipherService.getCipherTable();
        System.out.println(String.format("当前暗号池大小:%d",map.size()));
        cipherService.deleteCipherRecord("人定胜天");
        cipherService.deleteCipherRecord("人定胜天 ");
        map = cipherService.getCipherTable();
        System.out.println(String.format("当前暗号池大小:%d",map.size()));
        return;
    }


}
