package com.example.tank.plantprotectionrobot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.example.tank.plantprotectionrobot.appdata.ListviewAdapterOne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobDataActivity extends AppCompatActivity {

    private Button centerBtn;
    private ListView listView;
    private List<Map<String, Object>> dataList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_data);
        centerBtn = (Button)findViewById(R.id.button1);
        listView = (ListView)findViewById(R.id.listJobData);

    //    dataList = getData();
      //  listView.setAdapter(new ListviewAdapterOne(this, dataList));


        centerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 15; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            if(0 == i){
                map.put("mac_number", "总计");
            }else if(i<10){
                map.put("mac_number", "0" + i);
            }else{
                map.put("mac_number", ""+ i);
            }
            map.put("mac_task", "66");
            map.put("mac_pesticides", "66");
            map.put("mac_power", "666");
            map.put("mac_state", "张三");
            list.add(map);
        }
        return list;
    }
}
