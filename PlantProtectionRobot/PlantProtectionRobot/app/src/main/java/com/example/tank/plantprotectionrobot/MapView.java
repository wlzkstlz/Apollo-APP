package com.example.tank.plantprotectionrobot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by TK on 2018/1/17.
 */

import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;

import java.util.ArrayList;

public class MapView extends View {

    private  int drawNumber = 0;
    private  float mapRatio;

    private ArrayList<GpsPoint> mListPoint = new ArrayList<GpsPoint>();
    private ArrayList<GpsPoint> mListPoint1 = new ArrayList<GpsPoint>();
    private ArrayList<GpsPoint> mListPoint2 = new ArrayList<GpsPoint>();
    private Paint mPaint = new Paint();   //绘制路径画笔
    private Paint mPaint1 = new Paint(); //绘制删除或是还未完成的路径
    private Paint mPaint2 = new Paint();  //绘制主干道
    private GpsPoint Lcon1 = new GpsPoint();    //
    private GpsPoint Lcon2 = new GpsPoint();
    private GpsPoint basicLon = new GpsPoint();
    private GpsPoint mDet = new GpsPoint();
    private GpsPoint mPson = new GpsPoint();
    private GpsPoint movePoint = new GpsPoint();
    private Matrix matrix = new Matrix();


    private Bitmap bPson;
    private Bitmap bMap1;
    private Bitmap bMap2;
    private Bitmap bDet;
    private Bitmap bBasic;

    public MapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public MapView(Context context) {

        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);

        canvas.translate((int)movePoint.x,(int)movePoint.y);

        //绘制第一条
        Path path=new Path();
        for (int index = 0; index < mListPoint.size(); index++) {

            if(index == 0){
                path.moveTo( (int)(mapRatio*mListPoint.get(index).x),(int)(mapRatio*mListPoint.get(index).y));
            }else if(index == mListPoint.size() -1){
                path.moveTo( (int)(mapRatio*mListPoint.get(index).x),(int)(mapRatio*mListPoint.get(index).y));
            }else {
                path.lineTo( (int)(mapRatio*mListPoint.get(index).x),(int)(mapRatio*mListPoint.get(index).y));
            }

          //  path.addCircle((int)(mapRatio*mListPoint.get(index).x),(int)(mapRatio*mListPoint.get(index).y),3, Path.Direction.CW);
        }
        path.close();
        canvas.drawPath(path, mPaint);

        //绘制第二条，被擦除部分
        Path path1=new Path();
        for(int index = 0; index < mListPoint1.size(); index++) {

            if(index == 0){
                path1.moveTo( (int)(mapRatio*mListPoint1.get(index).x),(int)(mapRatio*mListPoint1.get(index).y));
            }else if(index == mListPoint1.size() -1){
                path1.moveTo( (int)(mapRatio*mListPoint1.get(index).x),(int)(mapRatio*mListPoint1.get(index).y));
            }else {
                path1.lineTo( (int)(mapRatio*mListPoint1.get(index).x),(int)(mapRatio*mListPoint1.get(index).y));
            }


        }
        path1.close();
        canvas.drawPath(path1, mPaint1);


        //绘制第三条,主干道
        Path path2=new Path();
        for(int index = 0; index < mListPoint2.size(); index++) {

            if(index == 0){
                path2.moveTo((int)( mapRatio*mListPoint2.get(index).x),(int)(mapRatio*mListPoint2.get(index).y));
            }else if(index == mListPoint2.size() -1){
                path2.moveTo( (int)(mapRatio*mListPoint2.get(index).x),(int)(mapRatio*mListPoint2.get(index).y));
            }else {
                path2.lineTo( (int)(mapRatio*mListPoint2.get(index).x),(int)(mapRatio*mListPoint2.get(index).y));
            }
            path.addCircle((int)(mapRatio*mListPoint2.get(index).x),(int)(mapRatio*mListPoint2.get(index).y),3, Path.Direction.CW);
        }
        path2.close();
        canvas.drawPath(path2, mPaint2);

        if(1 == drawNumber) {

            //工作模式
            matrix.reset();
            matrix.postRotate((float) Lcon1.d,(int) (mapRatio*Lcon1.x-bMap1.getWidth()/2),(int)(mapRatio*Lcon1.y-bMap1.getHeight()/2));
            matrix.postTranslate((int) (mapRatio*Lcon1.x-bMap1.getWidth()/2),(int)(mapRatio*Lcon1.y-bMap1.getHeight()/2));
            canvas.drawBitmap(bMap1, matrix, new Paint());

            matrix.reset();
            matrix.postRotate((float) mPson.d,(int)(mapRatio*mPson.x-bPson.getWidth()/2),(int)(mapRatio*mPson.y-bPson.getHeight()/2));
            matrix.postTranslate((int)(mapRatio*mPson.x-bPson.getWidth()/2),(int)(mapRatio*mPson.y-bPson.getHeight()/2));
            canvas.drawBitmap(bPson, matrix, new Paint());

        }else if(2 == drawNumber){

            matrix.reset();
            matrix.postRotate((float) Lcon2.d,(int)(bMap2.getWidth()/2),(int)(bMap2.getHeight()/2));
            matrix.postTranslate((int)(mapRatio*Lcon2.x-bMap2.getWidth()/2),(int)(mapRatio*Lcon2.y-bMap2.getHeight()/2));
            canvas.drawBitmap(bMap2, matrix, new Paint());

            //测绘模式1
            matrix.reset();
            matrix.postRotate((float) Lcon1.d,(int)(bMap1.getWidth()/2),(int)(bMap1.getHeight()/2));
            matrix.postTranslate((int)(mapRatio*Lcon1.x-bMap1.getWidth()/2),(int)(mapRatio*Lcon1.y-bMap1.getHeight()/2));
            canvas.drawBitmap(bMap1, matrix, new Paint());


        }else if(3 == drawNumber){

            matrix.reset();
            matrix.postRotate((float) Lcon1.d,(int)(bMap1.getWidth()/2),(int)(bMap1.getHeight()/2));
            matrix.postTranslate((int)(mapRatio*Lcon1.x-bMap1.getWidth()/2),(int)(mapRatio*Lcon1.y-bMap1.getHeight()/2));
            canvas.drawBitmap(bMap1, matrix, new Paint());

            matrix.reset();
           matrix.postRotate((float) Lcon2.d,(int)(bMap2.getWidth()/2),(int)(bMap2.getHeight()/2));
            matrix.postTranslate((int)(mapRatio*Lcon2.x-bMap2.getWidth()/2),(int)(mapRatio*Lcon2.y-bMap2.getHeight()/2));
            canvas.drawBitmap(bMap2, matrix, new Paint());

            matrix.reset();
            matrix.postTranslate((int)(mapRatio*mDet.x-bDet.getWidth()/2),(int)(mapRatio*mDet.y-bDet.getHeight()/2));
            canvas.drawBitmap(bDet, matrix, new Paint());

        }

        matrix.reset();
        matrix.postTranslate((int)(mapRatio*basicLon.x-bBasic.getWidth()/2),(int)(mapRatio*basicLon.y-bBasic.getHeight()/2));
        canvas.drawBitmap(bBasic, matrix, new Paint());

    }

    /**
     */
    public void setLinePoint1(ArrayList<GpsPoint> list1, ArrayList<GpsPoint> list2,GpsPoint point1,GpsPoint point2,GpsPoint mvPoint,float ratio)
    {
        mListPoint =list1;
        mListPoint1 =list2;
        mListPoint2.clear();

        drawNumber=1;
        Lcon1 = point1;
        mPson = point2;
        mapRatio = ratio;

        movePoint = mvPoint;
        invalidate();
    }

    /**
     */
    public void setLinePoint2(ArrayList<GpsPoint> list1, ArrayList<GpsPoint> list3,GpsPoint point1,GpsPoint point2,GpsPoint mvPoint,float ratio)
    {
        mListPoint =list1;
        mListPoint1.clear();
        mListPoint2 =list3;

        Lcon1 = point1;
        Lcon2 = point2;
        movePoint = mvPoint;

        drawNumber =2;
        mapRatio = ratio;
        invalidate();
    }


    public void setLinePoint3(ArrayList<GpsPoint> list1, ArrayList<GpsPoint> list2,ArrayList<GpsPoint> list3,GpsPoint point1,GpsPoint point2,GpsPoint point3,GpsPoint mvPoint,float ratio)
    {
        mListPoint =list1;
        mListPoint1 =list2;
        mListPoint2 = list3;

        Lcon1 = point1;
        Lcon2 = point2;
        mDet = point3;

        movePoint = mvPoint;
        drawNumber =3;
        mapRatio = ratio;


        invalidate();
    }

    /***
     * 使用前必须初始化
     */
    public  void InitView(GpsPoint zero,GpsPoint bLon,float ratio){

        movePoint = zero; //初始偏移坐标
        mapRatio = ratio; //初始比例尺
        basicLon.x = bLon.x/2;//基站位置
        basicLon.y = bLon.y/2;//基站位置

        //    Log.d("Tank001","基站位置"+basicLon.x+" "+bBasic.);

        //画笔初始化
        mPaint.setColor(Color.rgb(37, 155, 36));
        mPaint.setStrokeWidth(4f);
        mPaint.setStyle(Paint.Style.STROKE);

        mPaint1.setColor(Color.rgb(170, 170, 170));
        mPaint1.setStrokeWidth(4f);
        mPaint1.setStyle(Paint.Style.STROKE);

        mPaint2.setColor(Color.rgb(63, 81, 181));
        mPaint2.setStrokeWidth(4f);
        mPaint2.setStyle(Paint.Style.STROKE);


        bPson = BitmapFactory.decodeResource(getResources(), R.drawable.pson);
        bMap1 = BitmapFactory.decodeResource(getResources(), R.drawable.mapping1);
        bMap2 = BitmapFactory.decodeResource(getResources(), R.drawable.mapping2);
        bDet = BitmapFactory.decodeResource(getResources(), R.drawable.det);
        bBasic= BitmapFactory.decodeResource(getResources(), R.drawable.basic);
    }


}
