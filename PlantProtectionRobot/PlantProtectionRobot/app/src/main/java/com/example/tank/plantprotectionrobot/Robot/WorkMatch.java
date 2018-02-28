package com.example.tank.plantprotectionrobot.Robot;

/**工作匹配，机器人与果园匹配、路径匹配
 * Created by TK on 2018/2/23.
 */

public class WorkMatch {
    public String orchardName;//果园名称
    public String routeName;  //路径名
    public boolean isMatch;  //是否匹配

    public WorkMatch(){
        //默认值
        orchardName="";
        routeName="";
        isMatch = false;

    }

}
