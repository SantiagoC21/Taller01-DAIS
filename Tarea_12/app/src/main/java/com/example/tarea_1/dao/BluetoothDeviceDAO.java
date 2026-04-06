package com.example.tarea_1.dao;

import com.example.tarea_1.model.BluetoothDeviceEntity;
import java.util.List;

/**
 * Interfaz DAO (Data Access Object) para dispositivos Bluetooth.
 * Define el contrato CRUD para la capa de persistencia.
 */
public interface BluetoothDeviceDAO {

    /** Inserta un nuevo dispositivo. Retorna el ID generado, o -1 si falla. */
    long insert(BluetoothDeviceEntity device);

    /** Actualiza un dispositivo existente. Retorna filas afectadas. */
    int update(BluetoothDeviceEntity device);

    /** Elimina un dispositivo por su ID. Retorna filas afectadas. */
    int delete(long id);

    /** Elimina un dispositivo por su dirección MAC. */
    int deleteByMac(String macAddress);

    /** Obtiene un dispositivo por su ID. Retorna null si no existe. */
    BluetoothDeviceEntity findById(long id);

    /** Obtiene un dispositivo por su dirección MAC. Retorna null si no existe. */
    BluetoothDeviceEntity findByMac(String macAddress);

    /** Obtiene todos los dispositivos registrados. */
    List<BluetoothDeviceEntity> findAll();

    /** Obtiene solo los dispositivos con monitoreo activo. */
    List<BluetoothDeviceEntity> findAllMonitored();

    /** Verifica si una MAC ya está registrada. */
    boolean existsByMac(String macAddress);

    /** Actualiza solo el estado de monitoreo de un dispositivo. */
    int updateMonitoredStatus(long id, boolean isMonitored);

    /** Actualiza la duración de alarma de un dispositivo. */
    int updateAlarmDuration(long id, int durationSecs);
}
