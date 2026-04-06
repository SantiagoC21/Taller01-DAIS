package com.example.tarea_1.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.tarea_1.database.DatabaseHelper;
import com.example.tarea_1.model.BluetoothDeviceEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación concreta del DAO para dispositivos Bluetooth.
 * Todas las operaciones CRUD sobre SQLite se encapsulan aquí.
 */
public class BluetoothDeviceDAOImpl implements BluetoothDeviceDAO {

    private static final String TAG = "BluetoothDeviceDAO";

    private final DatabaseHelper dbHelper;

    public BluetoothDeviceDAOImpl(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    // ─── INSERT ───────────────────────────────────────────────────────────────

    @Override
    public long insert(BluetoothDeviceEntity device) {
        if (device == null || device.getMacAddress() == null) return -1;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues cv = toContentValues(device);
            long id = db.insertOrThrow(DatabaseHelper.TABLE_DEVICES, null, cv);
            Log.d(TAG, "Dispositivo insertado: id=" + id + " mac=" + device.getMacAddress());
            return id;
        } catch (SQLException e) {
            Log.e(TAG, "Error al insertar dispositivo: " + e.getMessage());
            return -1;
        }
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Override
    public int update(BluetoothDeviceEntity device) {
        if (device == null || device.getId() <= 0) return 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = toContentValues(device);
        int rows = db.update(
            DatabaseHelper.TABLE_DEVICES,
            cv,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(device.getId())}
        );
        Log.d(TAG, "Dispositivo actualizado: rows=" + rows);
        return rows;
    }

    @Override
    public int updateMonitoredStatus(long id, boolean isMonitored) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_IS_MONITORED, isMonitored ? 1 : 0);
        return db.update(
            DatabaseHelper.TABLE_DEVICES,
            cv,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(id)}
        );
    }

    @Override
    public int updateAlarmDuration(long id, int durationSecs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        // Clamp: mínimo 5s, máximo 300s
        int clamped = Math.max(5, Math.min(300, durationSecs));
        cv.put(DatabaseHelper.COL_ALARM_DURATION, clamped);
        return db.update(
            DatabaseHelper.TABLE_DEVICES,
            cv,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(id)}
        );
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Override
    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
            DatabaseHelper.TABLE_DEVICES,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(id)}
        );
    }

    @Override
    public int deleteByMac(String macAddress) {
        if (macAddress == null) return 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
            DatabaseHelper.TABLE_DEVICES,
            DatabaseHelper.COL_MAC + " = ?",
            new String[]{macAddress}
        );
    }

    // ─── QUERIES ──────────────────────────────────────────────────────────────

    @Override
    public BluetoothDeviceEntity findById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_DEVICES,
            null,
            DatabaseHelper.COL_ID + " = ?",
            new String[]{String.valueOf(id)},
            null, null, null, "1"
        );
        return extractSingle(cursor);
    }

    @Override
    public BluetoothDeviceEntity findByMac(String macAddress) {
        if (macAddress == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_DEVICES,
            null,
            DatabaseHelper.COL_MAC + " = ?",
            new String[]{macAddress},
            null, null, null, "1"
        );
        return extractSingle(cursor);
    }

    @Override
    public List<BluetoothDeviceEntity> findAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_DEVICES,
            null, null, null, null, null,
            DatabaseHelper.COL_NAME + " ASC"
        );
        return extractList(cursor);
    }

    @Override
    public List<BluetoothDeviceEntity> findAllMonitored() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_DEVICES,
            null,
            DatabaseHelper.COL_IS_MONITORED + " = 1",
            null, null, null,
            DatabaseHelper.COL_NAME + " ASC"
        );
        return extractList(cursor);
    }

    @Override
    public boolean existsByMac(String macAddress) {
        return findByMac(macAddress) != null;
    }

    // ─── Helpers de mapeo Cursor ↔ Entity ────────────────────────────────────

    private ContentValues toContentValues(BluetoothDeviceEntity d) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_MAC,            d.getMacAddress());
        cv.put(DatabaseHelper.COL_NAME,           d.getDeviceName());
        cv.put(DatabaseHelper.COL_IS_MONITORED,   d.isMonitored() ? 1 : 0);
        cv.put(DatabaseHelper.COL_ALARM_DURATION, d.getAlarmDurationSecs());
        cv.put(DatabaseHelper.COL_REGISTERED_AT,  d.getRegisteredAt());
        return cv;
    }

    private BluetoothDeviceEntity cursorToEntity(Cursor cursor) {
        BluetoothDeviceEntity entity = new BluetoothDeviceEntity();
        entity.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)));
        entity.setMacAddress(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MAC)));
        entity.setDeviceName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)));
        entity.setMonitored(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IS_MONITORED)) == 1);
        entity.setAlarmDurationSecs(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ALARM_DURATION)));
        entity.setRegisteredAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REGISTERED_AT)));
        return entity;
    }

    private BluetoothDeviceEntity extractSingle(Cursor cursor) {
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToEntity(cursor);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private List<BluetoothDeviceEntity> extractList(Cursor cursor) {
        List<BluetoothDeviceEntity> list = new ArrayList<>();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursorToEntity(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }
}
