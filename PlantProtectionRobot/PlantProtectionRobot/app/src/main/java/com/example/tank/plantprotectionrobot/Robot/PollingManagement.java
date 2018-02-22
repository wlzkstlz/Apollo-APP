package com.example.tank.plantprotectionrobot.Robot;

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

    private final int TIMECHIP = 400; //时间碎片默认默认值ms
    private final int TIME_MIN = 10;//定时器间隔时间ms
    private final int POLLING_MINT=5;//轮询最小周期，即5个时间片段为一个周期


    public boolean haveHandleCtr;//手动控制

    public PollingManagement(){

        haveHandleCtr=false;
        timeCount=0;
        pollTotalTime=0;//总时间2秒
        timeChip = TIMECHIP/TIME_MIN;
        //分时轮询
        timerTask = new TimerTask() {
            @Override
            public void run() {

                if(pollTotalTime>0) {
                    timeCount++;
                    if (haveHandleCtr == true) {//有手动控制
                        if (timeCount % timeChip == 0 && timeCount < pollTotalTime) {
                            pollCallback.askHanldeRobot();//将时间分配给手动控制
                        } else if (timeCount >= pollTotalTime) {
                            pollCallback.askAutoRobot(); //将时间分给自动控制
                            timeCount = 0;
                        }

                    } else {//无手动控制

                        if (timeCount % timeChip == 0) {
                            pollCallback.askAutoRobot();//无手动控制时时间全分给自动驾驶
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
     *@param pollTotalTime 轮询周期，是碎片时间的倍数，碎片时间是lora一个信号来回的时间
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
    public void onPollingAsk(){
        timeCount = timeChip;
    }

    /***
     * 分时轮询回调函数
     */
    public interface PollCallback {
        void askAutoRobot();
        void askHanldeRobot();
    }

}
