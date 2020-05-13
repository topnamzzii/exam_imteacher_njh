package com.example.fcm;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.exam_imteacher_njh.MainActivity;
import com.google.firebase.messaging.RemoteMessage;


public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private String TAG = "imteacher";
    String title, body;
    private static PowerManager.WakeLock sCpuWakeLock;

    public FirebaseMessagingService() {
        try{
        this.TAG = MainActivity.TAG;
        }catch(Exception e){
            //error 처리
        }
    }

    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body =remoteMessage.getData().get("body");
            Log.i(TAG,"title : " + title);
            Log.i(TAG,"body : " + body);

            wakePower();
            Intent intent = new Intent("MainActivity");
            intent.putExtra("title", title);
            intent.putExtra("body", body);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent); // 푸시리시버 전달

        }
    }
    // 화면 꺼짐방지 함수
    @SuppressLint("InvalidWakeLockTag")
    private void wakePower() {
        if (sCpuWakeLock != null) {
            return;
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, TAG);
        sCpuWakeLock.acquire(10000);

        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }


    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken: " + token);
    }
}