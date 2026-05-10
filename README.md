# 🔒 FocusLock

Aplicación Android para bloquear el teléfono durante un tiempo determinado. Establece una alarma y, cuando llegue la hora, el dispositivo quedará bloqueado hasta que el tiempo expire — sin posibilidad de salir.

Ideal para sesiones de estudio, trabajo en profundidad o simplemente desconectar del móvil.

---

## Características

- **Alarma programable** — elige la hora exacta a la que se activará el bloqueo
- **Duración configurable** — de 5 minutos a 8 horas, en pasos de 5 minutos
- **Pantalla de bloqueo inmersiva** — oculta la barra de navegación y el botón Atrás no funciona
- **Cuenta atrás en tiempo real** — con mensajes motivacionales según el tiempo restante
- **Vigilancia por Accesibilidad** — si consigues salir, la app te devuelve automáticamente al bloqueo
- **Persistencia tras reinicio** — si reinicias el teléfono durante un bloqueo activo, este se reanuda
- **Notificación persistente** — el servicio de bloqueo corre en primer plano para que Android no lo elimine

---

## Requisitos

- Android 8.0 (API 26) o superior
- Android Studio Hedgehog o posterior
- JDK 11+

---

## Instalación y compilación

1. Clona o descarga el repositorio
2. Abre Android Studio → **File → Open** → selecciona la carpeta `FocusLock`
3. Espera a que Gradle sincronice (**File → Sync Project with Gradle Files**)
4. Conecta tu dispositivo Android o crea un emulador
5. Pulsa ▶️ **Run**

---

## Configuración de permisos (primer uso)

Al abrir la app por primera vez, pulsa **"Configurar permisos"** y activa los siguientes:

| Permiso | Obligatorio | Para qué sirve |
|---|---|---|
| Mostrar sobre otras apps | ✅ Sí | Muestra la pantalla de bloqueo encima de cualquier app |
| Servicio de Accesibilidad | ✅ Sí | Detecta si el usuario sale y vuelve al bloqueo |
| Administrador del dispositivo | ⚠️ Recomendado | Bloquea también la pantalla del sistema |

Sin los dos primeros permisos la app no funcionará correctamente.

---

## Uso

1. Abre FocusLock
2. Pulsa **"Establecer alarma"** y elige la hora de activación
3. Ajusta la duración del bloqueo con el deslizador
4. Cuando llegue la hora, el teléfono se bloqueará automáticamente
5. Al terminar el tiempo, la pantalla de bloqueo desaparece sola

---

## Arquitectura

```
com.focuslock
├── ui
│   ├── MainActivity.java          # Pantalla principal (alarma + duración)
│   ├── LockScreenActivity.java    # Pantalla de bloqueo con cuenta atrás
│   └── PermissionsActivity.java   # Guía de configuración de permisos
├── service
│   ├── LockService.java           # Servicio en primer plano que gestiona el bloqueo
│   └── FocusAccessibilityService  # Detecta cambios de ventana y fuerza el retorno
├── receiver
│   ├── AlarmReceiver.java         # Recibe la alarma e inicia el bloqueo
│   └── BootReceiver.java          # Reanuda el bloqueo si el teléfono se reinicia
└── admin
    └── FocusDeviceAdminReceiver   # Administrador del dispositivo (bloqueo del sistema)
```

### Flujo de ejecución

```
Usuario configura alarma
        ↓
AlarmManager dispara a la hora indicada
        ↓
AlarmReceiver → inicia LockService
        ↓
LockService → lanza LockScreenActivity + notificación persistente
        ↓
FocusAccessibilityService vigila cambios de ventana
        ↓ (si el usuario consigue salir)
Vuelve automáticamente a LockScreenActivity
        ↓ (cuando expira el tiempo)
LockService se destruye → LockScreenActivity se cierra
```

---

## Limitaciones conocidas

Android no permite a apps de terceros bloquear el dispositivo al 100% sin ser app de sistema o tener root. Un usuario muy determinado podría:

- Desinstalar la app durante el bloqueo
- Reiniciar en **modo seguro** (los servicios de terceros no arrancan en modo seguro)
- Revocar los permisos desde Ajustes del sistema

FocusLock está diseñado como una **herramienta de productividad personal**, no como control parental ni sistema de seguridad.

---

## Dependencias

```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.core:core:1.12.0'
```

---

## Licencia

MIT License — libre para uso personal y comercial.
