package com.example.tarea_1.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.example.tarea_1.service.AlarmService;
import java.util.concurrent.Executor;

public class BiometricActivity extends AppCompatActivity {

    private static final String TAG = "BiometricActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Mantener pantalla encendida y mostrar sobre lock screen
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        
        // Layout mínimo con fondo semitransparente
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(0x99000000);
        
        TextView text = new TextView(this);
        text.setText("Autenticando...");
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(18f);
        text.setGravity(Gravity.CENTER);
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        );
        layout.addView(text, params);
        setContentView(layout);
        
        // Verificar si el dispositivo soporta autenticación biométrica o credenciales
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK | 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt();
        } else {
            Log.e(TAG, "Autenticación biométrica no disponible. Código: " + canAuthenticate);
            Toast.makeText(this, "Autenticación no disponible en este dispositivo", Toast.LENGTH_LONG).show();
            showOverlayAgain();
            finish();
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    Log.i(TAG, "Autenticación por huella exitosa");
                    Toast.makeText(getApplicationContext(), "Alarma desactivada", Toast.LENGTH_SHORT).show();
                    stopAlarmService();
                    finish(); // Cierra esta actividad transparente
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Log.w(TAG, "Error de autenticación o cancelado por usuario: " + errString);
                    // Si el usuario cancela, restauramos el overlay y la alarma sigue sonando
                    showOverlayAgain();
                    finish();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Log.w(TAG, "Autenticación fallida - huella no reconocida");
                    // No cerramos, el usuario puede reintentar
                }
            });

        // Permitir huella, rostro, iris O credenciales del dispositivo (PIN/patrón/contraseña)
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desactivar Alarma")
            .setSubtitle("Autentícate para detener la alerta")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void stopAlarmService() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(AlarmService.ACTION_STOP_ALARM);
        startService(intent);
    }

    /**
     * Restaura el overlay si el usuario cancela la autenticación.
     */
    private void showOverlayAgain() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction(AlarmService.ACTION_SHOW_OVERLAY);
        startService(intent);
    }
}
