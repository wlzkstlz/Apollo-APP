package com.example.tank.plantprotectionrobot.DataProcessing;


import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getFloat;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getShort;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putFloat;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putShort;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.readFileFromSDCard;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.writeFileToSDCard;

/**
 * Created by TK on 2018/1/15.
 * RTK测绘数据类
 */

public class MappingGroup {

       //测绘信息
        public   byte rtkState;   //RTK_SINGLE=0,RTK_FLOAT=1,RTK_FIX=2,
        public   int longitude;//弧度制，毫弧
        public   int latitude;//角度制
        public   float altitude;//单位米
        public   float roll; //IMU
        public   float pitch; //IMU
        public   float yaw;//角度制
        public   short GPSTime_weeks;//GPS周数
        public   int GPSTime_ms;//周内毫秒数

        public static final int GPS_GROUP_LENGTH = 31; //测绘信息组一帧数据byte长度
        public static final int GPS_BAC_LEN = 16; //基站数据长度
        public static final int GPS_LEN= 4; //帧长节数
        public static final double INM_LON_LAT_SCALE  =100000000.0;//经纬度转换成定点小数的比例系数
        public static final double PI = 3.14159265358;//经纬度转换成定点小数的比例系数


    /***
     *
     * @param rtkState RTK状态
     * @param longitude 经度
     * @param latitude 纬度
     * @param altitude 海拔
     * @param roll IMU
     * @param pitch IMU
     * @param yaw  方向
     * @param GPSTime_weeks
     * @param GPSTime_ms
     */
    public void setMappingParam(byte rtkState,int longitude,int latitude,float altitude,float roll,float pitch,float yaw
    ,short GPSTime_weeks,int GPSTime_ms){
        this.rtkState = rtkState;
        this.longitude=longitude;
        this.latitude=latitude;
        this.altitude =altitude;
        this.roll = roll;
        this.pitch =pitch;
        this.yaw=yaw;
        this.GPSTime_weeks=GPSTime_weeks;
        this.GPSTime_ms =GPSTime_ms;

    }

    /***
     *  //将GSP组所有数据转化为字节流
     * @param gps 一个gps组类
     *@param index 偏移地址
     * @return true转化成功 false失败
     */
    public static boolean MappingGroupToArray(MappingGroup gps,byte[] gpsArray,int index){
        int count = index;
        if(gpsArray.length - index< GPS_GROUP_LENGTH){
            return false;
        }
        //GPS状态
        gpsArray[count] = gps.rtkState;
        count=count+1;
        putInt(gpsArray, gps.longitude,count); //经度

        count=count+4;
        putInt(gpsArray, gps.latitude,count); //纬度
        count=count+4;
        putFloat(gpsArray, gps.altitude,count);//海拔
        count=count+4;
        putFloat(gpsArray, gps.roll,count);//
        count=count+4;
        putFloat(gpsArray, gps.pitch,count);//
        count=count+4;
        putFloat(gpsArray, gps.yaw,count);//方向
        count=count+4;
        putShort(gpsArray, gps.GPSTime_weeks,count);//GPS周数
        count=count+2;
        putInt(gpsArray, gps.GPSTime_ms,count);//周内毫秒数

        return true;
    }

    /***
     *
     * @param array 需要存为gps类的byte[]数据
     * @param index 数据起点
     * @return
     */
    public  static MappingGroup ByteToArrayMappingGroup(byte [] array,int index){
        int count=index;
        MappingGroup mappingGroup = new MappingGroup();
        //GPS状态
        mappingGroup.rtkState = array[count];
        count=count+1;
        mappingGroup.longitude = getInt(array,count);
        count=count+4;
        mappingGroup.latitude = getInt(array,count);
        count=count+4;
        mappingGroup.altitude = getFloat(array,count);
        count=count+4;
        mappingGroup.roll = getFloat(array,count);
        count=count+4;
        mappingGroup.pitch = getFloat(array,count);
        count=count+4;
        mappingGroup.yaw = getFloat(array,count);
        count=count+4;
        mappingGroup.GPSTime_weeks = getShort(array,count);
        count=count+2;
        mappingGroup.GPSTime_ms = getInt(array,count);

        /*
        Log.d("debug001",
                "RTK状态："+mappingGroup.getRtkState()+
                        " 经度："+mappingGroup.getLongitude()+
                        " 纬度："+mappingGroup.getLatitude()+
                        " 海拔："+mappingGroup.getAltitude()+
                         "方向："+mappingGroup.getDirection()+
                           "周："+mappingGroup.getGPSTime_wn()+
                        " 毫秒："+mappingGroup.getGPSTime_tow());
                        */
        return mappingGroup;

    }


    /***
     *
     * @param openfile 需要打开的文件名
     * @param gpslist   用于保存转换MappingGroup类结果
     * @param rLen      需要读取的测绘数据长度，即是测绘点个数
     * @return          读取成功true 失败flase
     */
    public static boolean getMappingData(String openfile,ArrayList<MappingGroup> gpslist,int rLen){
        int bufLen = GPS_LEN +GPS_BAC_LEN  +rLen*GPS_GROUP_LENGTH;
        byte[] buf = new byte[bufLen];

        if(rLen>0) {
            //读取数据
            if(readFileFromSDCard(buf, false, bufLen, openfile)){
                //解析数据
                for(int i=0;i<rLen;i++){
                        //保存结果
                    gpslist.add(ByteToArrayMappingGroup(buf,GPS_LEN +GPS_BAC_LEN  +i*GPS_GROUP_LENGTH));
              //          Log.d("Tank001", "读取 RTK状态：" + gpslist.get(gpslist.size()-1).rtkState + "时间：" + gpslist.get(gpslist.size()-1).GPSTime_tow + "经度：" + gpslist.get(gpslist.size()-1).longitude
               //                + "纬度：" + gpslist.get(gpslist.size()-1).latitude + "海拔：" + gpslist.get(gpslist.size()-1).altitude + "方向：" + gpslist.get(gpslist.size()-1).direction+"\n");
                }
            }else{
                return false;
            }
        }else{
            return false;
        }
        return true;
    }

    /***
     * 获取测绘数据文件测绘文件长度，即是测绘的点数
     * @param openfile
     * @param len
     * @return
     */
    public static boolean getMappingLength(String openfile,int[] len){
        byte[] buf = new byte[ GPS_LEN];
        //读取数据
        if(readFileFromSDCard(buf, true,  GPS_LEN, openfile)){
            //解析数据
            len[0] = getInt(buf,0);

        }else{
            return false;
        }

        return true;
    }

    /***
     *获取信息头，帧长 以及基站坐标
     * @param openfile 文件名
     * @param point    基站坐标值
     * @param len 返回帧长 返回的数据在len[0]
     * @return
     */
    public static boolean getMappingHead(String openfile,int[] len,GpsPoint point){
        byte[] buf = new byte[ GPS_LEN+GPS_BAC_LEN];
        //读取基站数据
        if(readFileFromSDCard(buf, true,  GPS_LEN+GPS_BAC_LEN, openfile)){
            //解析数据
            len[0] = getInt(buf,0);//帧长
            point.x = getDouble(buf,GPS_LEN); //基站经度
            point.y = getDouble(buf,GPS_LEN+GPS_BAC_LEN/2);//基站纬度

        }else{
            return false;
        }

        return true;
    }
    /***
     *
     * @param len 测绘帧长
     */
    public static boolean setMappingLength(String wrieFile,String mType,int len){
        byte[] wdata = new byte[ GPS_LEN];

        putInt(wdata,len,0);
        //写入文件，不追加，会创建新文件
        writeFileToSDCard(wdata, wrieFile, mType, false, false);

        return true;

    }


    /***
     * 分段写入数据
     * @param gpslist 需要写入的数据
     * @param off     数据帧偏移地址，帧头是帧长，即是4byte。
     */
    public static void saveMappingData(ArrayList<MappingGroup> gpslist,int len,int off,String wrieFile,String mType){
        byte[] wdata = new byte[gpslist.size()*GPS_GROUP_LENGTH];
        for(int i=0;i<gpslist.size();i++){
            MappingGroupToArray(gpslist.get(i),wdata,off+GPS_GROUP_LENGTH*i);
        }
        //追加写入
        writeFileToSDCard(wdata, wrieFile, mType, true, false);

    }

    /***
     * 一次性写入数据
     *
     * @param gpslist 需要写入的数据
     */
    public static void saveMappingDataAll(ArrayList<MappingGroup> gpslist,GpsPoint bPoint,String wrieFile,String mType){
        int off=0;
       byte[] wdata = new byte[ GPS_LEN+GPS_BAC_LEN+gpslist.size()*GPS_GROUP_LENGTH];
       //填入帧长数据
        putInt(wdata,gpslist.size(),off);
        //填入基站数据
        off = off+GPS_LEN;
        putDouble(wdata,bPoint.x,off);
        off = off+8;
        putDouble(wdata,bPoint.y,off);
        //填入测绘数据
        off = off+ 8;
        for(int i=0;i<gpslist.size();i++){
            MappingGroupToArray(gpslist.get(i),wdata,off+GPS_GROUP_LENGTH*i);
       //     Log.d("Tank001", "写入 RTK状态：" + gpslist.get(i).rtkState + "时间：" + gpslist.get(i).GPSTime_tow + "经度：" + gpslist.get(i).longitude
       //             + "纬度：" + gpslist.get(i).latitude + "海拔：" + gpslist.get(i).altitude + "方向：" + gpslist.get(i).direction+"\n");

        }

        //写入文件，不追加，会创建新文件
        writeFileToSDCard(wdata, wrieFile, mType, false, false);


    }
}
