# Study Pulse

Aplicación Android nativa en Kotlin con layouts XML, ViewBinding y arquitectura MVVM.

## Abrir y ejecutar

1. Abre la carpeta Study Pulse desde Android Studio.
2. Deja que Android Studio sincronice Gradle e instale los componentes del SDK que solicite.
3. Ejecuta la configuración app en un emulador o dispositivo Android 7.0 o posterior.

El proyecto usa Gradle 9.6 y JDK 17 o superior. Android Studio detecta la ruta local del SDK en cada equipo.

### Configuración de Google Books

Google Books requiere una API key para consultar datos públicos. Configúrala así:

1. Entra a [Google Cloud Console](https://console.cloud.google.com/) e inicia sesión.
2. Selecciona un proyecto o crea uno nuevo.
3. Abre **APIs y servicios > Biblioteca**, busca **Books API** y pulsa **Habilitar**.
4. Abre **APIs y servicios > Credenciales > Crear credenciales > Clave de API**.
5. En **Restricciones de la API**, selecciona **Restringir clave** y deja marcada únicamente **Books API**.
6. En la carpeta principal del proyecto, abre `local.properties` (junto a `settings.gradle.kts`) y agrega la línea siguiente, sustituyendo el texto por tu clave real, sin comillas:

```properties
googleBooksApiKey=TU_API_KEY
```

7. Guarda el archivo y en Android Studio pulsa **File > Sync Project with Gradle Files**. Después ejecuta de nuevo la app.

No pegues la clave en el código ni la compartas en capturas, chats o repositorios públicos. `local.properties` es un archivo local y no se incluye en el ZIP del proyecto.

## Funcionalidad

- Registro e inicio de sesión local; las materias se agregan desde Inicio.
- Búsqueda de universidades mexicanas en Hipolabs con debounce de 300 ms, sugerencias locales y manejo de errores de conexión.
- Panel con materias en tarjetas de color; permite agregar y quitar materias.
- Creación de asignaciones por materia, fecha de entrega, estado completado y eliminación.
- Cierre de sesión con regreso al login; eliminación de cuenta con confirmación por contraseña y limpieza de materias y asignaciones.
- Carrusel de Google Books en Inicio y biblioteca completa en cuadrícula de dos columnas.
- Carga de portadas con Glide y estados de carga, lista vacía y error.

## Organización

- model: respuestas JSON de Hipolabs y Google Books.
- network: servicios Retrofit y almacenamiento SQLite del perfil y las asignaciones.
- view: Activities, adaptadores y layouts con ViewBinding.
- viewmodel: estados de pantalla, llamadas a API y trabajo con Kotlin Coroutines.
