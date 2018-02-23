package com.example.tank.plantprotectionrobot;

import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;

/**工作模式下绘制地图控件
 * Created by TK on 2018/2/23.
 */

public class WorkMapView {

    private GpsPoint robotPositionGreen = new GpsPoint();    //在线时的当前位置，掉线时在线最后位置
    private GpsPoint robotPositionRead = new GpsPoint();     //掉线时的当前位置
    private GpsPoint peoplePosition = new GpsPoint();        //人的位置，手机定位

}
