package com.example.tank.plantprotectionrobot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
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

    //矩阵变换
    private Matrix matrix = new Matrix();
    //默认地图比例
    private  int mapRatio;

    //路径文件
    private ArrayList<ArrayList<GpsPoint>> routeList=null;
    //匹配标记，位置对应路径文件，=0未匹配=1已匹配
    private ArrayList<Integer> matchFlagList = null;

    //匹配后正在作业的路径,已完成部分
    ArrayList<GpsPoint> ctrRobotRouteGray=null;


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
     //   Log.d("Tank001","偏移X="+movePoint.x+"Y="+movePoint.y+"\n");

    //    Path path0 = new Path();
     //   path0.addCircle(200,200,50,Path.Direction.CW);
     //   canvas.drawPath(path0, greenPaint);


        if(routeList !=null && matchFlagList !=null){

            for(int i=0;i<routeList.size();i++){

                Path path = new Path();

                if(0 == matchFlagList.get(i)) {

                    for (int index = 0; index < routeList.get(i).size(); index++) {

                        Log.d("Tank001","未匹配坐标：X="+routeList.get(i).get(index).x+" Y="+routeList.get(i).get(index).y+"\n");
                        if (index == 0) {
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
        //绘制正在作业已完成的部分
        if(ctrRobotRouteGray !=null){
            Path path = new Path();
            for (int index = 0; index < ctrRobotRouteGray.size(); index++) {

                if (index == 0) {
                    path.moveTo((int) (mapRatio * ctrRobotRouteGray.get(index).x), (int) (mapRatio * ctrRobotRouteGray.get(index).y));
                } else if (index == ctrRobotRouteGray.size() - 1) {
                    path.moveTo((int) (mapRatio * ctrRobotRouteGray.get(index).x), (int) (mapRatio * ctrRobotRouteGray.get(index).y));
                } else {
                    path.lineTo((int) (mapRatio * ctrRobotRouteGray.get(index).x), (int) (mapRatio * ctrRobotRouteGray.get(index).y));
                }

            }
            path.close();
            canvas.drawPath(path, grayPaint);
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

    }

    /***
     * 绘制一次性路径，即是不需要实时刷新的
     * @param routeList
     * @param isMatch
     */
    public void drawMatchRoute(ArrayList<ArrayList<GpsPoint>> routeList,ArrayList<Integer> isMatch){
        this.routeList=routeList;

        this.matchFlagList =isMatch;
        ctrRobotRouteGray=null;
        invalidate();

    }

    /***
     * 绘制正在作业已完成部分
     * @param ctrRobotRouteGray
     */
    public void controlRobotRoute(ArrayList<GpsPoint> ctrRobotRouteGray){
        this.ctrRobotRouteGray = ctrRobotRouteGray;
        routeList =null;
        matchFlagList =null;
        invalidate();
    }

    /***
     * 机器人和作业人员的位置
     * @param robotPosition
     * @param personPosition
     */
    public void setRobotAndPersonPosition(GpsPoint robotPosition,GpsPoint personPosition){
        this.robotPosition=robotPosition;
        this.personPosition =personPosition;
        invalidate();
    }

}
