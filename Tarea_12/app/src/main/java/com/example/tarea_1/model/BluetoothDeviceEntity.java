package com.example.tarea_1.model;

/**
 * Entidad que representa un dispositivo Bluetooth monitoreado.
 * Utilizada con el patrón DAO para persistencia en SQLite.
 */
public class BluetoothDeviceEntity {

    private long id;
    private String macAddress;       // Dirección MAC única del dispositivo
    private String deviceName;       // Nombre visible del dispositivo
    private boolean isMonitored;     // ¿Se monitorea este dispositivo?
    private int alarmDurationSecs;   // Duración de la alarma en segundos (default: 30)
    private long registeredAt;       // Timestamp de registro

    public BluetoothDeviceEntity() {
        this.isMonitored = true;
        this.alarmDurationSecs = 30;
        this.registeredAt = System.currentTimeMillis();
    }

    public BluetoothDeviceEntity(String macAddress, String deviceName) {
        this();
        this.macAddress = macAddress;
        this.deviceName = deviceName;
    }

    // ─── Getters y Setters ─────────────────────────────────────────────────────

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public boolean isMonitored() { return isMonitored; }
    public void setMonitored(boolean monitored) { isMonitored = monitored; }

    public int getAlarmDurationSecs() { return alarmDurationSecs; }
    public void setAlarmDurationSecs(int alarmDurationSecs) {
        this.alarmDurationSecs = Math.max(5, Math.min(300, alarmDurationSecs)); // 5s - 300s
    }

    public long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }

    @Override
    public String toString() {
        return "BluetoothDeviceEntity{" +
                "id=" + id +
                ", mac='" + macAddress + '\'' +
                ", name='" + deviceName + '\'' +
                ", monitored=" + isMonitored +
                ", alarmDuration=" + alarmDurationSecs + "s" +
                '}';
    }
}
