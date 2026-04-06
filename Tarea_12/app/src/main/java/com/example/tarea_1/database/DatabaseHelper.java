package com.example.tarea_1.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper SQLite para creación y migración de la base de datos local.
 * Gestiona el esquema de la tabla de dispositivos Bluetooth monitoreados.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // ─── Configuración de la Base de Datos ────────────────────────────────────
    public static final String DB_NAME    = "bluetooth_alarm.db";
    public static final int    DB_VERSION = 1;

    // ─── Tabla: dispositivos_bluetooth ────────────────────────────────────────
    public static final String TABLE_DEVICES         = "bluetooth_devices";
    public static final String COL_ID                = "_id";
    public static final String COL_MAC               = "mac_address";
    public static final String COL_NAME              = "device_name";
    public static final String COL_IS_MONITORED      = "is_monitored";
    public static final String COL_ALARM_DURATION    = "alarm_duration_secs";
    public static final String COL_REGISTERED_AT     = "registered_at";

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static volatile DatabaseHelper instance;

    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ─── Creación del esquema ─────────────────────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createDevicesTable =
            "CREATE TABLE IF NOT EXISTS " + TABLE_DEVICES + " (" +
                COL_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MAC            + " TEXT NOT NULL UNIQUE, "              +
                COL_NAME           + " TEXT NOT NULL, "                     +
                COL_IS_MONITORED   + " INTEGER NOT NULL DEFAULT 1, "        +
                COL_ALARM_DURATION + " INTEGER NOT NULL DEFAULT 30, "       +
                COL_REGISTERED_AT  + " INTEGER NOT NULL"                    +
            ");";

        db.execSQL(createDevicesTable);

        // Índice para búsquedas frecuentes por MAC
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_mac ON " +
                   TABLE_DEVICES + "(" + COL_MAC + ");");

        Log.d(TAG, "Base de datos creada: " + DB_NAME);
    }

    // ─── Migración de versiones ───────────────────────────────────────────────
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Migrando BD de v" + oldVersion + " a v" + newVersion);

        // Estrategia simple: recrear tabla en migraciones futuras.
        // Para producción, usar ALTER TABLE según los cambios reales.
        if (oldVersion < 2) {
            // Ejemplo de migración futura:
            // db.execSQL("ALTER TABLE " + TABLE_DEVICES + " ADD COLUMN new_col TEXT;");
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
