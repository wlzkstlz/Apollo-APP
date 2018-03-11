package com.example.tank.plantprotectionrobot.Robot;

import android.graphics.PointF;

import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.WaveFiltering.RobotCruisePath;

import java.util.ArrayList;

/**工作匹配，机器人与果园匹配、路径匹配
 * Created by TK on 2018/2/23.
 */

public class WorkMatch {
    public String orchardName;//果园名称
    public String routeName;  //路径名
    public boolean isMatch;  //是否匹配
    public RobotCruisePath matchPath;//匹配的路径
    public ArrayList<GpsPoint> matchScreenRoute;//相对屏幕路径
    public int index;//工作完成进度

    public WorkMatch(){
        //默认值
        orchardName="";
        routeName="";
        isMatch = false;
        index =0;
        matchPath = null;
        matchScreenRoute =null;

    }

}
