package com.yourpackage.notificationalarm; // <<<<<<< 替换成你的实际包名

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MyNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "NotifListenerService";
    private static final String CHANNEL_ID = "NotificationAlarmChannel";
    private static final String ALARM_CHANNEL_ID = "AlarmChannel";

    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final int ALARM_NOTIFICATION_ID = 2;

    private MediaPlayer mediaPlayer;
    private boolean isAlarmPlaying = false;

    private static final String KEYWORD_TO_MATCH = "小海马看不见宝宝的小鼻子啦";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Notification Listener Service created.");
        createNotificationChannel();
        createAlarmNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Notification Listener Service onStartCommand.");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());
        }

        if (intent != null && "com.yourpackage.notificationalarm.ACTION_STOP_ALARM".equals(intent.getAction())) { // <<<<<<< 替换成你的实际包名
            Log.d(TAG, "Received ACTION_STOP_ALARM in Service. Stopping alarm.");
            stopAlarm();
        }

        if (intent != null && "ACTION_STOP_SERVICE".equals(intent.getAction())) {
            Log.d(TAG, "Received ACTION_STOP_SERVICE in Service. Stopping service.");
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = "";
        String text = "";

        if (sbn.getNotification() != null && sbn.getNotification().extras != null) {
            title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
            text = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);
        }

        Log.d(TAG, "Notification Posted: Package=" + packageName + ", Title=" + title + ", Text=" + text);

        boolean isKeywordFound = false;

        if (title != null && title.contains(KEYWORD_TO_MATCH)) {
            isKeywordFound = true;
            Log.d(TAG, "Keyword '" + KEYWORD_TO_MATCH + "' found in notification title.");
        }

        if (!isKeywordFound && text != null && text.contains(KEYWORD_TO_MATCH)) {
            isKeywordFound = true;
            Log.d(TAG, "Keyword '" + KEYWORD_TO_MATCH + "' found in notification text.");
        }

        if (isKeywordFound) {
            Log.d(TAG, "Matching keyword found! Triggering alarm.");
            if (!isAlarmPlaying) {
                startAlarm();
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification Removed: Package=" + sbn.getPackageName());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAlarm(); // 确保服务销毁时停止警报，这也会触发发送广播
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(FOREGROUND_NOTIFICATION_ID);
        }
        Log.d(TAG, "Notification Listener Service destroyed.");
    }

    private void startAlarm() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer. 'alarm.mp3' might be missing or corrupted.");
                return;
            }
            mediaPlayer.setLooping(true);
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isAlarmPlaying = true;
            Log.d(TAG, "Alarm started.");
            sendAlarmNotification();
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isAlarmPlaying = false;
            Log.d(TAG, "Alarm stopped.");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(ALARM_NOTIFICATION_ID);
            }

            // --- 新增代码：发送广播通知 MainActivity 警报已停止 ---
            Intent alarmStoppedIntent = new Intent(MainActivity.ACTION_ALARM_STOPPED); // 使用 MainActivity 中定义的常量 Action
            sendBroadcast(alarmStoppedIntent);
            Log.d(TAG, "Broadcast ACTION_ALARM_STOPPED sent.");
            // -----------------------------------------------------
        }
    }

    private Notification createForegroundNotification() {
        Intent stopServiceIntent = new Intent(this, MyNotificationListenerService.class);
        stopServiceIntent.setAction("ACTION_STOP_SERVICE");
        PendingIntent stopServicePendingIntent = PendingIntent.getService(this, 0, stopServiceIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("通知警报服务运行中")
                .setContentText("正在后台监听通知...")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .addAction(R.drawable.ic_stop, "停止服务", stopServicePendingIntent)
                .build();
    }

    private void sendAlarmNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Intent stopAlarmIntent = new Intent(this, StopAlarmReceiver.class);
        stopAlarmIntent.setAction("com.yourpackage.notificationalarm.ACTION_STOP_ALARM");
        PendingIntent stopAlarmPendingIntent = PendingIntent.getBroadcast(this, 0, stopAlarmIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                .setContentTitle("警报！有匹配的通知")
                .setContentText("请点击停止按钮来静音")
                .setSmallIcon(R.drawable.ic_alarm)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_stop, "停止警报", stopAlarmPendingIntent)
                .build();

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "通知警报服务";
            String description = "用于显示通知警报服务的运行状态";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "警报通知";
            String description = "当检测到匹配通知时发出的警报";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(ALARM_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}