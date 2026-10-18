# Mi Radio

App nativa Android para escuchar radios chilenas en vivo, usando la API pública de
[Radio Browser](https://api.radio-browser.info).

**Stack:** Kotlin · Jetpack Compose (Material 3) · MVVM · Coroutines/StateFlow · Hilt ·
Retrofit + kotlinx.serialization · Room · Media3 (ExoPlayer + MediaSessionService) · Coil.

## Estructura

```
app/src/main/java/cl/miradio/app/
├── data/
│   ├── remote/      API de Radio Browser, User-Agent y failover entre servidores espejo
│   ├── local/       Room: caché de estaciones y tabla de favoritos
│   └── mapper/      DTO → entidad → modelo de dominio (incluye extracción del dial)
├── domain/model/    Station
├── di/              Módulos Hilt (red, base de datos, coroutines)
└── playback/        PlaybackService, reconexión, Sleep Timer, MediaItem
```

## Motor de audio

`PlaybackService` hereda de `MediaSessionService`:

- Se ejecuta como foreground service con notificación multimedia mientras suena.
- Maneja el audio focus: pausa con llamadas, baja el volumen con notificaciones y pausa
  al desconectar audífonos.
- Reconecta solo ante cortes de red: primero reintenta dentro de ExoPlayer con backoff
  exponencial y, si el corte es largo, espera a que vuelva la red y reconecta al borde en vivo.
- Expone el Sleep Timer como comando de sesión (`PlaybackContract.ACTION_SET_SLEEP_TIMER`)
  y publica su estado y el de reconexión en los extras de sesión.

## Compilar

Requiere Android Studio Ladybug o superior (JDK 17).

```
./gradlew assembleDebug testDebugUnitTest
```

## Fases

1. Configuración, datos y audio ✅
2. ViewModels y UI en Compose
3. Integración y pulido
