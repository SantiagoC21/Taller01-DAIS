package com.example.tarea_1.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.tarea_1.service.BluetoothMonitorService;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Dispositivo reiniciado — iniciando servicio de monitoreo Bluetooth");

            Intent serviceIntent = new Intent(context, BluetoothMonitorService.class);

            // ✅ Fix API 26: startForegroundService solo disponible desde Android 8 (O)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
