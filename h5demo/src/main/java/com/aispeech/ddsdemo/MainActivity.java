package com.aispeech.ddsdemo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.aispeech.ailog.AILog;
import com.aispeech.ddsdemo.webview.HybridWebViewClient;
import com.aispeech.ddsdemo.widget.InputField;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.DDSConfig;
import com.aispeech.dui.dds.agent.MessageObserver;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.aispeech.dui.dds.update.DDSUpdateListener;
import com.aispeech.dui.dds.utils.PrefUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements InputField.Listener {
    String TAG = "MainActivity";
    private WebView webview;
    RelativeLayout webContainer;
    MyReceiver receiver;
    private MyMessageObserver mMessageObserver;
    private InputField inputField;
    private boolean isActivityShowing = false;
    private Dialog dialog;
    private Handler mHandler = new Handler();
    private boolean mLoadedTotally = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View titleView = inflater.inflate(R.layout.action_bar_title, null);
        actionBar.setCustomView(titleView, lp);

        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);

        inputField = (InputField) this.findViewById(R.id.input_field);
        inputField.setListener(this);

        webContainer = (RelativeLayout) this.findViewById(R.id.main_web_container);
        setWebView();
        webContainer.addView(webview, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
                .MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        IntentFilter filter = new IntentFilter();
        filter.addAction("ddsdemo.intent.action.init_complete");
        receiver = new MyReceiver();
        registerReceiver(receiver, filter);

        mMessageObserver = new MyMessageObserver();

        Button btnTest = (Button) this.findViewById(R.id.btn_zhinan);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNormalDialog();
            }
        });
    }

    private void showNormalDialog() {
        final AlertDialog.Builder
                normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.drawable.test);
        normalDialog.setTitle("使用指南");
        normalDialog.setMessage("生活中需要惊喜，赶快试试吧!  \n本应用处于测试阶段，欢迎提出建议和意见\nQQ:1622099118 \n精彩不断" +
                "更新中~~\n                               by:Fan");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface
                                                dialog, int which) {

                    }
                });
        normalDialog.show();
    }

    @Override
    protected void onStart() {
        isActivityShowing = true;
        try {
            DDS.getInstance().getUpdater().update(ddsUpdateListener);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }

        DDS.getInstance().getAgent().subscribe("sys.resource.updated", resourceUpdatedMessageObserver);

        super.onStart();
    }

    @Override
    protected void onStop() {
        AILog.d(TAG, "onStop() " + this.hashCode());
        isActivityShowing = false;
        DDS.getInstance().getAgent().unSubscribe(resourceUpdatedMessageObserver);

        if (dialog != null) {
            dialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        DDS.getInstance().getAgent().subscribe(new String[]{"avatar.silence", "avatar.listening", "avatar.understanding", "avatar.speaking"}, mMessageObserver);

        inputField.getAvatarView().go();
        enableWakeIfNecessary();

        super.onResume();
    }

    @Override
    protected void onPause() {

        DDS.getInstance().getAgent().unSubscribe(mMessageObserver);

        inputField.toIdle();
        disableWakeIfNecessary();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        MainActivity.this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        unregisterReceiver(receiver);
        webContainer.removeAllViews();
        webview.removeAllViews();
        webview.destroy();
        inputField.destroy();
        stopService();
    }

    private void stopService() {
        Intent intent = new Intent(MainActivity.this, DDSService.class);
        stopService(intent);
    }

    private void setWebView() {
        webview = new WebView(getApplicationContext());
        webview.setWebViewClient(new HybridWebViewClient(this));
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d(TAG, "view " + view + " progress " + newProgress + " mLoadedTotally " + mLoadedTotally);
                if (newProgress == 100 && !mLoadedTotally) {
                    mLoadedTotally = true;
                    sendHiMessage();
                }
            }
        });
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setBackgroundColor(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }
        try {
            loadUI(DDS.getInstance().getAgent().getValidH5Path());
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }


    void loadUI(String h5UiPath) {
        Log.d(TAG, "loadUI " + h5UiPath);
        String url = h5UiPath;
        mLoadedTotally = false;
        webview.loadUrl(url);
    }

    void enableWakeIfNecessary() {
        try {
            DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    void disableWakeIfNecessary() {
        try {
            DDS.getInstance().getAgent().stopDialog();
            DDS.getInstance().getAgent().getWakeupEngine().disableWakeup();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void sendHiMessage() {
        Log.d(TAG, "sendHiMessage");
        String[] wakeupWords = new String[0];
        String minorWakeupWord = null;
        try {
            wakeupWords = DDS.getInstance().getAgent().getWakeupEngine().getWakeupWords();
            minorWakeupWord = DDS.getInstance().getAgent().getWakeupEngine().getMinorWakeupWord();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
        String hiStr = "";
        if (wakeupWords != null && minorWakeupWord != null) {
            hiStr = getString(R.string.hi_str2, wakeupWords[0], minorWakeupWord);
        } else if (wakeupWords != null && wakeupWords.length == 2) {
            hiStr = getString(R.string.hi_str2, wakeupWords[0], wakeupWords[1]);
        } else if (wakeupWords != null && wakeupWords.length > 0) {
            hiStr = getString(R.string.hi_str, wakeupWords[0]);
        }
        JSONObject output = new JSONObject();
        try {
            output.put("text", hiStr);
            DDS.getInstance().getAgent().getBusClient().publish("context.output.text", output.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onMicClicked() {
        try {
            DDS.getInstance().getAgent().avatarClick();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
        return true;
    }


    class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String name = intent.getAction();
            Log.d(TAG, "onReceive " + name);
            if (name.equals("ddsdemo.intent.action.init_complete")) {
                try {
                    loadUI(DDS.getInstance().getAgent().getValidH5Path());
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class MyMessageObserver implements MessageObserver {
        private StringBuilder mInputText = new StringBuilder();

        @Override
        public void onMessage(String message, String data) {
       //     Log.e(TAG, "message : " + message + " data : " + data);

            if ("avatar.silence".equals(message)) {
                inputField.toIdle();
            } else if ("avatar.listening".equals(message)) {
                inputField.toListen();
            } else if ("avatar.understanding".equals(message)) {
                inputField.toRecognize();
            } else if ("avatar.speaking".equals(message)) {
                inputField.toSpeak();
            }
        }
    }

    private DDSUpdateListener  ddsUpdateListener = new DDSUpdateListener() {
        @Override
        public void onUpdateFound(String detail) {
            final String str = detail;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showNewVersionDialog(str);
                }
            });

            try {
                DDS.getInstance().getAgent().getTTSEngine().speak("发现新版本,正在为您更新", 1);
            } catch (DDSNotInitCompleteException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onUpdateFinish() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showUpdateFinishedDialog();
                }
            });

            try {
                DDS.getInstance().getAgent().getTTSEngine().speak("更新成功", 1);
            } catch (DDSNotInitCompleteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDownloadProgress(float progress) {
            Log.d(TAG, "onDownloadProgress :" + progress);
        }

        @Override
        public void onError(int what, String error) {
            String productId = PrefUtil.getString(getApplicationContext(), DDSConfig.K_PRODUCT_ID);
            if (what == 70319) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showProductNeedUpdateDialog();
                    }
                });

            } else {
                Log.e(TAG, "UPDATE ERROR : " + error);
            }
        }

        @Override
        public void onUpgrade(String version) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showApkUpdateDialog();
                }
            });

        }
    };

    private MessageObserver resourceUpdatedMessageObserver = new MessageObserver() {
        @Override
        public void onMessage(String message, String data) {
            try {
                DDS.getInstance().getUpdater().update(ddsUpdateListener);
            } catch (DDSNotInitCompleteException e) {
                e.printStackTrace();
            }
        }
    };


    protected void showNewVersionDialog(final String info) {
        if (!isActivityShowing) {
            return;
        }
        if (null != dialog) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setTitle("发现新版本,正在为您更新");
        ((ProgressDialog) dialog).setMessage(info);
        ((ProgressDialog) dialog).setProgress(0);
        dialog.show();
    }

    protected void showProductNeedUpdateDialog() {
        if (!isActivityShowing) {
            return;
        }
        if (null != dialog) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string
                .update_product_title)
                .setMessage(R.string.update_product_message).setPositiveButton(R.string.update_product_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialog.dismiss();
                            }
                        }).setNegativeButton(R.string.update_product_cancel, new DialogInterface
                        .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialog.dismiss();
                        System.exit(0);
                    }
                }).create();
        dialog.show();
    }

    protected void showApkUpdateDialog() {
        if (!isActivityShowing) {
            return;
        }
        if (null != dialog) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string
                .update_sdk_title)
                .setMessage(R.string.update_sdk_message).setPositiveButton(R.string.update_sdk_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialog.dismiss();
                            }
                        }).setNegativeButton(R.string.update_sdk_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialog.dismiss();
                        System.exit(0);
                    }
                }).create();
        dialog.show();
    }

    protected void showUpdateFinishedDialog() {
        if (!isActivityShowing) {
            return;
        }
        if (null != dialog) {
            dialog.dismiss();
            dialog = null;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("资源加载成功");

        dialog = builder.create();
        dialog.show();

        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                dialog.dismiss();
                t.cancel();
            }
        }, 2000);
    }

}
