package com.example.tank.plantprotectionrobot;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.BLE.BluetoothLeClass;
import com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool;
import com.example.tank.plantprotectionrobot.appdata.ListviewAdapterOne;
import com.example.tank.plantprotectionrobot.appdata.ListviewAdapterTwo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.initLocationPermission;
import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.verifyStoragePermissions;

/*
 @总控台界面
 */


public class CenterControlActivity extends AppCompatActivity {

    private static boolean isExit = false;

    private Button workBtn;
    private Button listBtn;
    private Button addBtn1;
    private Button addBtn2;
    private Button deleBtn;
    private Button dleOkBtn;

    private ListView listView;
    private List<Map<String, Object>> dataList;

    private Intent workData;
    private Intent orchardList;
    private Intent connectMac;
    private Intent workMap;

    //蓝牙
    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;
    /**蓝牙管理器**/
    private BluetoothManager bluetoothManager;

    //开启蓝牙Service
    //开启蓝牙Service
    private Intent intentSev;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_center_control);
        workBtn = (Button) findViewById(R.id.button1);
        listBtn = (Button) findViewById(R.id.button2);
        addBtn1 = (Button) findViewById(R.id.button3);
        addBtn2 = (Button) findViewById(R.id.button5);
        deleBtn = (Button) findViewById(R.id.button4);
        dleOkBtn = (Button) findViewById(R.id.button6);
        dleOkBtn.setVisibility(View.INVISIBLE);
        addBtn1.setVisibility(View.INVISIBLE);
        listView = (ListView) findViewById(R.id.listview1);

        //需要启动的activity
        workData = new Intent(this, JobDataActivity.class);
        orchardList = new Intent(this, OrchardListActivity.class);
        connectMac = new Intent(this, ConnectActivity.class);
        workMap = new Intent(this,WorkMapActivity.class);

        //蓝牙后台服务
        intentSev = new Intent(this, BLEService.class);
        startService(intentSev);

        dataList = getData();
        listView.setAdapter(new ListviewAdapterOne(this, dataList));

        setOnClick();//按键监听

    }

    @Override
    protected void onPostResume() {

        startService(intentSev); //启动蓝牙后台
        super.onPostResume();
    }

    private void setOnClick() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("debug001","你点的是"+i);
                startActivity(workMap);
            }
        });
        workBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(workData);
            }
        });
        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(orchardList);
            }
        });

        addBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(connectMac);
            }
        });
        addBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(connectMac);
            }
        });
        deleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listView.setAdapter(new ListviewAdapterTwo(CenterControlActivity.this, dataList));
                dleOkBtn.setVisibility(View.VISIBLE);
                addBtn2.setVisibility(View.INVISIBLE);
                deleBtn.setVisibility(View.INVISIBLE);
            }
        });

        dleOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listView.setAdapter(new ListviewAdapterOne(CenterControlActivity.this, dataList));
                dleOkBtn.setVisibility(View.INVISIBLE);
                addBtn2.setVisibility(View.VISIBLE);
                deleBtn.setVisibility(View.VISIBLE);

            }
        });
    }

    public List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 15; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            if(i>9){
                map.put("mac_number", "" + i);
                map.put("mac_task", "" + i + "%");
                map.put("mac_pesticides", "" + i + "%");
                map.put("mac_power", "" + i + "%");
                map.put("mac_state", "作业");
            }else {
                map.put("mac_number", "0" + i);
                map.put("mac_task", "6" + i + "%");
                map.put("mac_pesticides", "6" + i + "%");
                map.put("mac_power", "6" + i + "%");
                map.put("mac_state", "作业");
            }
            list.add(map);
        }
        return list;
    }

   private Handler mHandler = new Handler(){
       @Override
       public void handleMessage(Message msg) {
           super.handleMessage(msg);
           isExit = false;
       }
   };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            // 利用handler延迟发送更改状态信息
            mHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            Intent startMain = new Intent(Intent.ACTION_MAIN); //指定跳到系统桌面
            startMain.addCategory(Intent.CATEGORY_HOME);
            startActivity(startMain); //开始跳转
        //    System.exit(0);
        }
    }

    @Override
    protected void onStart() {
        verifyStoragePermissions(this) ;//针对6.0以上版本做权限适配
        initLocationPermission(this);
        super.onStart();
    }
}