package com.example.tank.plantprotectionrobot;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.tank.plantprotectionrobot.ChoicePage.ConnectRTK;
import com.example.tank.plantprotectionrobot.ChoicePage.NoScrollViewPager;
import com.example.tank.plantprotectionrobot.ChoicePage.SetMapName;
import com.example.tank.plantprotectionrobot.appdata.FragmentAdapter;


import java.util.ArrayList;
import java.util.List;

import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.initLocationPermission;
import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.verifyStoragePermissions;


/*
 @新建地图城市选择
 */

public class ChoiveActivity extends AppCompatActivity{

    private List<Fragment> fragmentList;
    private FragmentAdapter appPageAdapter;
    NoScrollViewPager viewPager;

    private SetMapName setMapName = new SetMapName();
    private ConnectRTK connectRTK = new ConnectRTK();

    private final int CHOIVEPAGE2 = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choive);
        viewPager = (NoScrollViewPager)findViewById(R.id.viewPager);

        fragmentList=new ArrayList<Fragment>();
        fragmentList.add(setMapName);
        fragmentList.add(connectRTK);
        appPageAdapter = new FragmentAdapter(getSupportFragmentManager(), fragmentList);

        viewPager.setAdapter(appPageAdapter);


       setMapName.setOnFragmentClickListener(new SetMapName.OnFragmentClickListener() {
           @Override
           public void onFragmentClick(View view) {
               viewPager.setCurrentItem(CHOIVEPAGE2);
           }
       });


    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}
