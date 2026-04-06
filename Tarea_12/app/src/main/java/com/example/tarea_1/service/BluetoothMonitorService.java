package com.example.tarea_1.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.tarea_1.dao.BluetoothDeviceDAO;
import com.example.tarea_1.dao.BluetoothDeviceDAOImpl;
import com.example.tarea_1.model.BluetoothDeviceEntity;

public class BluetoothMonitorService extends Service {

    private static final String TAG        = "BTMonitorService";
    private static final String CHANNEL_ID = "bt_monitor_channel";
    private static final int    NOTIF_ID   = 1002;

    private BluetoothDeviceDAO dao;
    private BroadcastReceiver  bluetoothReceiver;
    private boolean            receiverRegistered = false;

    @Override
    public void onCreate() {
        super.onCreate();
        dao = new BluetoothDeviceDAOImpl(this);
        createNotificationChannel();
        registerBluetoothReceiver();
        Log.i(TAG, "Servicio de monitoreo Bluetooth iniciado");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBluetoothReceiver();
        Log.i(TAG, "Servicio de monitoreo Bluetooth detenido");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ─── Registro dinámico del Receiver ───────────────────────────────────────

    private void registerBluetoothReceiver() {
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) return;

                // ✅ Obtener dispositivo con manejo de SecurityException
                BluetoothDevice device;
                try {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                } catch (Exception e) {
                    Log.e(TAG, "Error obteniendo dispositivo BT: " + e.getMessage());
                    return;
                }

                if (device == null) return;

                // ✅ Leer MAC y nombre con try-catch por permiso
                String mac;
                String name;
                try {
                    mac  = device.getAddress();
                    name = device.getName() != null ? device.getName() : mac;
                } catch (SecurityException e) {
                    Log.e(TAG, "Permiso Bluetooth denegado al leer dispositivo: " + e.getMessage());
                    return;
                }

                Log.d(TAG, "ACL desconectado: " + name + " (" + mac + ")");

                final String finalMac  = mac;
                final String finalName = name;

                new Thread(() -> {
                    BluetoothDeviceEntity entity = dao.findByMac(finalMac);
                    if (entity != null && entity.isMonitored()) {
                        Log.i(TAG, "Disparando alarma para: " + finalName);

                        Intent alarmIntent = new Intent(BluetoothMonitorService.this,
                                                        AlarmService.class);
                        alarmIntent.setAction(AlarmService.ACTION_START_ALARM);
                        alarmIntent.putExtra(AlarmService.EXTRA_DEVICE_NAME,    finalName);
                        alarmIntent.putExtra(AlarmService.EXTRA_DEVICE_MAC,     finalMac);
                        alarmIntent.putExtra(AlarmService.EXTRA_ALARM_DURATION,
                                             entity.getAlarmDurationSecs());

                        // ✅ Fix API 26
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(alarmIntent);
                        } else {
                            startService(alarmIntent);
                        }
                    }
                }).start();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        // ✅ Fix para Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(bluetoothReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void unregisterBluetoothReceiver() {
        if (receiverRegistered && bluetoothReceiver != null) {
            try {
                unregisterReceiver(bluetoothReceiver);
            } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
    }

    // ─── Notificación persistente ─────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Monitor Bluetooth",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Servicio activo de vigilancia Bluetooth");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitor Bluetooth activo")
            .setContentText("Vigilando desconexiones de dispositivos registrados")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
}
