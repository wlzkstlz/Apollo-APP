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
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;

import java.util.ArrayList;

/**工作模式下绘制地图控件
 * Created by TK on 2018/2/23.
 */

public class WorkMapView extends View {

    private GpsPoint robotPosition = null;    //机器人位置
    private GpsPoint personPosition = null;   //操作人员的位置，手机定位
    private GpsPoint basicPosition = null;   //基站位置
    //平移坐标
    private GpsPoint movePoint = new GpsPoint();

    private Bitmap bPerson;      //操作员的图标
    private Bitmap bRobotLog;    //机器人图标
    private Bitmap bBasicLog;    //基站图标

    private Paint grayPaint = new Paint();   //灰色画笔
    private Paint greenPaint = new Paint(); //绿色画笔
    private Paint bluePaint = new Paint(); //绿色画笔
    private Paint circlePaint = new Paint();//画圆形的画笔

    //矩阵变换
    private Matrix matrix = new Matrix();
    //默认地图比例
    private  int mapRatio;

    //路径文件
    private ArrayList<ArrayList<GpsPoint>> routeList=null;
    private ArrayList<ArrayList<GpsPoint>> routeListM=null;
    //匹配标记，位置对应路径文件，=0未匹配=1已匹配
    private ArrayList<Integer> matchFlagList = null;

    //匹配后正在作业的路径,已完成部分
    private ArrayList<GpsPoint> workRobotRoute=null;
    private int indexFlag;//工作时进度


    public WorkMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public WorkMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public WorkMapView(Context context) {

        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate((int)movePoint.x,(int)movePoint.y);
      //  Log.d("Tank001","倍数="+mapRatio+" MAP偏移X="+movePoint.x+" Y="+movePoint.y+" 位置X="+mapRatio * robotPosition.x+"Y="+mapRatio * robotPosition.y);

        if(routeListM !=null){
            for(int i=0;i<routeListM.size();i++){

                Path path = new Path();
                for (int index = 0; index < routeListM.get(i).size(); index++) {

                    if (index == 0) {
                        path.moveTo((int) (mapRatio * routeListM.get(i).get(index).x), (int) (mapRatio * routeListM.get(i).get(index).y));


                    } else if (index == routeListM.get(i).size() - 1) {
                        path.moveTo((int) (mapRatio * routeListM.get(i).get(index).x), (int) (mapRatio * routeListM.get(i).get(index).y));
                    } else {
                        path.lineTo((int) (mapRatio * routeListM.get(i).get(index).x), (int) (mapRatio * routeListM.get(i).get(index).y));
                    }

                }
                path.close();
                canvas.drawPath(path,bluePaint);
            }
        }
        //果园路径
        if(routeList !=null && matchFlagList !=null){

            for(int i=0;i<routeList.size();i++){

                Path path = new Path();

                if(0 == matchFlagList.get(i)) {

                    for (int index = 0; index < routeList.get(i).size(); index++) {

                   //     Log.d("Tank001","未匹配坐标：X="+routeList.get(i).get(index).x+" Y="+routeList.get(i).get(index).y+"\n");
                        if (index == 0) {

                            canvas.drawCircle((int) (mapRatio * routeList.get(i).get(index).x), (int) (mapRatio * routeList.get(i).get(index).y), 20,circlePaint);

                            path.moveTo((int) (mapRatio * routeList.get(i).get(index).x), (int) (mapRatio * routeList.get(i).get(index).y));
                        } else if (index == routeList.get(i).size() - 1) {
                            path.moveTo((int) (mapRatio * routeList.get(i).get(index).x), (int) (mapRatio * routeList.get(i).get(index).y));
                        } else {
                            path.lineTo((int) (mapRatio * routeList.get(i).get(index).x), (int) (mapRatio * routeList.get(i).get(index).y));
                        }

                    }

                    path.close();
                    canvas.drawPath(path, greenPaint);

                }else{

                    for (int index = 0; index < routeList.get(i).size(); index++) {

                     //   Log.d("Tank001","匹配坐标：X="+routeList.get(i).get(index).x+" Y="+routeList.get(i).get(index).y+"\n");
                        if (index == 0) {
                            path.moveTo((int) (mapRatio * routeList.get(i).get(index).x), (int) (mapRatio * routeList.get(i).get(index).y));
                        } else if (index == routeList.get(i).size() - 1) {
                            path.moveTo((int) (mapRatio * routeList.get(i).get(index).x), (int) (mapRatio * routeList.get(i).get(index).y));
                        } else {
                            path.lineTo((int) (mapRatio * routeList.get(i).get(index).x), (int) (mapRatio * routeList.get(i).get(index).y));
                        }

                    }
                    path.close();
                    canvas.drawPath(path, grayPaint);
                }
            }
        }

        //绘制正在作业的路径
        if(workRobotRoute !=null && indexFlag>0){
            Path path = new Path();
            //绘制正在作业已完成的部分
            for (int i = 0; i<indexFlag; i++) {

                if (i == 0) {
                    path.moveTo((int) (mapRatio * workRobotRoute.get(i).x), (int) (mapRatio * workRobotRoute.get(i).y));
                } else if (i == indexFlag - 1) {
                    path.moveTo((int) (mapRatio * workRobotRoute.get(i).x), (int) (mapRatio * workRobotRoute.get(i).y));
                } else {
                    path.lineTo((int) (mapRatio * workRobotRoute.get(i).x), (int) (mapRatio * workRobotRoute.get(i).y));
                }

            }
            path.close();
            canvas.drawPath(path, grayPaint);

            //未完成部分
            Path path1 = new Path();
            for(int i=indexFlag;i<workRobotRoute.size();i++){
                if (i == indexFlag) {
                    path1.moveTo((int) (mapRatio * workRobotRoute.get(i).x), (int) (mapRatio * workRobotRoute.get(i).y));
                } else if (i == workRobotRoute.size() - 1) {
                    path1.moveTo((int) (mapRatio * workRobotRoute.get(i).x), (int) (mapRatio * workRobotRoute.get(i).y));
                } else {
                    path1.lineTo((int) (mapRatio * workRobotRoute.get(i).x), (int) (mapRatio * workRobotRoute.get(i).y));
                }
            }
            path1.close();
            //显示任务进度

            if (indexFlag > 0 && indexFlag < workRobotRoute.size()) {
                String str = (indexFlag * 100 / workRobotRoute.size()) + "%";
                canvas.drawText(str, (float) workRobotRoute.get(indexFlag).x, (float) workRobotRoute.get(indexFlag).y, grayPaint);//显示进度
            }
            canvas.drawPath(path1, greenPaint);
        }else if(indexFlag == 0 && workRobotRoute !=null){
            if(workRobotRoute.size()>0){
                String str = ("0%");
                canvas.drawText(str,(float) workRobotRoute.get(indexFlag).x,(float)workRobotRoute.get(indexFlag).y,grayPaint);//显示进度
            }

        }


        //基站图标
        if(basicPosition !=null) {
            matrix.reset();
            matrix.postTranslate((int) (mapRatio * basicPosition.x - bBasicLog.getWidth() / 2), (int) (mapRatio * basicPosition.y - bBasicLog.getHeight() / 2));
            canvas.drawBitmap(bBasicLog, matrix, new Paint());
        }

        //机器人图标
        if(robotPosition !=null) {
            matrix.reset();
            matrix.postTranslate((int) (mapRatio * robotPosition.x - bRobotLog.getWidth() / 2), (int) (mapRatio * robotPosition.y - bRobotLog.getHeight() / 2));
            canvas.drawBitmap(bRobotLog, matrix, new Paint());
        }
        //操作人员图标
        if(personPosition !=null) {
            matrix.reset();
            matrix.postTranslate((int) (mapRatio * personPosition.x - bPerson.getWidth() / 2), (int) (mapRatio * personPosition.y - bPerson.getHeight() / 2));
            canvas.drawBitmap(bPerson, matrix, new Paint());
        }

    }


    /***
     * 初始化,使用一定要初始化
     */
    public void InitWorkMapView(int ratio,GpsPoint movePoint,GpsPoint basicPosition){

        this.mapRatio=ratio;
        this.movePoint=movePoint;
        this.basicPosition = basicPosition;

        bPerson = BitmapFactory.decodeResource(getResources(),R.drawable.mapping2);
        bRobotLog = BitmapFactory.decodeResource(getResources(), R.drawable.mapping1);
        bBasicLog = BitmapFactory.decodeResource(getResources(), R.drawable.basic);

        //初始化画笔
        grayPaint.setColor(getResources().getColor(R.color.colorGray));
        grayPaint.setStrokeWidth(4f);
        grayPaint.setStyle(Paint.Style.STROKE);

        greenPaint.setColor(getResources().getColor(R.color.tankgreen));
        greenPaint.setStrokeWidth(4f);
        greenPaint.setStyle(Paint.Style.STROKE);

        circlePaint.setColor(getResources().getColor(R.color.tankgreen));
        circlePaint.setStrokeWidth(4f);
        circlePaint.setStyle(Paint.Style.FILL);

        bluePaint.setColor(getResources().getColor(R.color.tankblue));
        bluePaint.setStrokeWidth(4f);
        bluePaint.setStyle(Paint.Style.STROKE);

    }

    /***
     * 绘制一次性路径，即是不需要实时刷新的
     * @param routeList
     * @param isMatch
     */
    public void drawMatchRoute(ArrayList<ArrayList<GpsPoint>> routeList,ArrayList<ArrayList<GpsPoint>> routeListM,ArrayList<Integer> isMatch){
        this.routeList=routeList;
        this.routeListM =routeListM;
        this.matchFlagList =isMatch;
        workRobotRoute=null;
        invalidate();

    }

    /***
     *
     * @param workRoute 这在作业的路径
     * @param routeListM 主干道
     * @param index 机器人当前位置
     */
    public void drawWorkRoute(ArrayList<GpsPoint> workRoute,ArrayList<ArrayList<GpsPoint>> routeListM,int index){
        this.routeListM =routeListM;
        this.routeList = null;
        this.workRobotRoute =workRoute;
        this.indexFlag = index;
        matchFlagList =null;
        invalidate();
    }

    /***
     * 机器人和作业人员的位置
     * @param robotPosition
     * @param personPosition
     */
    public void setRobotAndPersonPosition(GpsPoint robotPosition,GpsPoint personPosition,GpsPoint movePoint,int mapRatio){
        this.robotPosition=robotPosition;
        this.personPosition =personPosition;
        this.movePoint = movePoint;
        this.mapRatio=mapRatio;
        invalidate();
    }

}
