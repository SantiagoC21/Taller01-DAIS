🛡️ BlueGuard - Alarma Anti-Robo Bluetooth

BlueGuard es una aplicación de Android nativo diseñada para proteger tu dispositivo mediante un "lazo digital". La aplicación monitorea constantemente la conexión con un dispositivo Bluetooth específico (como audífonos, smartwatches o trackers) y dispara una alarma sonora de alto volumen si la conexión se interrumpe inesperadamente.



🚀 Características

Monitoreo en Tiempo Real: Vigilancia constante del estado del Bluetooth mediante servicios en primer plano (Foreground Services).



Selección de Dispositivo Maestro: Permite elegir qué dispositivo vinculado actuará como "llave" de seguridad.



Alarma de Alto Volumen: La alarma ignora el modo silencio y sube el volumen al máximo al activarse.



Notificaciones Persistentes: Mantiene al usuario informado sobre el estado del monitoreo.



Modo Inteligente: Opción para desactivar la alarma automáticamente si el Wi-Fi de "casa" está conectado.



🛠️ Tecnologías Utilizadas

Lenguaje: Kotlin / Java (Android Nativo).



Componentes de Android:



BroadcastReceiver: Para detectar cambios en el estado del Bluetooth (ACL\_DISCONNECTED).



Foreground Service: Para garantizar que la app no sea cerrada por el sistema en segundo plano.



NotificationManager: Para mostrar alertas críticas.



MediaPlayer: Para la gestión del sonido de la alarma.



Permisos: Bluetooth Connect, Bluetooth Scan, Fine Location (necesario para escaneo en versiones específicas).



📋 Requisitos

Dispositivo con Android 8.0 (Oreo) o superior (API 26+).



Bluetooth 4.0+.



🔧 Instalación y Configuración

Clonar el repositorio:



Bash

git clone https://github.com/tu-usuario/blueguard-antitheft.git

Abrir en Android Studio:

Importa el proyecto y espera a que Gradle sincronice las dependencias.



Configurar Permisos:

Asegúrate de conceder permisos de "Dispositivos cercanos" y "Notificaciones" al ejecutar la app por primera vez.



💡 Cómo funciona

Vincula tu dispositivo Bluetooth preferido en los ajustes del sistema.



Abre BlueGuard y selecciona el dispositivo de la lista.



Activa el Escudo: Presiona el botón de inicio. La app pasará a segundo plano con una notificación activa.



Alerta: Si te alejas demasiado del dispositivo o alguien lo apaga, el teléfono emitirá un sonido de sirena hasta que lo desbloquees y desactives la alerta.



🛡️ Permisos Requeridos

Para que la aplicación funcione correctamente, se deben declarar los siguientes permisos en el AndroidManifest.xml:



XML

<uses-permission android:name="android.permission.BLUETOOTH" />

<uses-permission android:name="android.permission.BLUETOOTH\_CONNECT" />

<uses-permission android:name="android.permission.FOREGROUND\_SERVICE" />

<uses-permission android:name="android.permission.MODIFY\_AUDIO\_SETTINGS" />

🤝 Contribuciones

¡Las contribuciones son bienvenidas! Si tienes ideas para mejorar la detección o añadir nuevas funciones:



Haz un Fork del proyecto.



Crea una rama para tu función (git checkout -b feature/NuevaMejora).



Haz un Commit de tus cambios (git commit -m 'Añadir NuevaMejora').



Haz un Push a la rama (git push origin feature/NuevaMejora).



Abre un Pull Request.



📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo LICENSE para más detalles.



Desarrollado con ❤️ para la seguridad móvil.

