package com.example.tarea_1.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.tarea_1.dao.BluetoothDeviceDAO;
import com.example.tarea_1.dao.BluetoothDeviceDAOImpl;
import com.example.tarea_1.model.BluetoothDeviceEntity;
import com.example.tarea_1.service.AlarmService;

public class BluetoothDisconnectReceiver extends BroadcastReceiver {

    private static final String TAG = "BTDisconnectReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            handleDisconnection(context, intent);
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                               BluetoothDevice.BOND_NONE);
            if (bondState == BluetoothDevice.BOND_NONE) {
                handleDisconnection(context, intent);
            }
        }
    }

    private void handleDisconnection(Context context, Intent intent) {
        // Obtener el dispositivo de forma segura
        BluetoothDevice btDevice;
        try {
            btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo dispositivo: " + e.getMessage());
            return;
        }

        if (btDevice == null) return;

        // Obtener MAC y nombre con manejo de SecurityException
        String macAddress;
        String deviceName;
        try {
            macAddress = btDevice.getAddress();
            deviceName = btDevice.getName() != null ? btDevice.getName() : "Desconocido";
        } catch (SecurityException e) {
            Log.e(TAG, "Permiso Bluetooth denegado: " + e.getMessage());
            return;
        }

        Log.d(TAG, "Dispositivo desconectado: " + deviceName + " (" + macAddress + ")");

        final PendingResult pendingResult = goAsync();
        final String finalMac  = macAddress;
        final String finalName = deviceName;

        new Thread(() -> {
            try {
                BluetoothDeviceDAO dao = new BluetoothDeviceDAOImpl(context);
                BluetoothDeviceEntity entity = dao.findByMac(finalMac);

                if (entity != null && entity.isMonitored()) {
                    Log.i(TAG, "Dispositivo monitoreado desconectado. Lanzando alarma...");
                    launchAlarmService(context, entity);
                }
            } finally {
                pendingResult.finish();
            }
        }).start();
    }

    private void launchAlarmService(Context context, BluetoothDeviceEntity entity) {
        Intent alarmIntent = new Intent(context, AlarmService.class);
        alarmIntent.setAction(AlarmService.ACTION_START_ALARM);
        alarmIntent.putExtra(AlarmService.EXTRA_DEVICE_NAME,    entity.getDeviceName());
        alarmIntent.putExtra(AlarmService.EXTRA_DEVICE_MAC,     entity.getMacAddress());
        alarmIntent.putExtra(AlarmService.EXTRA_ALARM_DURATION, entity.getAlarmDurationSecs());

        // ✅ Fix API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(alarmIntent);
        } else {
            context.startService(alarmIntent);
        }
    }
}
