package com.example.tarea_1.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.example.tarea_1.activity.BiometricActivity;

public class FingerprintService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Lanza la actividad transparente que mostrará el diálogo de la huella
        Intent biometricIntent = new Intent(this, BiometricActivity.class);
        biometricIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(biometricIntent);

        // El servicio ha cumplido su única misión, por lo que no necesita seguir corriendo.
        stopSelf();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
