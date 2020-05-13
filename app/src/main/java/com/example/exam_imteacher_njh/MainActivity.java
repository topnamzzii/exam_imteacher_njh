package com.example.exam_imteacher_njh;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG ="imteacher";
    private Context mContext;
    private WebView wv01;
    public static final int DELAYTIME_RESTART = 2*1000; // 토큰 불러오기실패시 재실행 텀
    private String final_url; // 푸시 또는 실행후의 url을 받아서 webview를
    public String URL_INDEX_FULL;
    public String SERVICE_USER;
    private BroadcastReceiver message_receiver; //푸시받을수 있는 리시버
    private SoundPool sp01; //알람음
    private int soundId;
    private int push_index; // 상단 알람바에 알림이 겹치지않게 하기위한 noti인덱스




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mContext = getBaseContext();
        Log.i(TAG,"");

        init();
        setFunction();
    }

    private void init() {
        this.URL_INDEX_FULL = getString(R.string.URL_INDEX_FULL);
        this.SERVICE_USER = getString(R.string.SERVICE_USER);

        // 토큰체크
        if(!check_token()){
            return ;
        }
        final_url = URL_INDEX_FULL+ getSwHwInfo();


        // 웹뷰 시작
        wv01 = (WebView) findViewById(R.id.wv01);

        wv01.setWebViewClient(new WebViewClient()); // 클릭시 새창 안뜨게
        wv01.getSettings().setJavaScriptEnabled(true); //자바스크립트 허용
        wv01.getSettings().setTextZoom(100); // 줌인 100% 맞춤

        wv01.loadUrl(final_url); // 웹뷰에 표시할 웹사이트 주소, 웹뷰 시작

    }


    private void setFunction() {
        //푸시를 받는 리시버
        message_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub // Get extra data included in the Intent
                Log.i(TAG, "message_receiver/onReceive");
                receive_fcm(intent,true);


            }
        };
    }

    //소프트웨어 정보 가져오기 함수
    public String getSwHwInfo() {
        String token;
        String kidsmarker_version;
        String hw_wifi_mac;
        String hw_model;
        String sw_version;
        String sw_os;

        FirebaseMessaging.getInstance().subscribeToTopic("notice");
        token = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "MainActivity/token:" + token);
        kidsmarker_version = getAppVersionName();
        hw_wifi_mac = getMACAddress("wlan0");
        hw_model = Build.MODEL;
        sw_version = android.os.Build.VERSION.SDK_INT + "";
        sw_os = "android";
        return "?"+SERVICE_USER+"_token=" + token + "&kidsmarker_version=" + kidsmarker_version + "&hw_wifi_mac=" + hw_wifi_mac + "&hw_model=" + hw_model + "&sw_version=" + sw_version +
                "&sw_os=" + sw_os;

    }

    //앱 버전 가져오기 함수
    public String getAppVersionName() {
        PackageInfo packageInfo = null;         //패키지에 대한 전반적인 정보

        //PackageInfo 초기화
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }

        return packageInfo.versionName;
    }

    //모바일의 고유값( 맥어드레스) 가져오기 함수
    public String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }


    //토큰 체크 함수
    public Boolean check_token() {
        FirebaseMessaging.getInstance().subscribeToTopic("notice");
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "teacher_token:"+token);
        if (token==null){ //토큰없을시 토큰이 발급될때까지 재실행
            Handler dshandler = new Handler(Looper.getMainLooper());
            dshandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent_restart = new Intent(MainActivity.this, MainActivity.class);
                    finish();
                    startActivity(intent_restart);
                }
            }, DELAYTIME_RESTART);
            return false;
        }else{
            return true;
        }
    }
    // 포그라운드 상태에서 푸시받았을때 처리
    public String receive_fcm(Intent intent, boolean is_foreground) {
        String title = intent.getStringExtra("title"); //0 받으면 정상
        String body = intent.getStringExtra("body"); // json 형태로 받아짐
        JSONObject body_json = null;
        Log.i(TAG, "message_receiver/onReceive/title:" + title + "/body:" + body);
        String url = "";
        String noti_title = "error";
        String noti_contents = "error";
        String to_string = "";

        try {
            // json 파싱
            body_json = new JSONObject(body);

            if (((body_json.has("noti_breakaway_title") && !body_json.isNull("noti_breakaway_title"))) ? true : false) {
                url = body_json.getString("noti_breakaway_url");
                noti_title = body_json.getString("noti_breakaway_title");
                noti_contents = body_json.getString("noti_breakaway_content");
                to_string = "noti_breakaway ::  url : /" + url + "/noti_title:" + noti_title + " / noti_contents : " + noti_contents;
            } else if (((body_json.has("noti_lateabsence_title") && !body_json.isNull("noti_lateabsence_title"))) ? true : false) {
                url = body_json.getString("noti_lateabsence_url");
                noti_title = body_json.getString("noti_lateabsence_title");
                noti_contents = body_json.getString("noti_lateabsence_content");
                to_string = "noti_lateabsence :: url : /" + url + "/noti_title:" + noti_title + " / noti_contents : " + noti_contents;
            }

            Log.i(TAG, "message_receiver: " + to_string);
            if(is_foreground) {
                startNoti(noti_title, noti_contents, url,title,body);
                startSound();
                return null;
            }else{
                return url;

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;

    }

    //상단 노티피케이션 띄우기 함수
    public void startNoti(String noti_title, String contents, String url,String title, String body) {
        Log.i(TAG, "startNoti");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        String channelId = "채널 ID";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.icon_noti)
                        .setContentTitle(noti_title)
                        .setContentText(contents)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(push_index++ /* ID of notification */, notificationBuilder.build());
    }

    // 알람음 재생
    public void startSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            sp01 = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(10).build();

        }
        else {
            sp01 = new SoundPool(10, AudioManager.STREAM_NOTIFICATION, 0);
        }
        soundId = sp01.load(this, R.raw.alert_kidsmarker, 1);

        sp01.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(sampleId, 1f, 1f, 0, 0, 1f);
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(message_receiver, new IntentFilter("MainActivity"));// 푸시받는 리시버 등록

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
