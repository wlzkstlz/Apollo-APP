package com.example.tank.plantprotectionrobot.WaveFiltering;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;

import java.io.File;
import java.util.ArrayList;

import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.MappingGroupToArray;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingData;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingHead;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getFloat;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putFloat;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putShort;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.readFileFromSDCard;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.writeFileToSDCard;
import static java.lang.Math.pow;

/**
 * Created by 54391 on 2018/2/7.
 */

public class RobotCruisePath {
    public static final double EPSINON=0.00001;

    public String mFileName;
    public String mFileDir;
    public ArrayList<PointF>mPoints;

    private ArrayList<PointF>mPoints_1;
    private ArrayList<PointF>mPoints_2;
    private ArrayList<PointF>mPoints_5;
    private ArrayList<PointF>mPoints_10;
    private ArrayList<PointF>mPoints_20;
    private ArrayList<PointF>mPoints_50;

    public ArrayList<PathPoint>mPathPoints;
    private RectF mRectRange;
    private double mSumX,mSumY;
    private int mSumNum;

    //TODO start ----------------------------------//
    public  GpsPoint bPoint = new GpsPoint();
    public static final int POINT_LEN = 8; //测绘信息PathPoint数据byte长度
    public static final int RTK_BAC_LEN = 16; //基站数据长度
    public static final int PATH_POINT_LEN = 20;//PathPoint类数据长度
    public static final float POINT_PA =0.3f;//转换参数
    public static final float POINT_PB =2.0f;
    public static final float POINT_PC =0.1f;
    public static final float POINT_PD =(float) (5.0*MappingGroup.PI);
    public static final float POINT_PE =(float) (5.0*MappingGroup.PI);



    //TODO end ------------------------------------//

    public RobotCruisePath() {
        mPoints=new ArrayList<PointF>();
        mPathPoints=new ArrayList<PathPoint>();
        mRectRange=new RectF();

    }

    //数据编辑

    //线段/圆弧数据点
    public class PathPoint{
        PointF startPt;
        PointF aPt;
        float deltaPhi;
    }

    public void AddPointForce(PointF point){
        mPoints.add(point);
        mSumNum=1;
        mSumX=point.x;
        mSumY=point.y;
    }

    /**
     * 测绘时使用此函数添加新路径点
     * @param point  Data point to add
     * @param deltaDist filter cluster distance threshold
     * @return if a point is added
     */
    public boolean AddPoint(PointF point,float deltaDist){
        if (mPoints.isEmpty()){
            AddPointForce(point);
        }
        else{
            PointF cluster_pt=mPoints.get(mPoints.size()-1);
            if (Math.sqrt(pow(cluster_pt.x-point.x,2)+ pow(cluster_pt.y-point.y,2))<deltaDist){
                mSumX+=point.x;
                mSumY+=point.y;
                mSumNum++;
                return false;
            }
            else{
                mPoints.set(mPoints.size()-1,new PointF((float)( mSumX/mSumNum),(float)( mSumY/mSumNum)));
                AddPointForce(point);
            }
        }
        return true;
    }

    /**
     * 删除尾部路径片段
     * @param id 切除点位置
     */
    public void DeletePoints(int id){
        if (id>=mPoints.size()){
            return;
        }

        ArrayList<PointF>points=new ArrayList<>();
        points.addAll(mPoints.subList(0,id));
        mPoints=points;

        mSumNum=1;
        mSumX=mPoints.get(mPoints.size()-1).x;
        mSumY=mPoints.get(mPoints.size()-1).y;
    }
    //数据分析

    /**
     * create PathPoints List which consists of lines and arcs from raw PointF List data
     * @param pa 圆弧近似时的偏差上界
     * @param pb 圆弧近似时的圆弧长度上界
     * @param pc 圆弧半径下界
     * @param pd 共线判断角度阈值,弧度制
     * @param pe 尖锐拐弯角度阈值，弧度制
     */
    public void createPathPoints(float pa , float pb, float pc,float pd,float pe ){
        mPathPoints.clear();
   //     ArrayList<PointF> points_buf=new ArrayList<>(mPoints.size());
        ArrayList<PointF> points_buf=new ArrayList<PointF>();
        points_buf.addAll(mPoints.subList(0,mPoints.size()));
        /*
        for (int i=0;i<mPoints.size();i++){
            points_buf.set(i,new PointF(mPoints.get(i).x,mPoints.get(i).y));
        }*/

        for (int i=2;i<points_buf.size();i++){
            PathPoint pathPt=new PathPoint();
            pathPt.startPt = points_buf.get(i-2);

            float theta1 = (float) CommonAlg.GetThetaFromVector(new PointF(points_buf.get(i-1).x-points_buf.get(i-2).x, points_buf.get(i-1).y - points_buf.get(i-2).y));
            float theta2 = (float)CommonAlg.GetThetaFromVector(new PointF(points_buf.get(i).x - points_buf.get(i-1).x, points_buf.get(i).y - points_buf.get(i-1).y));
            float deltaTheta = theta2 - theta1;
            while (deltaTheta > Math.PI) { deltaTheta -= 2 * Math.PI; }
            while (deltaTheta < -Math.PI) { deltaTheta += 2 * Math.PI; }

            if ((Math.abs(deltaTheta) < pd)|| (Math.abs(Math.abs(deltaTheta) - Math.PI) < pe))//接近共线or转角过于尖锐
            {
                pathPt.aPt = points_buf.get(i-1);
                pathPt.deltaPhi = 0;
                mPathPoints.add(pathPt);
                continue;
            }

            float theta_half = (float)Math.abs(deltaTheta*0.5);
            float Rmax1 =  (float)(Math.cos(theta_half)/(1 - Math.cos(theta_half))*pa);
            float length1 = (float) Math.sqrt(pow(points_buf.get(i-1).x-points_buf.get(i-2).x,2)+ pow(points_buf.get(i-1).y - points_buf.get(i-2).y, 2));
            float length2 = (float)Math.sqrt(pow(points_buf.get(i).x - points_buf.get(i-1).x, 2) + pow(points_buf.get(i).y - points_buf.get(i-1).y, 2));
            float Rmax2 = (float)(length1/Math.tan(theta_half));
            float Rmax3 = (float)(length2*0.5 / Math.tan(theta_half));
            float Rmax4 = pb / Math.abs(deltaTheta);

            float R = Math.min(Math.min(Rmax1, Rmax2), Math.min(Rmax3,Rmax4));

            if (R < pc)//R不够大，干脆不要
            {
                pathPt.aPt = points_buf.get(i-1);
                pathPt.deltaPhi = 0;
                mPathPoints.add(pathPt);
                continue;
            }

            float A1, B1, C1, D1, A2, B2, C2, D2;
            A1 = points_buf.get(i-1).y - points_buf.get(i-2).y;
            B1 = points_buf.get(i-2).x - points_buf.get(i-1).x;
            C1 = points_buf.get(i-1).x*points_buf.get(i-2).y - points_buf.get(i-2).x*points_buf.get(i-1).y;
            D1 = (float)Math.sqrt(pow(A1,2)+ pow(B1,2));

            A2 = points_buf.get(i).y - points_buf.get(i-1).y;
            B2 = points_buf.get(i-1).x - points_buf.get(i).x;
            C2 = points_buf.get(i).x*points_buf.get(i-1).y - points_buf.get(i-1).x*points_buf.get(i).y;
            D2 = (float)Math.sqrt(pow(A2, 2) + pow(B2, 2));

            float A1B2mA2B1 = A1*B2 - A2*B1;
            if (Math.abs(A1B2mA2B1)<EPSINON)
            {
                //共线情况。。。
                pathPt.aPt = points_buf.get(i-1);
                pathPt.deltaPhi = 0;
                mPathPoints.add(pathPt);
                continue;
            }

            if (A1*points_buf.get(i).x + B1*points_buf.get(i).y + C1 > 0)
                D1 = -D1;

            if (A2*points_buf.get(i-2).x + B2*points_buf.get(i-2).y + C2 > 0)
                D2 = -D2;

            float x0 = (B2*C1 - B1*C2 + (B2*D1 - B1*D2)*R) / (-A1B2mA2B1);
            float y0= (A2*C1 - A1*C2 + (A2*D1 - A1*D2)*R) / A1B2mA2B1;

            PointF center_pt=new PointF(x0,y0);

            if (Math.abs(R*Math.tan(theta_half) - length1) < EPSINON)//i-2点作为圆弧起点
            {
                pathPt.aPt= center_pt;
                pathPt.deltaPhi = deltaTheta;
                mPathPoints.add(pathPt);

                PointF vect = new PointF(points_buf.get(i-2).x - center_pt.x,points_buf.get(i-2).y - center_pt.y);
                CommonAlg.RotateTheta4Point(vect, deltaTheta);
                points_buf.set(i - 1,new PointF(center_pt.x+vect.x,center_pt.y+vect.y));// = center_pt +vect;
                continue;
            }

            PointF footPt1 =new PointF();
            PointF footPt2 =new PointF();
            footPt1.x = (B1*B1*x0 - A1*B1*y0 - A1*C1) / (A1*A1+B1*B1);
            footPt1.y = (A1*A1*y0 - A1*B1*x0 - B1*C1) / (A1*A1 + B1*B1);
            footPt2.x = (B2*B2*x0 - A2*B2*y0 - A2*C2) / (A2*A2 + B2*B2);
            footPt2.y = (A2*A2*y0 - A2*B2*x0 - B2*C2) / (A2*A2 + B2*B2);

            pathPt.aPt = footPt1;
            pathPt.deltaPhi = 0;
            mPathPoints.add(pathPt);

            PathPoint pathPt2=new PathPoint();
            pathPt2.startPt = footPt1;
            pathPt2.aPt = center_pt;
            pathPt2.deltaPhi = deltaTheta;
            mPathPoints.add(pathPt2);

            points_buf.set(i - 1,new PointF( footPt2.x,footPt2.y));
            continue;
        }
    }

    /**
     * 更新路径范围矩形，在完成所有路径点添加后调用此函数
     */
    void UpdateRange(){
        for (int i=0;i<mPoints.size();i++){
            mRectRange.left=mRectRange.left<mPoints.get(i).x?mRectRange.left:mPoints.get(i).x;
            mRectRange.right=mRectRange.right>mPoints.get(i).x?mRectRange.right:mPoints.get(i).x;
            mRectRange.top=mRectRange.top>mPoints.get(i).y?mRectRange.top:mPoints.get(i).y;
            mRectRange.bottom=mRectRange.bottom<mPoints.get(i).y?mRectRange.bottom:mPoints.get(i).y;
        }
    }

    /**
     * 使用前必须先调用UpdateRange()函数
     * @param range 输入的屏幕窗口矩形，以基站为原点的东北上坐标系，单位米
     * @return true表示路径与屏幕有交集，false表示没有交集
     */
    boolean isCrossRange(RectF range){
        if (mRectRange.left>range.right||mRectRange.right<range.left||mRectRange.bottom>range.top||mRectRange.top<range.bottom){
            return false;
        }
        else{
            return true;
        }
    }

    /**
     *
     * @param outs  输出的经过稀疏后的路径点
     * @param distThreshold  输入的稀疏距离参数，单位米
     * @return
     */
    void createSparsePoints(ArrayList<PointF> outs, float distThreshold)
    {
        outs.clear();
        if (mPoints.isEmpty()){
            return ;
        }

        outs.add(new PointF(mPoints.get(0).x,mPoints.get(0).y));
        for (int i = 1; i <mPoints.size() ; i++) {
            if (Math.sqrt(pow(outs.get(outs.size()-1).x-mPoints.get(i).x,2)+pow(outs.get(outs.size()-1).y-mPoints.get(i).y,2))>distThreshold){
                outs.add(new PointF(mPoints.get(i).x,mPoints.get(i).y));
            }
        }
    }

    /**
     * 更新稀疏路径点数据，在完成所有路径点添加后调用此函数
     */
    void updateSparsePoints(){
        createSparsePoints(mPoints_1,1.0f);
        createSparsePoints(mPoints_2,2.0f);
        createSparsePoints(mPoints_5,5.0f);
        createSparsePoints(mPoints_10,10.0f);
        createSparsePoints(mPoints_20,20.0f);
        createSparsePoints(mPoints_50,50.0f);
    }

    /**
     * 使用前必须先调用updateSparsePoints()函数
     * @param outs 输出的稀疏路径点，注意是引用！
     * @param distThreshold  稀疏距离参数，单位米
     * @return  返回稀疏路径点个数
     */
    int getSparsePoints(ArrayList<PointF>outs,float distThreshold){
        if (distThreshold<0.5f){
            outs= mPoints;
        }
        else if (distThreshold<1.0f){
            outs=mPoints_1;
        }
        else if (distThreshold<2.0f){
            outs=mPoints_2;
        }
        else if (distThreshold<5.0f){
            outs=mPoints_5;
        }
        else if (distThreshold<10.0f){
            outs=mPoints_10;
        }
        else if (distThreshold<20.0f){
            outs=mPoints_20;
        }
        else{
            outs=mPoints_50;
        }

        return outs.size();
    }

    //文件IO操作
    public void Open(String fname, String fdir){
        mFileName=fname;
        mFileDir=fdir;
        //TODO 从SD卡读取路径文件数据
       if(readPathPointFromSD()){
           createPathPoints(POINT_PA, POINT_PB, POINT_PC,POINT_PD,POINT_PE );
       }

    }
    public void Save(){
        if(!mFileName.isEmpty()){
            //TODO 保存到原路径
        }
    }
    public void Save(String fname,String fdir){
        //TODO 保存到新路径
        savePathPointsAll(fname, fdir);
    }

//TODO  start --------------------------------------------//
    /***
     * 一次性写入数据
     */
    private void savePathPointsAll(String mType,String wrieFile){
        int off=0;
        byte[] wdata = new byte[ 4+RTK_BAC_LEN +mPoints.size()*POINT_LEN];
        //填入帧长数据
        putInt(wdata,mPoints.size(),off);
        //填入基站数据
        off = off+4;
        putDouble(wdata,bPoint.x,off);
        off = off+8;
        putDouble(wdata,bPoint.y,off);
        //填入测绘数据
        off = off+ 8;

        for(int i=0;i<mPoints.size();i++){

            putFloat(wdata, mPoints.get(i).x,off);
            off = off+4;
            putFloat(wdata, mPoints.get(i).y,off);
            off = off+4;

       //     Log.d("Tank001","点数"+mPoints.size()+"基站"+bPoint.x+" "+bPoint.y+"经度："+mPoints.get(i).x+" 纬度:"+mPoints.get(i).y);
        }
        //写入文件，不追加，会创建新文件
        writeFileToSDCard(wdata, wrieFile, mType, false, false);
    }

    /***
     * 从SD卡读取路径文件
     * @return
     */
    private boolean readPathPointFromSD(){

        String openf =   mFileDir + File.separator+mFileName;
        int len;

        //读取数据
        byte[] buf = new byte[ 4+RTK_BAC_LEN];
        //读取基站数据
        if(readFileFromSDCard(buf, true,  4+RTK_BAC_LEN, openf)){
            //解析数据
            len = getInt(buf,0);//帧长
            bPoint.x = getDouble(buf,4); //基站经度
            bPoint.y = getDouble(buf,4+RTK_BAC_LEN/2);//基站纬度
       //     Log.d("Tank001","长度"+len+" 经度"+bPoint.x+" 纬度"+bPoint.y);

            if(len>0){
                int bufLen = 4 + RTK_BAC_LEN +len*POINT_LEN;
                byte[] bufp = new byte[bufLen];

                    //读取数据
                    if(readFileFromSDCard(bufp, false, bufLen, openf)){
                        //解析数据
                        int indx = 4 + RTK_BAC_LEN;

                        for(int i=0;i<len;i++){

                            PointF pointF = new PointF();

                            pointF.x = getFloat(bufp,indx);
                            indx +=4;
                            pointF.y = getFloat(bufp,indx);
                            indx +=4;
                            mPoints.add(pointF);

                      //      Log.d("Tank001","点数"+len+"基站"+bPoint.x+" "+bPoint.y+"经度："+mPoints.get(i).x+" 纬度:"+mPoints.get(i).y);
                        }
                    }else {
                        return false;
                    }
            }

        }else{
            return false;
        }
       return true;
    }

    /***
     * 将路径文件转为字节流
     * @return
     */
    public byte[] PathPoitToArray(PathPoint pathPoint){

        byte[] pbuf = new byte[20];
        int index=0;

        putFloat(pbuf,pathPoint.startPt.x,index);
        index+=4;
        putFloat(pbuf,pathPoint.startPt.y,index);
        index+=4;
        putFloat(pbuf,pathPoint.aPt.x,index);
        index+=4;
        putFloat(pbuf,pathPoint.aPt.y,index);
        index+=4;
        putFloat(pbuf,pathPoint.deltaPhi,index);

        return pbuf;
    }
    //TODO  end --------------------------------------------//
}
