package com.example.tank.plantprotectionrobot;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.tank.plantprotectionrobot.CenterControlActivity;
import com.example.tank.plantprotectionrobot.ChoicePage.MySpinnerAdapter;
import com.example.tank.plantprotectionrobot.R;

import java.util.ArrayList;
import java.util.List;

/*
 @ 工作地图界面，以车位中心，每次显示一台车
 */
public class WorkMapActivity extends AppCompatActivity {

    private Button centerBtn;
    private Button startBtn;


    private Intent centerCtr;
    private Spinner spinner1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_map);

        centerBtn = (Button)findViewById(R.id.button1);
        startBtn = (Button)findViewById(R.id.button2);
        spinner1=(Spinner) findViewById(R.id.spinner1);
       //控制中心界面
       centerCtr = new Intent(this,CenterControlActivity.class);

        setOnClick();
        initData();

    }

    private void setOnClick() {
        centerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(centerCtr);
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setVisibility(View.INVISIBLE);
                spinner1.setVisibility(View.VISIBLE);
            }
        });

    }
    /*
*初始化数据
*/
    private void initData() {

        //获取字符
        String [] ctr= (String[])getResources().getStringArray(R.array.carCtr_spinner);

        //填充到spinner
        MySpinnerAdapter adapter;
        adapter = new MySpinnerAdapter(this,
                android.R.layout.simple_spinner_item, ctr);
        spinner1.setAdapter(adapter);
    }

}
