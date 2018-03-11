package com.example.tank.plantprotectionrobot.DataProcessing;

/**
 * Created by TK on 2018/1/22.
 */

public class GpsPoint {
    public double x;//经度
    public double y;//纬度
    public double z;//海拔
    public double d;//方向

    public void set(double x,double y,double z,double d){
        this.x=x;
        this.y=y;
        this.z=z;
        this.d=d;
    }
}
