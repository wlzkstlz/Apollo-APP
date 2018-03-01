package com.example.tank.plantprotectionrobot.WaveFiltering;
import android.graphics.PointF;

/**
 * Created by 54391 on 2018/2/7.
 */

public class CommonAlg {
    
    public static double GetThetaFromVector(PointF point_vector)
    {
        double x=point_vector.x;
        double y=point_vector.y;
        double theta;
        if (x==0)
        {
            theta=y>0?0.5*Math.PI:1.5*Math.PI;
        }
        else if (y==0)
        {
            theta=x>0?0:Math.PI;
        }
        else if (x>0)
        {
            theta=Math.atan(y/x);
            theta=theta>=0?theta:(theta+2.0*Math.PI);
        }
        else
        {
            theta=Math.atan(y/x);
            theta=theta+Math.PI;
        }
        return theta;
    }
    public static void RotateTheta4Point(PointF pt,double theta)
    {
        PointF pt_temp=new PointF(pt.x,pt.y);
        pt.x=(float)( pt_temp.x*Math.cos(theta)-pt_temp.y*Math.sin(theta));
        pt.y=(float)( pt_temp.x*Math.sin(theta)+pt_temp.y*Math.cos(theta));
    }

}
