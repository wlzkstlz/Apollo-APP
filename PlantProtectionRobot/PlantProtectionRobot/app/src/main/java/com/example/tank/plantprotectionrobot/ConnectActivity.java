package com.example.tank.plantprotectionrobot;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.ChoicePage.ConnectRTK;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.Robot.CommondType;
import com.example.tank.plantprotectionrobot.Robot.TankRobot;

import java.util.ArrayList;

public class ConnectActivity extends AppCompatActivity {

    private Button connnetBtn;
    private EditText editText;
    private ProgressDialog progressDialog;//等待对话框
    private Handler mHandler;

    private TankRobot tankRobot;
    //BLE service
    private ConnectActivity.BleServiceConn bleServiceConn;
    private BLEService.BleBinder binder = null;
    private Intent intentSev;

    //消息标志
    private final int ROBOT_ADD_SUCCESS = 10;
    private final int ROBOT_ADD_FAIL=20; //添加失败
    private final int ROBOT_NOMSG_BACK=0;//没有收到回复

    ArrayList<TankRobot> workRobotList;


    private final  String TAG = "Tank001";

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        connnetBtn = (Button)findViewById(R.id.button1);
        editText = (EditText)findViewById(R.id.editText1);

       initProgressDialog();//初始化进度条
       //开启蓝牙Service
       intentSev = new Intent(this, BLEService.class);
       connnetBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {

               addRobot();
           //    finish();
           }
       });


       mHandler=new Handler(){
           @Override
           public void handleMessage(Message msg) {

               ArrayList<TankRobot> robotList = new ArrayList<TankRobot>();
               //消息提示
               AlertDialog.Builder msgDialog =
                       new AlertDialog.Builder(ConnectActivity.this);

               switch (msg.what){
                   case ROBOT_ADD_FAIL://添加失败

                    //   progressDialog.cancel();
                       Log.d(TAG,"ConnectActivity->添加机器人失败");
                       if(workRobotList.size()>1) {
                           for (int i = 0; i < workRobotList.size()-1; i++) {
                               robotList.add(workRobotList.get(i));
                           }
                           //机器人不存在，删除添加的
                           workRobotList.clear();
                           for(int i=0;i<robotList.size();i++) {
                               workRobotList.add(robotList.get(i));
                           }
                       }

                       if(1 == workRobotList.size()){
                           workRobotList.clear();
                       }

                       msgDialog.setTitle("提示");
                       msgDialog.setMessage("编号"+tankRobot.heatDataMsg.robotId+"机器人不存在");
                       msgDialog.show();

                       break;
                   case ROBOT_ADD_SUCCESS:
                       progressDialog.cancel();
                       mHandler.removeMessages(ROBOT_NOMSG_BACK);
                       Log.d(TAG,"ConnectActivity->添加机器人成功");
                       finish();
                       break;
                   case ROBOT_NOMSG_BACK:

                       progressDialog.cancel();

                       if(workRobotList.size()>1) {
                           for (int i = 0; i < workRobotList.size()-1; i++) {
                               robotList.add(workRobotList.get(i));
                           }
                           //机器人不存在，删除添加的
                           workRobotList.clear();
                           for(int i=0;i<robotList.size();i++) {
                               workRobotList.add(robotList.get(i));
                           }
                       }

                       if(1 == workRobotList.size()){
                           workRobotList.clear();
                       }

                       msgDialog.setTitle("提示");
                       msgDialog.setMessage("编号"+tankRobot.heatDataMsg.robotId+"机器人不存在");
                       msgDialog.show();

                       break;
               }
               super.handleMessage(msg);
           }
       };

    }

    private void addRobot(){
        String check = "\\d{5}";
        if(editText.getText().toString().matches(check)) {//是否为5位数
            int robotId = Integer.parseInt(editText.getText().toString());
            boolean addFlag =true;
            if (robotId < 65536) {//是否在编号范围内
                for(int i=0;i<workRobotList.size();i++) {
                    if(workRobotList.get(i).heatDataMsg.robotId == robotId) {
                        Toast.makeText(this, "该编号机器人已经添加" ,
                                Toast.LENGTH_SHORT).show();
                        addFlag = false;//添加失败
                        break;
                    }
                }
                if(addFlag == true) {
                    tankRobot = new TankRobot(robotId);
                    //添加机器人
                    if(binder !=null){
                        tankRobot.checkCount=4;
                        binder.addWorkRobot(tankRobot);
                    }
                    progressDialog.show();
                    mHandler.sendEmptyMessageDelayed(ROBOT_NOMSG_BACK, 4000);
                }



            }else{
                Toast.makeText(this, "请输入正确的机器人编号" ,
                        Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "请输入正确的机器人编号" ,
                    Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onStart() {

        bleServiceConn = new ConnectActivity.BleServiceConn();
        bindService(intentSev, bleServiceConn, Context.BIND_AUTO_CREATE);

        super.onStart();
    }

    private void initProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("提示");
        progressDialog.setMessage("正在添加机器人");
        progressDialog.setCancelable(false);
    }
    /***
     * service连接
     */
    class BleServiceConn implements ServiceConnection {
        // 服务被绑定成功之后执行
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // IBinder service为onBind方法返回的Service实例
            binder = (BLEService.BleBinder) service;

            //绑定后执行动作
            binder.setBleWorkTpye(BLEService.BLE_HANDLE_CONECT);
            binder.startScanBle();//搜索蓝牙
            workRobotList = binder.getRobotList();

            binder.getService().setRobotWorkingCallback(new BLEService.RobotWorkingCallback() {
                @Override
                public void RobotStateChanged(TankRobot tankRobot) {

                    if(tankRobot.isWorking == false) {//刚添加的机器人，判断是否在线
                        if (tankRobot.checkCount == 0) { //连接成功
                            mHandler.sendEmptyMessage(ROBOT_ADD_SUCCESS);

                        } else {
                            mHandler.sendEmptyMessage(ROBOT_ADD_FAIL);
                        }
                        if(workRobotList.size()>0){
                            workRobotList.get(workRobotList.size()-1).isWorking =true;
                        }
                    }


                }

                @Override
                public void BleStateChanged(int msg) {
                    switch(msg){
                        case BLEService.BLE_CONNECT_OFF:
                            //掉线重连
                            binder.connectBle(null,true);
                            break;
                        case BLEService.BLE_CONNECT_ON:
                            break;
                    }
                }

                @Override
                public void BleScanChanged(ArrayList<BluetoothDevice> mBleDeviceList) {
                    if (mBleDeviceList.size()>0) {
                        //连接蓝牙
                        binder.connectBle(mBleDeviceList.get(0),false);
                        Log.d(TAG,"ConnectActivity->连接蓝牙"+mBleDeviceList.get(0).getName());
                    }
                }

                @Override
                public void BleConnectedDevice(BluetoothDevice connectedDevice) {
                   if(connectedDevice == null){
                  //     binder.startScanBle();
                   }

                }
            });

        }

        // 服务奔溃或者被杀掉执行
        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    }
}
