package com.example.tank.plantprotectionrobot.DataProcessing;

/**
 * Created by Think on 2018/1/14.
 */

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * Created by Think on 2018/1/14.
 */

public class SDCardFileTool {

    private Context context;
    public SDCardFileTool(Context context) {
        this.context = context;
    }

    /***
     *
     * @param fileName
     * @return
     */
    public static boolean createDataFiles(String fileName)
    {
        if(!getSDCardBaseDir().isEmpty()){
            if(!isFolderExists(fileName)){
                return false;
            }
        }else{
            return false;
        }
        return  true;
    }

    /***
     *
     * @param fileName
     * @return
     */
    public static boolean isFolderExists(String fileName)
    {
        String fileDir = getSDCardBaseDir() +  File.separator + fileName;
        File file = new File(fileDir);
        if (!file.exists())
        {
            if (file.mkdirs())
            {
                Log.e("tankdebug",fileDir+" create success1");
                return true;
            }
            else {

                Log.e("tankdebug",fileDir+" create fail1");
                return false;
            }
        }

        return true;
    }


    /***
     *  按照指定长度读取文件
     * @param buf 读数据缓存空间，
     * @param readFlag true 读取指定字节数 false 全部读取
     * @param len  读数据长度
     * @param openfile 文件名（含路径）Tank/用户名/mapping/
     * @return 读数据后偏移地址
     */
    public static boolean readFileFromSDCard(byte[] buf,boolean readFlag,int len,String openfile){

        //获取根目录
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        String folderPath = "";
        if (sdCardExist) {
            // 获取文件路径
            folderPath = Environment.getExternalStorageDirectory()
                    + File.separator + openfile;

        }
        //为了确保文件一定在之前是存在的，将字符串路径封装成File对象
        File file = new File(folderPath);
        if(!file.exists()){
            return false;
        }
        //声明输入流引用，字节输入常用类FileInputStream
        FileInputStream fis = null;
        try
        {
            //生成输入流的对象
           fis = new FileInputStream(folderPath);
            //将文件中的数据读进程序
            if(readFlag){ //读取指定长度
                for(int i=0;i<len;i++) {
                    buf[i] = (byte)fis.read();
                }
            }else { //读取全部文件
                while (fis.read(buf, 0, len) != -1) ;
            }

        } catch(Exception e)
        {
            System.out.println(e);
        }
        finally
        {
            try
            {
                //关闭字节流输入输出管道
                fis.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        return true;

    }

    /*
      * 此方法为android程序写入sd文件文件，用到了android-annotation的支持库@
     *
     * @param buffer   写入文件的内容
     * @param folder   保存文件的文件夹名称,如log；可为null，默认保存在sd卡根目录
     * @param mappingType 保存测绘文件类型M_ 主干道测绘 S_路径测绘
     * @param append   是否追加写入，true为追加写入，false为重写文件
     * @param autoLine 针对追加模式，true为增加时换行，false为增加时不换行
     */
    public synchronized static void writeFileToSDCard(final byte[] buffer,final String folder, final String mappingType,
                                                      final boolean append, final boolean autoLine) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean sdCardExist = Environment.getExternalStorageState().equals(
                        Environment.MEDIA_MOUNTED);
                String folderPath = "";
                if (sdCardExist) {
                   // 获取文件路径
                    folderPath = Environment.getExternalStorageDirectory()
                                + File.separator + folder + File.separator;

                } else {
                    return;
                }
                File fileDir = new File(folderPath);
                if (!fileDir.exists()) {
                    if (!fileDir.mkdirs()) {
                        return;
                    }
                }

                File file;
                File[] files = fileDir.listFiles(); //获取文件夹下的文件目录
            //    Log.d("debug001",files.toString());
                //文件命名方式是计数累加
                int fileTotal=0;
                for(int i=0;i<files.length;i++){
                    //判断是否出现字符串M_，L_
                    if(files[i].getName().toString().indexOf(mappingType) !=-1){
                        fileTotal++;
                    }
                }
               //文件名命名方式，mappingType+1-> mappingType+N
                //追加则打开最新文件
                if(append) {
                    file = new File(folderPath + mappingType + fileTotal + ".bin");
                }else{ //不追加就打开新建文件
                    file = new File(folderPath + mappingType + (fileTotal + 1) + ".bin");
                }

        //        Log.d("debug001",folderPath + mappingType + (fileTotal + 1) + ".bin");

           //     file = new File(folderPath + mappingType + 1 +".bin");
                RandomAccessFile raf = null;
                FileOutputStream out = null;
                try {
                    if (append) {
                        //如果为追加则在原来的基础上继续写文件
                        raf = new RandomAccessFile(file, "rw");
                        raf.seek(file.length());
                        raf.write(buffer);
                        if (autoLine) {
                            raf.write("\n".getBytes());
                        }
                    } else {
                        //重写文件，覆盖掉原来的数据
                        out = new FileOutputStream(file);
                        out.write(buffer);

                //        Log.d("debug001","写入数据："+);
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (raf != null) {
                            raf.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /***
     * 获取文件夹下的所有文件
     * @return
     */
    public static File[] getFileList(){
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        String folderPath = "";
        if (sdCardExist) {
            // 获取文件路径
            folderPath = Environment.getExternalStorageDirectory()
                    + File.separator + "Tank" + File.separator;

        }
        File fileDir = new File(folderPath);
        if (fileDir.exists()) {

            File[] files = fileDir.listFiles(); //获取文件夹下的文件目录

            return files;
        }
        return null;

    }

    /***
     * 获取测绘文件夹下所有测绘文件
     * @param filedir
     * @return
     */
    public static File[] getMappingList(String filedir){
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        String folderPath = "";
        if (sdCardExist) {
            // 获取文件路径
            folderPath = Environment.getExternalStorageDirectory()
                    + File.separator + filedir + File.separator;

        }
        File fileDir = new File(folderPath);
        if (fileDir.exists()) {

            File[] files = fileDir.listFiles(); //获取文件夹下的文件目录
            return files;
        }
        return null;

    }

        /**
         * 判断SD卡是否被挂载
         *
         * @return
         */
    public static boolean isSDCardMounted() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取SD卡的根目录
     *
     * @return
     */
    public static String getSDCardBaseDir() {
        if (isSDCardMounted()) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return null;
    }

    /**
     * 转换short为byte
     *
     * @param b
     * @param s 需要转换的short
     * @param index
     */
    public static void putShort(byte b[], short s, int index) {
        b[index + 0] = (byte) (s >> 0);
        b[index + 1] = (byte) (s >> 8);
    }

    /**
     * 通过byte数组取到short
     *
     * @param b
     * @param index  第几位开始取
     * @return
     */
    public static short getShort(byte[] b, int index) {
        return (short) (((b[index + 1] << 8) | b[index + 0] & 0xff));
    }

    /**
     *将32位的int值放到4字节的byte数组
     * @param num
     * @return
     */
    public static void putInt(byte[] b,int num,int index) {

        b[index++] = (byte)(num );
        b[index++] = (byte)(num >> 8);
        b[index++] = (byte)(num >> 16);
        b[index] = (byte)(num >> 24);
    }

    /**
     * 将4字节的byte数组转成一个int值
     * @param b
     * @return
     */
    public static int getInt(byte[] b,int index){

        int v = (b[index+3] & 0xff) << 24 | (b[index+2] & 0xff) << 16 |
                (b[index+1] & 0xff) << 8 | (b[index+0] & 0xff);
        return v;
    }

    /**
     * double转换byte
     *
     * @param b
     * @param x
     * @param index
     */
    public static void putDouble(byte[] b, double x, int index) {

        long d = Double.doubleToLongBits(x);
        for (int i = 0; i < 8; i++) {
            b[index + i] = (byte)((d >> 8 * i) & 0xff);
        }

    }

    /**
     * 通过byte数组取得Double
     *
     * @param b
     * @param index
     * @return
     */
    public static double getDouble(byte[] b, int index) {
        long d;
        d = b[index++] & 0xff;
        d |= ((long) b[index++] << 8) & 0xffff;
        d |= ((long) b[index++] << 16) & 0xffffff;;
        d |= ((long) b[index++] << 24) & 0xffffffffl;
        d |= ((long) b[index++] << 32) & 0xffffffffffl;
        d |= ((long) b[index++] << 40) & 0xffffffffffffl;
        d |= ((long) b[index++] << 48) & 0xffffffffffffffl;
        d |= ((long) b[index] << 56) & 0xffffffffffffffffl;
        return Double.longBitsToDouble(d);
    }


    /**
     * float转换byte
     *
     * @param b
     * @param x
     * @param index
     */
    public static void putFloat(byte[] b, float x, int index) {

        int  f = Float.floatToIntBits(x);

        b[index++] = (byte)(f);
        b[index++] = (byte)(f >> 8);
        b[index++] = (byte)(f >> 16);
        b[index] = (byte)(f >> 24);
    }

    /**
     * 通过byte数组取得float
     *
     * @param b
     * @param index
     * @return
     */
    public static float getFloat(byte[] b, int index) {
        int f;

        f = b[index + 0];
        f &= 0xff;
        f |= ((long) b[index + 1] << 8);
        f &= 0xffff;
        f |= ((long) b[index + 2] << 16);
        f &= 0xffffff;
        f |= ((long) b[index + 3] << 24);

        return Float.intBitsToFloat(f);
    }

}
