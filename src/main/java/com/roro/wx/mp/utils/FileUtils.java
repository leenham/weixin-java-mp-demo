package com.roro.wx.mp.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 *  代码源于https://www.cnblogs.com/henuyuxiang/p/11608936.html
 *  这种方式下载得到的图片和网站上直接下载的一样,
 *  而用ImageIO.read()方法读取到的图片会被严重压缩,且带有随机性.(每次压缩的结果不尽相同)
 */

public class FileUtils {
    public static File saveUrlAs(String url, String filePath){
        String method = "GET";//默认用get方式获取图片
        //创建不同的文件夹目录
        File file=new File(filePath);
        FileOutputStream fileOut = null;
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        try
        {
            // 建立链接
            URL httpUrl=new URL(url);
            conn=(HttpURLConnection) httpUrl.openConnection();
            //以Post方式提交表单，默认get方式
            conn.setRequestMethod(method);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // post方式不能使用缓存
            conn.setUseCaches(false);
            //连接指定的资源
            conn.connect();
            //获取网络输入流
            inputStream=conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            //判断文件的保存路径后面是否以/结尾
            if (!filePath.endsWith("/")) {
                filePath += "/";
            }
            //写入到文件（注意文件保存路径的后面一定要加上文件的名称）
            fileOut = new FileOutputStream(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(fileOut);

            byte[] buf = new byte[4096];
            int length = bis.read(buf);
            //保存文件
            while(length != -1)
            {
                bos.write(buf, 0, length);
                length = bis.read(buf);
            }
            bos.close();
            bis.close();
            conn.disconnect();
        } catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("抛出异常！！");
        }

        return file;
    }
}
