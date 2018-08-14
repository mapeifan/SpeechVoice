package com.aispeech.ddsdemo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.DDSAuthListener;
import com.aispeech.dui.dds.DDSConfig;
import com.aispeech.dui.dds.DDSInitListener;
import com.aispeech.dui.dds.agent.MessageObserver;
import com.aispeech.dui.dds.auth.AuthType;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.aispeech.dui.dsk.duiwidget.CommandObserver;

import org.json.JSONArray;
import org.json.JSONObject;

public class DDSService extends Service {
    public static final String TAG = "SBC_Service";
    private static String[] commands;
    private MyMessageObserver mMessageObserver;
    public DDSService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        commands = getResources().getStringArray(R.array.demo_actions);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return super.onStartCommand(intent, flags, startId);
    }


    private void init() {
        mMessageObserver = new MyMessageObserver();
        DDS.getInstance().setDebugMode(2);//在调试时可以打开sdk调试日志，在发布版本时，请关闭
        DDS.getInstance().init(getApplicationContext(), createConfig(), new DDSInitListener() {
            @Override
            public void onInitComplete(boolean isFull) {
                Log.e(TAG, "思必驰语音初始化成功");
                if (isFull) {
                    sendBroadcast(new Intent("ddsdemo.intent.action.init_complete"));

                    // 注册CommandObserver,用于处理DUI平台技能配置里的客户端动作指令, 同一个CommandObserver可以处理多个commands.
                    DDS.getInstance().getAgent().subscribe(commands, commandObserver);
                    DDS.getInstance().getAgent().subscribe(new String[]{"avatar.silence", "avatar.listening", "avatar.understanding", "avatar.speaking"}, mMessageObserver);

                    //开唤醒，调用后才能唤醒
                    try {
                        DDS.getInstance().doAuth();
                        DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();
                    } catch (DDSNotInitCompleteException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "思必驰语音初始化成功:" + "完全初始化");
                } else {
                    Log.e(TAG, "思必驰语音初始化成功:" + "未完全初始化");
                }
            }

            @Override
            public void onError(int what, final String msg) {
                Log.e(TAG, "思必驰语音初始化失败：" + msg);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }, new DDSAuthListener() {
            @Override
            public void onAuthSuccess() {
                Log.e(TAG, "思必驰语音授权成功");
                sendBroadcast(new Intent("ddsdemo.intent.action.auth_success"));
            }

            @Override
            public void onAuthFailed(final String errId, final String error) {
                Log.e(TAG, "思必驰语音授权错误 ，错误码：" + errId + "，错误信息：" + error);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "授权错误:" + errId + ":\n" + error + "\n请查看手册处理", Toast
                                .LENGTH_SHORT).show();
                    }
                });
                sendBroadcast(new Intent("ddsdemo.intent.action.auth_failed"));
            }
        });

    }
    class MyMessageObserver implements MessageObserver {
        @Override
        public void onMessage(String message, String data) {
            if ("avatar.silence".equals(message)) {
                Log.e("=================", "思必驰==》空闲" );
            } else if ("avatar.listening".equals(message)) {
                Log.e("=================", "思必驰==》听" );
            } else if ("avatar.understanding".equals(message)) {
                Log.e("=================", "思必驰==》识别中" );
            } else if ("avatar.speaking".equals(message)) {
                Log.e("=================", "思必驰==》讲话" );
            }
        }
    }
    /**
     * Command 解析
     */
    private CommandObserver commandObserver = new CommandObserver() {
        @Override
        public void onCall(final String command, final String data) {
          //  Log.e("==================", "cmd:" + data.toString());
            if (command.equals("sys.open.app")) {
                try {
                    DDS.getInstance().getAgent().getTTSEngine().speak("正在打开" + parseJson(data), 1);
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }else if (command.equals("cmd.media.play")){
                try {
                    DDS.getInstance().getAgent().getTTSEngine().speak("即将为您播放" + parseJson(data), 1);

                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }else if (command.equals("cmd.media.pause")){
                try {
                    DDS.getInstance().getAgent().getTTSEngine().speak("即将为您暂停" + parseJson(data), 1);
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }else if (command.equals("cmd.media.next")){
                try {
                    DDS.getInstance().getAgent().getTTSEngine().speak("已为您做 " + parseJson(data)+" 操作", 1);
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }else if (command.equals("cmd.media.pre")){
                try {
                    DDS.getInstance().getAgent().getTTSEngine().speak("已为您做 " + parseJson(data)+" 操作", 1);
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }
           // Log.e("=========1=========", "操作:" + parseJson(data));
        }
    };

    /**
     *  解析一般数据 （不限于 媒体方向 ）
     * @param data
     * @return
     */
    private String parseJson(String data){
        String value="";
        try {
            JSONObject jsonObject = new JSONObject(data);
            JSONObject jsonObject1 = jsonObject.getJSONObject("nlu");
            JSONObject jsonObject2 = jsonObject1.getJSONObject("semantics");
            JSONObject jsonObject3 = jsonObject2.getJSONObject("request");
            JSONArray jsonArray = jsonObject3.getJSONArray("slots");
            JSONObject jsonObject4 = jsonArray.getJSONObject(0);
            value = jsonObject4.getString("value");
            // TODO 依据w的值，执行打开窗户操作
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  value;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        DDS.getInstance().getAgent().unSubscribe(commandObserver);
        DDS.getInstance().getAgent().unSubscribe(mMessageObserver);
        DDS.getInstance().release();
        System.exit(0);
    }

    private DDSConfig createConfig() {

        DDSConfig config = new DDSConfig();

        // 基础配置项
        config.addConfig(DDSConfig.K_PRODUCT_ID, "278574049"); // 产品ID
        config.addConfig(DDSConfig.K_USER_ID, "MrFan");  // 用户ID
        config.addConfig(DDSConfig.K_ALIAS_KEY, "test");   // 产品的发布分支
        config.addConfig(DDSConfig.K_AUTH_TYPE, AuthType.PROFILE); //授权方式, 支持思必驰账号授权和profile文件授权
        config.addConfig(DDSConfig.K_API_KEY, "6c9d105dab126c9d105dab125b5ac8b3");  // 产品授权秘钥，服务端生成，用于产品授权
        //config.addConfig("MINIMUM_STORAGE", (long)  200 * 1024 * 1024); // SDK需要的最小存储空间的配置，对于/data分区较小的机器可以配置此项，同时需要把内核资源放在其他位置

//        // 资源更新配置项
//        // 参考可选内置资源包文档: https://www.dui.ai/docs/operation/#/ct_ziyuan
        config.addConfig(DDSConfig.K_DUICORE_ZIP, "duicore.zip"); // 预置在指定目录下的DUI内核资源包名, 避免在线下载内核消耗流量, 推荐使用
        config.addConfig(DDSConfig.K_CUSTOM_ZIP, "product.zip"); // 预置在指定目录下的DUI产品配置资源包名, 避免在线下载产品配置消耗流量, 推荐使用
        config.addConfig(DDSConfig.K_USE_UPDATE_DUICORE, "false"); //设置为false可以关闭dui内核的热更新功能，可以配合内置dui内核资源使用
        config.addConfig(DDSConfig.K_USE_UPDATE_NOTIFICATION, "false"); // 是否使用内置的资源更新通知栏

        //   config.addConfig(DDSConfig.K_USE_UPDATE_NOTIFICATION, "true");
        Log.i(TAG, "config->" + config.toString());
        return config;
    }


}