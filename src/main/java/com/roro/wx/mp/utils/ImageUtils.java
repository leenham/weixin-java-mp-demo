package com.roro.wx.mp.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.roro.wx.mp.enums.ErrorCodeEnum;
import com.roro.wx.mp.enums.MpException;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageUtils {
    public static BufferedImage resize(BufferedImage originalImage, int targetWidth, int targetHeight) {
        try {
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = resizedImage.createGraphics();
            graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            graphics2D.dispose();
            return resizedImage;
        }catch(Exception e){
            throw new MpException(ErrorCodeEnum.UNABLE_RESOLVE_CIPHER);
        }
    }
    public static int[] getRGBFeatures(BufferedImage img){
        int w = img.getWidth();
        int h = img.getHeight();
        int fpc = 4; // fpc = features per channel
        int dividee = 256 / fpc;
        int[] ret = new int[fpc*fpc*fpc];//3通道,每个通道取featurePerChannel个直方图

        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                int p = img.getRGB(i,j);
                int r = (p>>16)&255;
                int g = (p>>8)&255;
                int b = p & 255;
                r = r/dividee;
                g = g/dividee;
                b = b/dividee;
                ret[r*fpc*fpc + g*fpc + b]++;
            }
        }
        return ret;
    }
    public static int[] getGrayFeatures(BufferedImage img){
        int w = img.getWidth();
        int h = img.getHeight();
        int fpc = 256; //灰度图上取fpc个特征
        int[] ret = new int[fpc];

        for(int i=0;i<w;i++){
            for(int j=0;j<h;j++){
                int p = img.getRGB(i,j);
                int r = (p>>16)&255;
                int g = (p>>8)&255;
                int b = p & 255;
                int gray = (r*19595+g*38469+b*7472)>>16;
                ret[gray/(256/fpc)]++;
            }
        }
        return ret;
    }
    public static int[] getFeatures(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int resizew = 16;
        int resizeh = 16;
        BufferedImage tmp = ImageUtils.resize(ImageUtils.truncate(img,50,0,150,0),resizew,resizeh);
        int[] ret = new int[resizew*resizeh*3];
        for(int i=0;i<resizew;i++){
            for(int j=0;j<resizeh;j++){
                int p = tmp.getRGB(i,j);
                int r = (p>>16)&255;
                int g = (p>>8)&255;
                int b = p & 255;
                ret[i*j*3+0] = r;
                ret[i*j*3+1] = g;
                ret[i*j*3+2] = b;
            }
        }
        return ret;
    }
    public static void write(BufferedImage img, String format, String filename) throws IOException {
        ImageIO.write(img,format,new File(filename));
    }
    public static void write(BufferedImage img) throws IOException {
        String defaultFormat = "png";
        String defaultFilename = "output.png";
        ImageIO.write(img,defaultFilename,new File(defaultFilename));
    }
    public static BufferedImage truncate(BufferedImage img, int top,int right,int bottom,int left){
        return img.getSubimage(left,top,img.getWidth()-left-right,img.getHeight()-top-bottom);
    }
    public static String parseQRCode(BufferedImage image){
        try {
            BufferedImage qrcodeRegion = ImageUtils.truncate(image, image.getHeight() - 150, 200, 0, 200);
            LuminanceSource source = new BufferedImageLuminanceSource(qrcodeRegion);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);//解码
            String link = result.getText();
            return link;
        }catch(NotFoundException e){
            return "读取图片错误";
        }catch(Exception e){
            return "解析二维码错误";
        }
    }
}
