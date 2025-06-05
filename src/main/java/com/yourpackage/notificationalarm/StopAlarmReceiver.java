package com.yourpackage.notificationalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StopAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "StopAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "com.yourpackage.notificationalarm.ACTION_STOP_ALARM".equals(intent.getAction())) {
            Log.d(TAG, "Received ACTION_STOP_ALARM broadcast.");
            // 通过 Intent 启动服务，让服务来停止警报
            Intent stopAlarmServiceIntent = new Intent(context, MyNotificationListenerService.class);
            stopAlarmServiceIntent.setAction("ACTION_STOP_ALARM");
            context.startService(stopAlarmServiceIntent); // 直接调用 startService 即可，因为服务已经运行
        }
    }
}