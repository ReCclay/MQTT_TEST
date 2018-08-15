package com.mqtt.test;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ServiceMqtt extends Service {

    private String TelephonyIMEI="";//获取手机IMEI号
    private MqttClient mqttClient;
    private MqttConnectOptions mqttConnectOptions;
    String MqttUserString = "";//用户名
    String MqttPwdString = "";//密码
    String MqttServerURI = "";

    //生成一个HandlerThread对象
    HandlerThread handlerThread = new HandlerThread("handler_thread");
    MyHandler mHandler;

    @Override
    public void onCreate() {

        //启动后台，进行创建mHandler以及所需要的线程.
        handlerThread.start();//上面已经实例化好了handlerThread对象，供mHandler传入构造方法使用
        mHandler = new MyHandler(handlerThread.getLooper());
        //发送字段1，表示要进行连接
        Message msg = mHandler.obtainMessage();//import android.os.Handler; 实现
        msg.what = 1;
        mHandler.sendMessageDelayed(msg, 1);

        //活动到MQTT广播的接收端
        IntentFilter filter = new IntentFilter();//监听的广播
        filter.addAction("ActivitySendMqttService");
        registerReceiver(ServiceMqttReceiver, filter);

        super.onCreate();
    }

    /*该类的广播接收程序*/
    private BroadcastReceiver ServiceMqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try
            {
                String msgString = intent.getStringExtra("OtherActivitySend");//getStringExtra和putExtra配套使用 //这里是获取OtherActivitySend键值的内容
                String tempString[] = msgString.split(";;");//两个引号分隔字符串，分隔后的结果存入数组中

                //如果开头是ResetMqtt，就设置字段重新连接
                if (tempString[0].equals("ResetMqtt")) {//如果分号分隔符前面是"ResetMqtt"
                    Message msg = mHandler.obtainMessage();//创建一个Message对象
                    msg.what = 1;//写入字段值
                    mHandler.sendMessageDelayed(msg, 10);//延时后发送Message对象
                    Toast.makeText(getApplicationContext(), "正在重新启动MQTT", Toast.LENGTH_SHORT).show();
                }
                ////如果开头是SendData，就把接收的内容发送到设置的主题
                else if (tempString[0].equals("SendData")) {//如果分号分隔符前面是"SendData"
                    MqttMessage msgMessage = new MqttMessage(tempString[1].getBytes());//实例一个MqttMessage对象，括号参数对应内容，getBytes把string 转为 byte。
                    try {
                        mqttClient.publish(MainActivity.PublishString,msgMessage);//发布内容
                    } catch (MqttPersistenceException e) {
//                        Log.e("err",e.toString());
                    } catch (MqttException e) {
//                        Log.e("err",e.toString());
                    }
                }

            } catch (Exception e) {
            }
        }
    };


    /*初始化Mqtt连接*/
    private void InitMqttConnect()
    {
        try
        {
            MqttUserString = MainActivity.MqttUserString;//主活动定义的用户名
            MqttPwdString = MainActivity.MqttPwdString;//主活动定义的密码
            MqttServerURI = "tcp://"+MainActivity.MqttIPString+":"+MainActivity.MqttPort;//主活动定义的IP+PROT

            TelephonyIMEI =  getDeviceId(getApplicationContext())+"MqttDemo";//ClientID
            mqttClient = new MqttClient(MqttServerURI,TelephonyIMEI,new MemoryPersistence());//MemoryPersistence-记忆持久性
            mqttConnectOptions = new MqttConnectOptions();//MQTT的连接设置
            mqttConnectOptions.setCleanSession(true);//设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            mqttConnectOptions.setUserName(MqttUserString);//设置连接的用户名
            mqttConnectOptions.setPassword(MqttPwdString.toCharArray());//设置连接的密码
            mqttConnectOptions.setConnectionTimeout(10);// 设置连接超时时间 单位为秒
            mqttConnectOptions.setKeepAliveInterval(5);// 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制

            mqttClient.setCallback(new MqttCallback() {//接收到消息之后的回调函数
                @Override
                public void messageArrived(String arg0, MqttMessage arg1) throws Exception {//throws Exception - 在当前方法不知道该如何处理该异常时，则可以使用throws对异常进行抛出给调用者处理或者交给JVM。
                    // TODO Auto-generated method stub

                    //发送端广播，在MainActivity.java中接收
                    Intent intent = new Intent();
                    intent.setAction("Broadcast.MqttServiceSend");//这两句可以合体为Intent intent = new Intent("Broadcast.MqttServiceSend");
                    intent.putExtra("MqttServiceSend",arg0+";;"+arg1.toString());//puExtra遵循键值对的写法，可让广播携带数据
                    sendBroadcast(intent);//发送广播
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken arg0) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void connectionLost(Throwable arg0) {
                    // TODO Auto-generated method stub

                    //连接失败也设置对应的字段！
                    Message msg = mHandler.obtainMessage();
                    msg.what = 1;
                    mHandler.sendMessageDelayed(msg, 3000);
                }
            });
        }
        catch (Exception e) {

        }
    }

    //定义类
    class MyHandler extends Handler {
        public MyHandler() {//MyHandler的构造方法

        }
        public MyHandler(Looper looper){//MyHandler的构造方法
            super(looper);
        }
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1)//如果传回来what的字段是1，断开并重新连接！
            {
                try
                {//断开MQTT
                    try {mqttClient.disconnect();} catch (Exception e) {}
                    try {mqttClient.close();} catch (Exception e) {}
                    Thread.sleep(1000);//挂起线程1s

                    InitMqttConnect();//初始化MQTT配置
                    mqttClient.connect(mqttConnectOptions);//连接MQTT
                    mqttClient.subscribe(MainActivity.SubscribeString,0);//进行主题订阅

                } catch (MqttSecurityException e) {//已经连接了,
                } catch (MqttException e) {//连接时没有网络,什么原因造成的连接不正常,正在进行连接
                } catch (Exception e) {
                }

                try
                {
                    if (mqttClient.isConnected() == false)//没连接上，再次设置字段
                    {
                        Message msg1 = mHandler.obtainMessage();//注意这里是msg1
                        msg1.what = 1;
                        mHandler.sendMessageDelayed(msg1, 3000);//延时3s后发送
                    }
                }
                catch (Exception e)
                {}
            }
        }
    }

    /*获取手机IMEI号*/
    private static String getDeviceId(Context context) {
        String id = "test";
        //android.telephony.TelephonyManager
        TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (mTelephony.getDeviceId() != null)
            {
                id = mTelephony.getDeviceId();
            } else {
                //android.provider.Settings;
                id = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }
        return id;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}
