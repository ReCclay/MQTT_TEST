package com.mqtt.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //public的类变量
    public static String MqttUserString = "yang";//用户名
    public static String MqttPwdString = "11223344";//密码
    public static String MqttIPString = "47.93.19.134";//IP地址
    public static int MqttPort = 1883;//端口号
    public static String SubscribeString = "/pub";//订阅的主题
    public static String PublishString = "/sub";//发布的主题


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(btnClickListener);

        IntentFilter filter = new IntentFilter();//监听的广播
        filter.addAction("Broadcast.MqttServiceSend");
        registerReceiver(MainActivityReceiver, filter);

    }

    private View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //活动到MQTT广播的发送端(点下按钮)
            Intent intent = new Intent();
            intent.setAction("ActivitySendMqttService");
            intent.putExtra("OtherActivitySend","SendData;;"+"1234321");
            sendBroadcast(intent);
        }
    };


    /** 当活动即将可见时调用 */
    @Override
    protected void onStart()
    {
        Intent startIntent = new Intent(getApplicationContext(), ServiceMqtt.class);
        startService(startIntent); //启动后台服务

        super.onStart();
    }

    /*该类的广播接收程序*/
    private BroadcastReceiver MainActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String msgString = intent.getStringExtra("MqttServiceSend");//键值对接收
            Toast.makeText(context, "Receive;;"+msgString, Toast.LENGTH_SHORT).show();
        }
    };



}
