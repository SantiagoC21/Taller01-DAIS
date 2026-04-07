# 🔐 Bluetooth Guardian

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![API](https://img.shields.io/badge/API-24%2B-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**Sistema de alarma antirrobo adaptativo basado en desconexión Bluetooth**

[Características](#-características) • [Arquitectura](#-arquitectura) • [Instalación](#-instalación) • [Uso](#-uso) • [Teoría](#-fundamento-teórico)

</div>

---

## 👥 Integrantes

| Nombre | 
|--------|
| **CASTILLO SILVA PEDRO SANTIAGO** |
| **AMAYA ABANTO DIEGO ANTONIO** |
| **YURIVILCA MEJIA OSMAN ANDRE** |

**Universidad Nacional de Ingeniería** - 2026-I

**Curso:** Desarrollo Adaptativo e Integrado de Software

---

## 📱 Descripción

**Bluetooth Guardian** es una aplicación Android de seguridad que monitorea dispositivos Bluetooth vinculados y activa una alarma sonora cuando detecta una desconexión inesperada. Ideal para proteger objetos como audífonos, smartwatches o llaves con Bluetooth.

### ¿Cómo funciona?

```
📡 Dispositivo BT conectado → 🔌 Se desconecta → 🚨 Alarma automática → 🔓 Desbloqueo con huella
```

---

## ✨ Características

| Función | Descripción |
|---------|-------------|
| 🔊 **Alarma a máximo volumen** | Reproduce sonido de alarma al 100%, bloqueando intentos de bajar el volumen |
| 📳 **Vibración continua** | Patrón de vibración persistente durante la alarma |
| 🖥️ **Bloqueo de pantalla** | Overlay que impide interacción táctil hasta autenticarse |
| 🔐 **Desbloqueo biométrico** | Solo se desactiva con huella digital, rostro o PIN del dispositivo |
| 🔄 **Auto-inicio** | El servicio se reinicia automáticamente tras reiniciar el teléfono |
| ⏱️ **Duración configurable** | Alarma de 5 a 300 segundos por dispositivo |
| 💾 **Persistencia SQLite** | Los dispositivos monitoreados se guardan en base de datos local |

---

## 🏗️ Arquitectura

```
📦 com.example.tarea_1
├── 📂 activity
│   └── BiometricActivity.java      # Autenticación biométrica
├── 📂 dao
│   ├── BluetoothDeviceDAO.java     # Interfaz de acceso a datos
│   └── BluetoothDeviceDAOImpl.java # Implementación SQLite
├── 📂 database
│   └── DatabaseHelper.java         # Helper SQLite (Singleton)
├── 📂 model
│   └── BluetoothDeviceEntity.java  # Entidad de dispositivo BT
├── 📂 receiver
│   ├── BluetoothDisconnectReceiver.java  # Detecta desconexiones BT
│   └── BootReceiver.java                 # Reinicia servicio tras boot
├── 📂 service
│   ├── AlarmService.java           # Servicio de alarma (sonido, vibración, overlay)
│   ├── BluetoothMonitorService.java # Monitoreo en segundo plano
│   └── FingerprintService.java     # Auxiliar para biometría
└── MainActivity.java               # Pantalla principal
```

---

## 🔄 Pipeline Adaptativo

```
┌─────────────┐    ┌──────────────────┐    ┌─────────────┐    ┌──────────────┐
│   ENTRADA   │ →  │  PROCESAMIENTO   │ →  │  DECISIÓN   │ →  │  ADAPTACIÓN  │
└─────────────┘    └──────────────────┘    └─────────────┘    └──────────────┘
       │                    │                     │                   │
  • Desconexión BT    • BroadcastReceiver    • isMonitored()?    • Inicia alarma
  • Boot completed    • Consulta BD          • Auth exitosa?     • Muestra overlay
  • Auth biométrica   • Verifica permisos    • API >= 26?        • Reinicia servicio
```

---

## 📋 Requisitos

- **Android:** 7.0+ (API 24)
- **Bluetooth:** Requerido
- **Permisos:**
  - `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` (Android 12+)
  - `SYSTEM_ALERT_WINDOW` (overlay)
  - `USE_BIOMETRIC` (huella/rostro)
  - `RECEIVE_BOOT_COMPLETED` (auto-inicio)
  - `FOREGROUND_SERVICE` (servicio en primer plano)

---

## 🚀 Instalación

### Opción 1: APK directo
```bash
# Descargar desde /app/release/
adb install app/release/Taller01.apk
```

### Opción 2: Compilar desde código
```bash
# Clonar repositorio
git clone <repo-url>
cd Tarea_12

# Compilar con Gradle
./gradlew assembleDebug

# Instalar en dispositivo
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📖 Uso

### 1️⃣ Configuración inicial
1. Abrir la app y conceder permisos de Bluetooth y overlay
2. Pulsar **"Escanear dispositivos"** para ver dispositivos vinculados
3. Seleccionar un dispositivo → **"Registrar"**

### 2️⃣ Activar monitoreo
1. Tocar el dispositivo registrado → **"Activar monitoreo"**
2. (Opcional) Configurar duración de alarma (5-300 segundos)
3. Pulsar **"Iniciar Monitoreo"** para activar el servicio en segundo plano

### 3️⃣ Cuando se activa la alarma
- 🔊 Suena alarma al máximo volumen
- 📳 Vibración continua
- 🖥️ Pantalla bloqueada con overlay
- 🔓 Pulsar **"Desbloquear con huella"** para autenticarse y detener

---

## 🎓 Fundamento Teórico

Esta aplicación es un ejemplo de **Software Adaptativo** según la teoría del curso *Desarrollo Adaptativo e Integrado de Software*.

### Tipos de adaptación implementados:

| Tipo | Implementación en la app |
|------|--------------------------|
| **Auto-adaptativo** | Reacciona automáticamente a eventos del sistema sin intervención humana |
| **Adaptación dinámica** | Ajusta comportamiento en tiempo real según estado de conexión BT |
| **Adaptación basada en reglas** | Condición `isMonitored() == true` determina si activa alarma |
| **Adaptación contextual** | Detecta versión de API y usa `startForegroundService()` o `startService()` |

### Características de software adaptativo cumplidas:

- ✅ **Flexibilidad:** Se ajusta a diferentes versiones de Android (API 24-34)
- ✅ **Automatización:** Decisiones sin intervención humana (BroadcastReceivers)
- ✅ **Resiliencia:** Restaura servicio tras reinicio del dispositivo
- ✅ **Modularidad:** Componentes independientes (DAO, Services, Receivers)

---

## 🧪 Pruebas de verificación

| Escenario | Acción | Resultado esperado |
|-----------|--------|-------------------|
| Desconexión BT | Alejar dispositivo monitoreado | Alarma se activa automáticamente |
| Autenticación | Usar huella con alarma activa | Alarma se detiene, overlay desaparece |
| Reinicio | Reiniciar teléfono | Servicio de monitoreo se reinicia solo |
| Cancelar auth | Cancelar diálogo biométrico | Overlay reaparece, alarma continúa |
| Bajar volumen | Intentar bajar volumen durante alarma | Volumen se restaura al máximo |

---

## 📁 Estructura del proyecto

```
Tarea_12/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/tarea_1/   # Código fuente
│   │   ├── res/                        # Recursos (layouts, strings)
│   │   └── AndroidManifest.xml         # Configuración y permisos
│   ├── build.gradle.kts                # Dependencias del módulo
│   └── release/Taller01.apk            # APK compilado
├── gradle/
│   └── libs.versions.toml              # Catálogo de versiones
├── build.gradle.kts                    # Configuración raíz
└── README.md                           # Este archivo
```

---

## 👨‍💻 Tecnologías

- **Lenguaje:** Java 8
- **SDK:** Android 34 (target), Android 24 (min)
- **Base de datos:** SQLite
- **Biometría:** AndroidX Biometric 1.1.0
- **UI:** Material Design Components
- **Build:** Gradle Kotlin DSL

---

## 📄 Licencia

Este proyecto fue desarrollado como parte del curso **Desarrollo Adaptativo e Integrado de Software** - Universidad Nacional de Ingeniería 2026-I.

---

<div align="center">

**Desarrollado con ❤️ para el curso DAIS**

**Universidad Nacional de Ingeniería** | 2026-I

</div>
