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
│   └── repository/  Repositorio offline-first (Room como fuente de verdad)
├── domain/model/    Station, StationCategory
├── di/              Módulos Hilt (red, base de datos, repositorio, coroutines)
├── playback/        PlaybackService, PlayerController, reconexión, Sleep Timer
└── ui/              Compose: tema, componentes, inicio, favoritos, reproductor
```

## UI

- Barra superior con menú (actualizar estaciones), título y buscador; chips de categoría debajo.
- Grilla de 2 columnas con logo (Coil, con fallback), nombre, dial y corazón de favorito.
- `Modifier.shimmerEffect()` en los placeholders mientras carga la primera lista.
- `FloatingMiniPlayer` sobre la barra de navegación con badge EN VIVO y Play/Pause
  (spinner mientras hace buffering o reconecta).
- `NowPlayingSheet` (`ModalBottomSheet`) con logo grande, volumen y Sleep Timer de 15, 30 o 60 min.

## Motor de audio

`PlaybackService` hereda de `MediaSessionService`:

- Se ejecuta como foreground service con notificación multimedia mientras suena.
- Maneja el audio focus: pausa con llamadas, baja el volumen con notificaciones y pausa
  al desconectar audífonos.
- Reconecta solo ante cortes de red: primero reintenta dentro de ExoPlayer con backoff
  exponencial y, si el corte es largo, espera a que vuelva la red y reconecta al borde en vivo.
- Expone el Sleep Timer como comando de sesión (`PlaybackContract.ACTION_SET_SLEEP_TIMER`)
  y publica su estado y el de reconexión en los extras de sesión.

## Diseño "Cordillera"

Azul noche (#0D1221) con el rojo (#FF4F5A) y el blanco de la bandera. El ícono adaptativo
(con versión monocromática para Android 13+) combina una estrella con un ecualizador con forma
de cerros. Al abrir la app, el splash del sistema da paso a `IntroAnimation`: las barras laten,
se dibuja el perfil de la cordillera, la estrella gira hasta su lugar y aparece el nombre.

Tipografías Unbounded (títulos) y Manrope (texto), incluidas en `res/font` bajo la
SIL Open Font License.

## Compilar

Requiere Android Studio Ladybug o superior (JDK 17).

```
./gradlew assembleDebug testDebugUnitTest
```

## Fases

1. Configuración, datos y audio ✅
2. ViewModels y UI en Compose ✅
3. Diseño Cordillera: ícono, animación de inicio y tema ✅
