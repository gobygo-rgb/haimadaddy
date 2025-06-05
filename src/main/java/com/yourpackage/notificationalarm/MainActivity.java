package com.yourpackage.notificationalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NotificationAlarm";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    public static final String ACTION_ALARM_STOPPED = "com.yourpackage.notificationalarm.ACTION_ALARM_STOPPED";

    private Button btnCheckPermission;
    private Button btnStartService;
    private Button btnStopAlarmManual;

    private BroadcastReceiver alarmStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(ACTION_ALARM_STOPPED)) {
                Log.d(TAG, "Received ACTION_ALARM_STOPPED broadcast. Enabling 'Stop Alarm Manual' button.");
                btnStopAlarmManual.setEnabled(true);
                Toast.makeText(context, "警报已停止", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCheckPermission = findViewById(R.id.btn_check_permission);
        btnStartService = findViewById(R.id.btn_start_service);
        btnStopAlarmManual = findViewById(R.id.btn_stop_alarm_manual);

        btnCheckPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isNotificationServiceEnabled()) {
                    Intent intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "请在列表中找到并开启 '通知警报监听器'", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "通知监听权限已开启", Toast.LENGTH_SHORT).show();
                }
                updateUI();
            }
        });

        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNotificationServiceEnabled()) {
                    Intent serviceIntent = new Intent(MainActivity.this, MyNotificationListenerService.class);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }

                    Toast.makeText(getApplicationContext(), "通知监听服务已启动", Toast.LENGTH_SHORT).show();
                    updateUI();
                } else {
                    Toast.makeText(MainActivity.this, "请先开启通知监听权限！", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnStopAlarmManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stopAlarmIntent = new Intent(MainActivity.this, MyNotificationListenerService.class);
                stopAlarmIntent.setAction("com.yourpackage.notificationalarm.ACTION_STOP_ALARM");
                startService(stopAlarmIntent);
                Toast.makeText(MainActivity.this, "已发送静音指令", Toast.LENGTH_SHORT).show();
                btnStopAlarmManual.setEnabled(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        IntentFilter filter = new IntentFilter(ACTION_ALARM_STOPPED);

        // <<<<<<<< 在这里新增代码 >>>>>>>>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) 及更高版本
            // 对于 Android 13+, receiverFlags 参数被强制要求。
            // Context.RECEIVER_NOT_EXPORTED 表示这个接收器只能接收本应用发送的广播。
            registerReceiver(alarmStoppedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // 对于 Android 12 (API 31) 和 Android 12L (API 32)，虽然不是强制要求，
            // 但为了兼容性最好也加上 FLAG_IMMUTABLE 或 FLAG_MUTABLE。
            // 对于你的情况，直接用下面的写法也行，因为这个 Receiver 只是接收本应用广播。
            // 或者： registerReceiver(alarmStoppedReceiver, filter, 0); // 0 表示没有特殊 flags
            registerReceiver(alarmStoppedReceiver, filter); // 兼容旧版本 Android
        }
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        Log.d(TAG, "BroadcastReceiver registered.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(alarmStoppedReceiver);
        Log.d(TAG, "BroadcastReceiver unregistered.");
    }

    private void updateUI() {
        if (isNotificationServiceEnabled()) {
            btnCheckPermission.setText("权限已开启");
            btnCheckPermission.setEnabled(false);
            btnStartService.setEnabled(true);
            btnStopAlarmManual.setEnabled(true);
        } else {
            btnCheckPermission.setText("检查并开启权限");
            btnCheckPermission.setEnabled(true);
            btnStartService.setEnabled(false);
            btnStopAlarmManual.setEnabled(false);
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null && TextUtils.equals(pkgName, cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }
}