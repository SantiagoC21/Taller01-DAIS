package com.example.tarea_1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.tarea_1.dao.BluetoothDeviceDAO;
import com.example.tarea_1.dao.BluetoothDeviceDAOImpl;
import com.example.tarea_1.model.BluetoothDeviceEntity;
import com.example.tarea_1.service.AlarmService;
import com.example.tarea_1.service.BluetoothMonitorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Actividad principal de la app de alarma Bluetooth.
 *
 * Funciones:
 *  - Listar dispositivos Bluetooth emparejados.
 *  - Registrarlos / quitarlos del monitoreo (DAO).
 *  - Configurar la duración de la alarma.
 *  - Iniciar / detener el servicio de monitoreo en segundo plano.
 *  - Solicitar permisos necesarios.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_BLUETOOTH_PERMS = 100;
    private static final int REQ_OVERLAY_PERM    = 101;

    // ─── Componentes de UI ────────────────────────────────────────────────────
    private ListView   listDevices;
    private Button     btnScanPaired;
    private Button     btnStartMonitor;
    private Button     btnStopMonitor;
    private TextView   tvStatus;

    // ─── Lógica ───────────────────────────────────────────────────────────────
    private BluetoothAdapter   bluetoothAdapter;
    private BluetoothDeviceDAO dao;
    private ExecutorService    executor = Executors.newSingleThreadExecutor();

    private List<BluetoothDevice>  pairedList    = new ArrayList<>();
    private List<String>           pairedLabels  = new ArrayList<>();
    private ArrayAdapter<String>   listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao             = new BluetoothDeviceDAOImpl(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bindViews();
        setupListView();
        checkPermissions();
        checkOverlayPermission();
        updateStatus();
    }

    // ─── Inicialización de vistas ─────────────────────────────────────────────

    private void bindViews() {
        listDevices    = findViewById(R.id.listDevices);
        btnScanPaired  = findViewById(R.id.btnScanPaired);
        btnStartMonitor = findViewById(R.id.btnStartMonitor);
        btnStopMonitor  = findViewById(R.id.btnStopMonitor);
        tvStatus        = findViewById(R.id.tvStatus);

        btnScanPaired.setOnClickListener(v -> scanPairedDevices());
        btnStartMonitor.setOnClickListener(v -> startMonitorService());
        btnStopMonitor.setOnClickListener(v  -> stopMonitorService());
    }

    private void setupListView() {
        listAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1, pairedLabels);
        listDevices.setAdapter(listAdapter);

        // Toque en un dispositivo → diálogo de opciones
        listDevices.setOnItemClickListener((parent, view, position, id) -> {
            if (position < pairedList.size()) {
                showDeviceOptionsDialog(pairedList.get(position));
            }
        });
    }

    // ─── Escaneo de dispositivos emparejados ──────────────────────────────────

    private void scanPairedDevices() {
        if (bluetoothAdapter == null) {
            toast("Bluetooth no disponible en este dispositivo");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            toast("Activa el Bluetooth primero");
            return;
        }
        if (!hasBluetoothPermissions()) {
            checkPermissions();
            return;
        }

        pairedList.clear();
        pairedLabels.clear();

        try {
            Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
            if (bonded == null || bonded.isEmpty()) {
                toast("No hay dispositivos emparejados");
                return;
            }

            for (BluetoothDevice device : bonded) {
                pairedList.add(device);
                String label = (device.getName() != null ? device.getName() : "Sin nombre")
                               + "\n" + device.getAddress();
                pairedLabels.add(label);
            }
            listAdapter.notifyDataSetChanged();
            tvStatus.setText("Dispositivos emparejados encontrados: " + pairedList.size());

        } catch (SecurityException e) {
            toast("Permiso Bluetooth denegado");
        }
    }

    // ─── Diálogo de opciones por dispositivo ──────────────────────────────────

    private void showDeviceOptionsDialog(BluetoothDevice btDevice) {
        String mac  = btDevice.getAddress();
        String name = "";
        try { name = btDevice.getName() != null ? btDevice.getName() : mac; }
        catch (SecurityException e) { name = mac; }

        final String deviceName = name;

        executor.execute(() -> {
            BluetoothDeviceEntity existing = dao.findByMac(mac);
            boolean isRegistered = existing != null;

            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(deviceName);

                if (!isRegistered) {
                    // Dispositivo NO registrado → opción de registrar
                    builder.setMessage("¿Registrar este dispositivo para monitoreo de alarma?");
                    builder.setPositiveButton("Registrar", (d, w) ->
                        registerDevice(btDevice, deviceName));
                    builder.setNegativeButton("Cancelar", null);
                } else {
                    // Dispositivo YA registrado → opciones de configuración
                    String[] options = {
                        existing.isMonitored()
                            ? "Desactivar monitoreo"
                            : "Activar monitoreo",
                        "Configurar duración alarma (" + existing.getAlarmDurationSecs() + "s)",
                        "Eliminar dispositivo"
                    };
                    builder.setItems(options, (d, which) -> {
                        switch (which) {
                            case 0: toggleMonitoring(existing); break;
                            case 1: showDurationDialog(existing); break;
                            case 2: removeDevice(existing); break;
                        }
                    });
                }

                builder.setNeutralButton("Cerrar", null);
                builder.show();
            });
        });
    }

    // ─── Operaciones DAO ──────────────────────────────────────────────────────

    private void registerDevice(BluetoothDevice btDevice, String name) {
        executor.execute(() -> {
            BluetoothDeviceEntity entity = new BluetoothDeviceEntity(btDevice.getAddress(), name);
            long id = dao.insert(entity);
            runOnUiThread(() -> {
                if (id > 0) {
                    toast("✓ " + name + " registrado para monitoreo");
                } else {
                    toast("Ya estaba registrado o error al insertar");
                }
                updateStatus();
            });
        });
    }

    private void toggleMonitoring(BluetoothDeviceEntity entity) {
        executor.execute(() -> {
            boolean newState = !entity.isMonitored();
            dao.updateMonitoredStatus(entity.getId(), newState);
            runOnUiThread(() -> {
                toast(entity.getDeviceName() + ": monitoreo " +
                      (newState ? "activado ✓" : "desactivado"));
                updateStatus();
            });
        });
    }

    private void showDurationDialog(BluetoothDeviceEntity entity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Duración de la alarma (segundos)");

        final EditText input = new EditText(this);
        input.setText(String.valueOf(entity.getAlarmDurationSecs()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Guardar", (d, w) -> {
            try {
                int secs = Integer.parseInt(input.getText().toString().trim());
                if (secs < 5 || secs > 300) {
                    toast("Ingresa un valor entre 5 y 300 segundos");
                    return;
                }
                executor.execute(() -> {
                    dao.updateAlarmDuration(entity.getId(), secs);
                    runOnUiThread(() ->
                        toast("Duración actualizada a " + secs + " segundos")
                    );
                });
            } catch (NumberFormatException e) {
                toast("Número inválido");
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void removeDevice(BluetoothDeviceEntity entity) {
        new AlertDialog.Builder(this)
            .setTitle("Eliminar dispositivo")
            .setMessage("¿Eliminar " + entity.getDeviceName() + " del monitoreo?")
            .setPositiveButton("Eliminar", (d, w) -> {
                executor.execute(() -> {
                    dao.delete(entity.getId());
                    runOnUiThread(() -> {
                        toast(entity.getDeviceName() + " eliminado");
                        updateStatus();
                    });
                });
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    // ─── Control del servicio de monitoreo ───────────────────────────────────

    private void startMonitorService() {
        Intent intent = new Intent(this, BluetoothMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        toast("Monitoreo Bluetooth iniciado");
        tvStatus.setText("Estado: MONITOREANDO activamente");
    }

    private void stopMonitorService() {
        Intent intent = new Intent(this, BluetoothMonitorService.class);
        stopService(intent);

        Intent alarmStop = new Intent(this, AlarmService.class);
        alarmStop.setAction(AlarmService.ACTION_STOP_ALARM);
        startService(alarmStop);

        toast("Monitoreo detenido");
        tvStatus.setText("Estado: INACTIVO");
    }

    // ─── Estado ───────────────────────────────────────────────────────────────

    private void updateStatus() {
        executor.execute(() -> {
            List<BluetoothDeviceEntity> monitored = dao.findAllMonitored();
            runOnUiThread(() ->
                tvStatus.setText("Dispositivos monitoreados: " + monitored.size())
            );
        });
    }

    // ─── Permisos ─────────────────────────────────────────────────────────────

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[0]), REQ_BLUETOOTH_PERMS);
        }
    }

    private void checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                .setTitle("Permiso requerido")
                .setMessage("Para bloquear la pantalla táctil durante la alarma, " +
                            "se necesita el permiso 'Mostrar sobre otras apps'.")
                .setPositiveButton("Configurar", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_OVERLAY_PERM);
                })
                .setNegativeButton("Omitir", null)
                .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BLUETOOTH_PERMS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                toast("Se necesitan permisos Bluetooth para funcionar correctamente");
            }
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
