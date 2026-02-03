# MediLocator UNI

Aplicación creada como examen final del curso Programación Movil de la Universidad nacional de ingeniería

## ¿Qué es?

Aplicación Android para localizar centros médicos cercanos según especialidades médicas. Utiliza Google Maps para mostrar ubicaciones y calcular rutas, con Firebase Realtime Database para gestión de datos.

## Requisitos

- Android Studio Hedgehog o superior
- JDK 17
- SDK de Android (minSdk 21, targetSdk 34)
- Cuenta de Firebase con proyecto configurado
- API Key de Google Maps habilitada
- Dispositivo Android o emulador con Google Play Services

## Quickstart

### Instalar

1. Clonar el repositorio:

    ```bash
    git clone <url-repositorio>
    cd medilocator
    ```

2. Abrir el proyecto en Android Studio

### Configurar

1. Configurar Firebase:
   - Descargar `google-services.json` desde la consola de Firebase
   - Colocar el archivo en `app/google-services.json`
   - Configurar Firebase Realtime Database con la URL: `https://mykhospitales-moviles-default-rtdb.firebaseio.com/`

2. Configurar Google Maps:
   - Obtener API Key de Google Cloud Console
   - Crear archivo `app/src/main/res/values/strings.xml` con:

   ```xml
   <string name="google_maps_key">TU_API_KEY_AQUI</string>
   ```

3. Sincronizar dependencias:

```bash
./gradlew build
```

### Ejecutar

1. Conectar dispositivo Android o iniciar emulador
2. En Android Studio: Run > Run 'app'
3. O desde terminal:

```bash
./gradlew installDebug
```

## Configuración

Variables críticas en `app/build.gradle`:

- `applicationId`: Identificador único de la app (actualmente: `com.example.mykfirebase1`)
- `minSdk`: Versión mínima de Android (21 = Android 5.0)
- `targetSdk`: Versión objetivo de Android (34 = Android 14)

Firebase Database URL en `MainActivity.kt`:

```kotlin
FirebaseDatabase.getInstance("https://mykhospitales-moviles-default-rtdb.firebaseio.com/")
```

## Uso

1. Al iniciar la app, el mapa muestra tu ubicación actual
2. Seleccionar especialidad médica del menú desplegable
3. Presionar "Ubicar Centro Médico Más Cercano" para:
   - Ver centros marcados en el mapa
   - Calcular ruta al centro más cercano
4. Menú superior:
   - Ver lista de especialidades
   - Gestionar centros médicos (agregar/editar/eliminar)

## Estructura

```txt
app/src/main/java/com/example/mykfirebase1/
├── MainActivity.kt                    # Actividad principal con mapa
├── ListaEspecialidadesActivity.kt    # Lista de especialidades
├── CentroListaActivity.kt            # Gestión de centros médicos
├── AgregarEditarCentroActivity.kt    # Formulario de centros
├── CentroMedico.kt                   # Modelo de datos
├── Especialidad.kt                   # Modelo de especialidad
├── CentroAdaptador.kt                # Adaptador RecyclerView centros
└── EspecialidadAdaptador.kt          # Adaptador RecyclerView especialidades

app/src/main/res/
├── layout/                           # Layouts XML
├── values/strings.xml                # Configurar API Key aquí
└── AndroidManifest.xml               # Permisos y configuración
```

## Problemas comunes

**Error: "Google Maps no carga"**

- Verificar que la API Key esté correctamente configurada en `strings.xml`
- Confirmar que Google Maps SDK está habilitado en Google Cloud Console

**Error: "Sin datos de Firebase"**

- Verificar conexión a internet
- Confirmar que `google-services.json` esté en `app/`
- Revisar reglas de Firebase Database (deben permitir lectura/escritura)

**La ubicación no funciona**

- Aceptar permisos de ubicación cuando la app lo solicite
- Activar GPS en el dispositivo
- En emulador: usar Extended Controls para establecer ubicación
