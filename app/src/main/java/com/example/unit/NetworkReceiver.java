package com.example.unit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.example.exam_imteacher_njh.R;


public class NetworkReceiver extends BroadcastReceiver {
    public String TAG;
    // BroadCast를 받앗을때 자동으로 호출되는 콜백 메소드
    @Override
    public void onReceive(Context context, Intent intent) {
        //final static value
        this.TAG = context.getString(R.string.TAG);
        String action= intent.getAction();

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            try {
                ConnectivityManager connectivityManager =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
                NetworkInfo _wifi_network =
                        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if(_wifi_network != null) {
                    // wifi, 3g 둘 중 하나라도 있을 경우
                    if(_wifi_network != null && activeNetInfo != null){
                        Log.d(TAG, "wifi, 3g 둘 중 하나라도 있을 경우 ");
                    }
                    // wifi, 3g 둘 다 없을 경우
                    else{
                        Log.d(TAG, "wifi, 3g 둘 다 없을 경우");

                        Intent intent_wifi = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        context.startActivity(intent_wifi);
                        Toast.makeText(context, "인터넷 연결을 확인해 주세요!", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
            }
        }

    }
}
