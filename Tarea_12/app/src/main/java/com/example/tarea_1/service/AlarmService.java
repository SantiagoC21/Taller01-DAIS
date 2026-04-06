package com.example.tarea_1.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.tarea_1.MainActivity;

/**
 * Servicio en primer plano que:
 *  1. Reproduce alarma al máximo volumen.
 *  2. Bloquea cualquier intento de bajar el volumen (ContentObserver).
 *  3. Hace vibrar el dispositivo.
 *  4. Bloquea la pantalla táctil con overlay WindowManager.
 *  5. Se detiene automáticamente tras la duración configurada.
 */
public class AlarmService extends Service {

    private static final String TAG = "AlarmService";

    public static final String ACTION_START_ALARM   = "com.example.tarea_1.START_ALARM";
    public static final String ACTION_STOP_ALARM    = "com.example.tarea_1.STOP_ALARM";
    public static final String EXTRA_DEVICE_NAME    = "extra_device_name";
    public static final String EXTRA_DEVICE_MAC     = "extra_device_mac";
    public static final String EXTRA_ALARM_DURATION = "extra_alarm_duration";

    private static final String CHANNEL_ID      = "bt_alarm_channel";
    private static final int    NOTIFICATION_ID  = 1001;
    private static final int    DEFAULT_DURATION = 30;

    // ─── Componentes ──────────────────────────────────────────────────────────
    private MediaPlayer     mediaPlayer;
    private AudioManager    audioManager;
    private Vibrator        vibrator;
    private WindowManager   windowManager;
    private View            overlayView;
    private Handler         handler;
    private Runnable        stopRunnable;
    private int             savedVolume;
    private int             maxVolume;
    private boolean         isRunning = false;

    // ─── Observer que vigila y bloquea cambios de volumen ─────────────────────
    private ContentObserver volumeObserver;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        handler       = new Handler(Looper.getMainLooper());
        audioManager  = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator      = (Vibrator)     getSystemService(Context.VIBRATOR_SERVICE);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if (ACTION_STOP_ALARM.equals(intent.getAction())) {
            stopAlarm();
            return START_NOT_STICKY;
        }

        if (ACTION_START_ALARM.equals(intent.getAction()) && !isRunning) {
            String deviceName  = intent.getStringExtra(EXTRA_DEVICE_NAME);
            String deviceMac   = intent.getStringExtra(EXTRA_DEVICE_MAC);
            int durationSecs   = intent.getIntExtra(EXTRA_ALARM_DURATION, DEFAULT_DURATION);

            startForeground(NOTIFICATION_ID,
                buildNotification(deviceName, deviceMac, durationSecs));
            startAlarm(durationSecs);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ─── Lógica principal ─────────────────────────────────────────────────────

    private void startAlarm(int durationSecs) {
        isRunning = true;
        Log.i(TAG, "Alarma iniciada por " + durationSecs + "s");

        setMaxVolume();
        registerVolumeObserver();   // ← NUEVO: vigila intentos de bajar volumen
        startAudioAlarm();
        startVibration();
        showTouchBlockerOverlay();

        stopRunnable = this::stopAlarm;
        handler.postDelayed(stopRunnable, durationSecs * 1000L);
    }

    // ─── Volumen ──────────────────────────────────────────────────────────────

    /** Guarda el volumen actual y sube al máximo. */
    private void setMaxVolume() {
        if (audioManager == null) return;
        savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        maxVolume   = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            maxVolume,
            0   // Sin FLAG_SHOW_UI para no mostrar el slider en pantalla
        );
        Log.d(TAG, "Volumen forzado al máximo: " + maxVolume);
    }

    /**
     * Registra un ContentObserver sobre la URI de volumen del sistema.
     * Cada vez que el volumen cambie (por botones físicos, gestos, apps),
     * el observer lo detecta y lo restablece inmediatamente al máximo.
     */
    private void registerVolumeObserver() {
        volumeObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (!isRunning || audioManager == null) return;

                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                if (currentVolume < maxVolume) {
                    Log.d(TAG, "Intento de bajar volumen detectado ("
                        + currentVolume + "/" + maxVolume + ") — restaurando");
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        maxVolume,
                        0   // Sin UI para que no aparezca el slider
                    );
                }
            }
        };

        // Observar cambios en el volumen de la alarma del sistema
        getContentResolver().registerContentObserver(
            Settings.System.CONTENT_URI,
            true,           // notifyForDescendants: cubre todas las sub-URIs de Settings
            volumeObserver
        );

        Log.d(TAG, "VolumeObserver registrado");
    }

    /** Desregistra el observer al detener la alarma. */
    private void unregisterVolumeObserver() {
        if (volumeObserver != null) {
            try {
                getContentResolver().unregisterContentObserver(volumeObserver);
                Log.d(TAG, "VolumeObserver desregistrado");
            } catch (Exception ignored) {}
            volumeObserver = null;
        }
    }

    // ─── Audio ────────────────────────────────────────────────────────────────

    private void startAudioAlarm() {
        try {
            Uri alarmUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
            mediaPlayer  = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d(TAG, "MediaPlayer iniciado");
        } catch (Exception e) {
            Log.e(TAG, "Error al reproducir alarma: " + e.getMessage());
        }
    }

    // ─── Vibración ────────────────────────────────────────────────────────────

    private void startVibration() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 800, 200};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    // ─── Overlay táctil ───────────────────────────────────────────────────────

    private void showTouchBlockerOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Permiso SYSTEM_ALERT_WINDOW no concedido — overlay omitido");
            return;
        }

        overlayView = new View(this) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return true; // Absorbe todos los toques
            }
        };
        overlayView.setBackgroundColor(0xCC000000);

        TextView label = new TextView(this);
        label.setText("ALARMA BLUETOOTH ACTIVA\nDispositivo desconectado\nEspere...");
        label.setTextColor(0xFFFF4444);
        label.setTextSize(20f);
        label.setGravity(Gravity.CENTER);
        label.setPadding(40, 40, 40, 40);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        try {
            windowManager.addView(overlayView, params);
            Log.d(TAG, "Overlay bloqueador añadido");
        } catch (Exception e) {
            Log.e(TAG, "Error al añadir overlay: " + e.getMessage());
        }
    }

    // ─── Detención ────────────────────────────────────────────────────────────

    private void stopAlarm() {
        if (!isRunning) return;
        isRunning = false;
        Log.i(TAG, "Deteniendo alarma y liberando recursos");

        // 1. Cancelar temporizador automático
        if (stopRunnable != null) {
            handler.removeCallbacks(stopRunnable);
            stopRunnable = null;
        }

        // 2. Desregistrar observer de volumen ANTES de restaurar el volumen
        unregisterVolumeObserver();

        // 3. Detener audio
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        // 4. Restaurar volumen original del usuario
        if (audioManager != null) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM, savedVolume, 0);
            Log.d(TAG, "Volumen restaurado a: " + savedVolume);
        }

        // 5. Detener vibración
        if (vibrator != null) vibrator.cancel();

        // 6. Eliminar overlay
        if (overlayView != null && windowManager != null) {
            try { windowManager.removeView(overlayView); }
            catch (Exception ignored) {}
            overlayView = null;
        }

        stopForeground(true);
        stopSelf();
    }

    // ─── Notificación ─────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Alarma Bluetooth",
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alertas de desconexión Bluetooth");
            channel.enableVibration(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String deviceName, String mac, int duration) {
        Intent stopIntent = new Intent(this, AlarmService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Dispositivo desconectado";
        String text  = (deviceName != null ? deviceName : mac)
                       + " desconectado. Alarma activa " + duration + "s.";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopPending)
            .build();
    }
}
