package com.example.tank.plantprotectionrobot;

import android.app.Activity;
import android.app.Service;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import com.example.tank.plantprotectionrobot.BLE.BLEService;

/**手机振动控制
 * Created by TK on 2018/1/25.
 */


public class VibrationAndMusic {
    private   boolean vibratorWork;//false，true正在振动
    private   boolean musicWork;//true正在播放
    private Vibrator vib ;

    //音乐播放
    private MediaPlayer mediaPlayer;

    public  VibrationAndMusic(Activity activity){
        vibratorWork = false;
        musicWork =false;
        vib = (Vibrator) activity.getSystemService(Service.VIBRATOR_SERVICE);
        mediaPlayer = MediaPlayer.create(activity, R.raw.startmusic);
    }


    /***
     *
     * @return 获取振动状态
     */
    public boolean getVibrate(){
        return vibratorWork;
    }

    /***
     * 获取音乐播放状态
     */
    public boolean getMusicState(){
        return musicWork;
    }

    public void  stopVibration(){
        vib.cancel();

        vibratorWork =false;

    }

    /**
     * final Activity activity ：调用该方法的Activity实例
     * long pattern ：震动的时长，单位是毫秒
     */
    public  void Vibrate(long milliseconds) {
        vib.vibrate(milliseconds);
        vibratorWork =true;

    }

    /**
     *
     * <strong>@param </strong>activity 调用该方法的Activity实例
     * <strong>@param </strong>pattern long[] pattern ：自定义震动模式 。数组中数字的含义依次是[静止时长，震动时长，静止时长，震动时长。。。]时长的单位是毫秒
     * <strong>@param </strong>isRepeat 是否反复震动，如果是true，反复震动，如果是false，只震动一次
     */
    public void Vibrate(long[] pattern, boolean isRepeat) {

        vib.vibrate(pattern, isRepeat ? 1 : -1);
        vibratorWork =true;

    }

    /***
     * 关闭播放器资源
     */
    public void deletMediaPlayer(){
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
    /***
     *  播放音乐
     * @param play 播放音乐标志 true 播放
     * @return 播放货关闭成功
     */
    public void playmusic(boolean play){

        if(play == true) {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            //开始播放
            mediaPlayer.start();
            //是否循环播放
            mediaPlayer.setLooping(true);

            musicWork =true;
        }else{
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }
            musicWork =false;
        }

    }
}
