package com.example.tank.plantprotectionrobot.Robot;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by TK on 2018/2/7.
 * 分时轮询管理
 */

public class PollingManagement {

    private TimerTask timerTask;
    private Timer timer = new Timer();
    private int timeChip;   //轮询时间片段
    private int timeCount; //轮询计时
    private int  pollTotalTime;//轮询总时间
    private PollCallback pollCallback;//回调接口

    private final int TIMECHIP = 500; //时间碎片默认默认值ms
 //   private final int TIMECHIP = 1000; //时间碎片默认默认值ms
    private final int TIME_MIN = 10;//定时器间隔时间ms
    private final int POLLING_MINT=5;//轮询最小周期，即5个时间片段为一个周期


    private boolean haveWorkMapPageCtr;//true有进入工作地图界面的机器人

    public PollingManagement(){

        haveWorkMapPageCtr=false;
        timeCount=0;
        pollTotalTime=0;
        timeChip = TIMECHIP/TIME_MIN;
        //分时轮询
        timerTask = new TimerTask() {
            @Override
            public void run() {

                if(pollTotalTime>0) {
                    timeCount++;
                    if (haveWorkMapPageCtr == true) {//有手动控制

                        if (timeCount % timeChip == 0 && timeCount < pollTotalTime) {
                            pollCallback.askInWorkMapRobot();//将时间分配给进入工作地图界面的机器人
                        } else if (timeCount >= pollTotalTime) {
                            pollCallback.askInCenterRobot(); //将时间分给控制中心界面机器人
                            timeCount = 0;
                        }

                    } else {//无手动控制

                        if (timeCount % timeChip == 0) {
                            pollCallback.askInCenterRobot();//无手动控制时时间全分给自动驾驶
                            if (timeCount >= pollTotalTime) {
                                timeCount = 0;
                            }
                        }
                    }
                }
            }

        };
        timer.schedule(timerTask,0,TIME_MIN);

    }

    public void setPollCallback(PollCallback pollCallback){
       this.pollCallback = pollCallback;
    }

    public void stopPolling(){
        timerTask.cancel();
    }

    public void runPolling(){
        timerTask.run();
    }

    /***
     *@param poll_T 轮询周期，是碎片时间的倍数，碎片时间是lora一个信号来回的时间
     */
    public boolean setPolling(int poll_T){
        if(poll_T>=POLLING_MINT){
            this.pollTotalTime = poll_T*timeChip;
        }else{
            return false;
        }

        return true;
    }

    /***
     * 分时轮询，在接收到返回信息后直接进入下一轮，问答
     */
    public void onPollingNext(){
        //timeCount = timeChip;
        timeCount = (timeCount/timeChip)*timeChip+timeChip-1;//下一组轮询
    }

    public void setHaveWorkMapPageCtr(boolean mapPageCtr){
        haveWorkMapPageCtr = mapPageCtr;
        Log.d("Tank001","PollingManagement->"+mapPageCtr);
    }

    /***
     * 分时轮询回调函数
     */
    public interface PollCallback {
        void askInCenterRobot();
        void askInWorkMapRobot();
    }

}
