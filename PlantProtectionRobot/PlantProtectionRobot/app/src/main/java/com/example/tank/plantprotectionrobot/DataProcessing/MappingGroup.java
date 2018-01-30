package com.example.tank.plantprotectionrobot.DataProcessing;


import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getShort;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putDouble;
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
        public   double longitude;//角度制
        public   double latitude;//角度制
        public   double altitude;//单位米
        public   double direction;//角度制
        public   short GPSTime_wn;//GPS周数
        public   int GPSTime_tow;//周内毫秒数
        public static final int GPS_GROUP_LENGTH = 39; //测绘信息组一帧数据byte长度
        public static final int GPS_BAC_LEN = 16; //基站数据长度
       public static final int GPS_LEN= 4; //帧长节数

    /***
     *
     * @param rtkState
     * @param longitude
     * @param latitude
     * @param altitude
     * @param direction
     * @param GPSTime_wn
     * @param GPSTime_tow
     */
    public void setMappingParam(byte rtkState,double longitude,double latitude,double altitude,double direction
    ,short GPSTime_wn,int GPSTime_tow){
        this.rtkState = rtkState;
        this.longitude=longitude;
        this.latitude=latitude;
        this.altitude =altitude;
        this.direction=direction;
        this.GPSTime_wn=GPSTime_wn;
        this.GPSTime_tow =GPSTime_tow;
    }

    /***
     *  //将GSP组所有数据转化为
     * @param gps 一个gps组类
     *@param index 偏移地址
     * @return true转化成功 false失败
     */
    public static boolean MappingGroupToArray(MappingGroup gps,byte[] gpsArray,int index){
        int count = index;
        if(gpsArray.length < GPS_GROUP_LENGTH){
            return false;
        }
        //GPS状态
        gpsArray[count] = gps.rtkState;
        count=count+1;
        putDouble(gpsArray, gps.longitude,count); //经度

        count=count+8;
        putDouble(gpsArray, gps.latitude,count); //纬度
        count=count+8;
        putDouble(gpsArray, gps.altitude,count);//海拔
        count=count+8;
        putDouble(gpsArray, gps.direction,count);//方向
        count=count+8;
        putShort(gpsArray, gps.GPSTime_wn,count);//GPS周数
        count=count+2;
        putInt(gpsArray, gps.GPSTime_tow,count);//周内毫秒数

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
        mappingGroup.longitude = getDouble(array,count);
        count=count+8;
        mappingGroup.latitude = getDouble(array,count);
        count=count+8;
        mappingGroup.altitude = getDouble(array,count);
        count=count+8;
        mappingGroup.direction = getDouble(array,count);
        count=count+8;
        mappingGroup.GPSTime_wn = getShort(array,count);
        count=count+2;
        mappingGroup.GPSTime_tow = getInt(array,count);

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
    public static void setMappingData(ArrayList<MappingGroup> gpslist,int len,int off,String wrieFile,String mType){
        byte[] wdata = new byte[len*GPS_GROUP_LENGTH];
        for(int i=0;i<len;i++){
            MappingGroupToArray(gpslist.get(i),wdata,off+GPS_GROUP_LENGTH*i);
        }
        //追加写入
        writeFileToSDCard(wdata, wrieFile, mType, true, false);

    }

    /***
     * 一次性写入数据
     *
     * @param gpslist 需要写入的数据
     * @param len     测绘数据帧长
     */
    public static void setMappingDataAll(ArrayList<MappingGroup> gpslist,int len,GpsPoint bPoint,String wrieFile,String mType){
        int off=0;
       byte[] wdata = new byte[ GPS_LEN+len*GPS_GROUP_LENGTH];
       //填入帧长数据
        putInt(wdata,len,off);
        //填入基站数据
        off = off+GPS_LEN;
        putDouble(wdata,bPoint.x,off);
        off = off+GPS_LEN+GPS_BAC_LEN/2;
        putDouble(wdata,bPoint.y,off);
        //填入测绘数据
        off = off+GPS_LEN+ GPS_BAC_LEN;
        for(int i=0;i<len;i++){
            MappingGroupToArray(gpslist.get(i),wdata,off+GPS_GROUP_LENGTH*i);
        }

        //写入文件，不追加，会创建新文件
        writeFileToSDCard(wdata, wrieFile, mType, false, false);


    }
}
