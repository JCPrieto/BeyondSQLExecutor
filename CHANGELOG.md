# Changelog

* 1.1.12
  * Refuerzo de la batería de tests de `MainUI`, cubriendo bloqueo y desbloqueo de pantalla, actualización y
    eliminación de servidores, persistencia de configuración y ajuste del divisor en `refresSplit`.
  * Mejora menor de legibilidad en `ScriptPanel` usando pattern matching en el filtrado de componentes seleccionados.
  * `CryptoUtils.AesGcmPayload` añade `toString()` para facilitar depuración y diagnóstico en tests y logs.
  * Actualización de dependencias y tooling: AWS SDK `rds` y `sts` a `2.42.16` y Gradle Wrapper a `9.4.1`.

* 1.1.11
  * Endurecimiento del cifrado portable legacy en `UtilidadesEncryptacion`: los nuevos payloads CBC usan IV aleatorio
    por mensaje, manteniendo compatibilidad de lectura con el formato anterior y renombrando la API de exportación a
    `encryptPortableCompat`.
  * Refactor interno de `LoadSchemaWorker` para inyectar cargador de esquemas y notificador de errores, facilitando
    pruebas unitarias sin instrumentación dinámica ni cambios funcionales en la UI.
  * Nuevos tests unitarios para `LoadSchemaWorker` y `UtilidadesEncryptacion`, cubriendo filtrado de esquemas,
    notificación de errores, IV dinámico y compatibilidad con cifrados legacy previos.
  * Ajuste menor de legibilidad en `ScriptPanel` usando pattern matching en el filtrado de componentes.
  * Actualización de dependencias y tooling: Gradle Wrapper `9.4.0`, plugin SonarQube `7.2.3.7755`, AWS SDK `rds` y
    `sts` a `2.42.6`, y `rsyntaxtextarea` a `3.6.2`.
  * `CryptoUtils.AesGcmPayload` implementa `equals` y `hashCode` para mejorar comparaciones seguras en tests y
    colecciones.

* 1.1.10
  * Endurecimiento adicional del workflow de release (`.github/workflows/release.yml`) añadiendo validaciones
    explícitas también en los pasos de `checkout` para permitir ejecución solo en `workflow_run` de `push` exitoso a
    `main` del mismo repositorio, bloqueando forks.
  * Actualización de dependencias AWS SDK: `software.amazon.awssdk:rds` y `software.amazon.awssdk:sts` de `2.41.32` a
    `2.42.2`.
  * Refactor menor en `SecureStorageDialog` usando pattern matching (`instanceof Integer intValue`) en la asignación
    de prioridad, sin cambios funcionales.
  * Nuevos tests unitarios para `JSonFilter` cubriendo aceptación de directorios, extensión `.json`, rechazo de otras
    extensiones y descripción localizada.

* 1.1.9
  * Endurecimiento adicional del workflow de release: condiciones `if` reescritas y reforzadas para ejecutar solo en
    `push` exitoso a `main` del mismo repositorio y bloquear explícitamente forks en jobs y checkout.
  * Workflow de CI (`gradle.yml`) actualizado con `fetch-depth: 0` para disponer de historial completo en checkout.
  * Mejora de robustez en `SecureStorageDialog`: `rows` del `ProviderTableModel` marcado como `transient` para evitar
    problemas de serialización.
  * Actualización de dependencias: AWS SDK `rds`/`sts` a `2.41.32` y stack de tests JUnit (`junit-jupiter` y
    `junit-platform-launcher`) a `6.0.3`.
  * Nuevos tests unitarios para `ConfigServer` cubriendo selección de login (password/AWS), edición de rol PostgreSQL,
    defaults de layout y validación de formulario.

* 1.1.8
  * Endurecimiento del workflow de release para evitar ejecución/publicación con código de forks en eventos
    `workflow_run`,
    añadiendo validaciones explícitas del repositorio origen y checkout más restrictivo (`persist-credentials: false`,
    `fetch-depth: 1`).
  * Corrección de robustez en `SecureStorageDialog`: `SecureStorageManager` marcado como `transient` para prevenir
    problemas de serialización en el modelo de tabla.
  * Actualización de tooling: plugin de SonarQube en Gradle a `7.2.2.6593`.
  * Nuevos tests unitarios para `CommandRunner` cubriendo captura de `stdout/stderr`, propagación de `stdin`, cierre de
    entrada y manejo de comando inexistente.

* 1.1.7
  * Ajuste del workflow de release para publicar en el repositorio APT solo el `.deb` generado en Ubuntu 22.04,
    evitando ambigüedades entre variantes Linux por versión.
  * Documentación actualizada en `README.md` para aclarar que GitHub Release mantiene artefactos `.deb` de Ubuntu 22.04
    y 24.04, mientras que APT publica una única variante estable.

* 1.1.6
  * Workflow de release reforzado: permisos `contents: write` movidos a nivel de job y validación explícita para no
    ejecutar publicaciones desde forks.
  * Publicación automática de paquetes `.deb` al repositorio APT mediante `repository_dispatch`, incluyendo metadatos de
    distro y URL del artefacto.
  * Documentación ampliada en `README.md` sobre instalación/actualización vía APT y notas de CI/CD del repositorio APT.
  * Nuevos tests unitarios para `AcercaDe` cubriendo etiquetas clicables, listeners de ratón y comportamiento con/sin
    URL en componentes "Powered by".

* 1.1.5
  * Refactor interno del diálogo de configuración de servidor para separar la lógica de login por contraseña y AWS
    profile sin cambiar el comportamiento funcional.
  * Ajuste de robustez en `SecureStorageDialog` marcando `SecureStorageManager` como `transient` para evitar problemas
    de serialización.
  * Simplificación del parser SQL en `ScriptPanel` eliminando una excepción declarada innecesaria en
    `dividirEnSentenciasPostgres`.
  * Actualización de dependencias: `org.postgresql:postgresql` a `42.7.10`, `software.amazon.awssdk:rds` a `2.41.27` y
    `software.amazon.awssdk:sts` a `2.41.27`.
  * Actualización del wrapper de Gradle a `9.3.1` y ajuste de workflows CI/release (JDK 21, cobertura JaCoCo y análisis
    Sonar en `main`).

* 1.1.4
    * Actualiza scripts de instalación para simplificar la gestión de archivos y procesos en Linux.

* 1.1.3
    * Correción de test unitarios.

* 1.1.2
    * Correciones de seguridad y estabilidad.

* 1.1.1
    * Correciones de seguridad y estabilidad.

* 1.1.0
    * Gestión de almacenamiento seguro reforzada: comprobación de proveedor del sistema al iniciar y fallback guiado a
      contraseña maestra.
    * Migración legacy mejorada con aviso en interfaz cuando se detectan contraseñas previas en `config.json`.
    * Exportación de proyectos convertida a formato portable: ZIP con `connections.json` y credenciales cifradas, sin
      carpeta `.secure/`.
    * Experiencia de errores SQL mejorada con panel redimensionable y detalle legible de la sentencia/causa.
    * Ajuste de formulario en configuración de servidor para inicializar correctamente el tipo de login.
    * Actualización de dependencias AWS SDK (`rds` y `sts` a `2.41.22`).
    * Nuevos tests de exportación/importación ZIP para validar portabilidad sin metadatos de vault.

* 1.0.5
    * Panel de servidores: ancho mínimo y divisor ajustado para evitar colapsos
    * Scrollbars horizontales desactivadas en listas de servidores/esquemas
    * Refresco de UI mejorado al cargar esquemas
    * Corrección de typo en textos de internacionalización
    * Actualización de dependencias (MySQL, AWS SDK, JNA)
    * Tests de UI para validar el ajuste del divisor

* 1.0.4
    * Ubicaciones de logs estandarizadas por sistema operativo
    * Tests unitarios para resolver la ruta de logs

* 1.0.3
    * Workflow de release: .deb generado en Ubuntu 22.04 y 24.04 con sufijos `_ubuntu22.04` y `_ubuntu24.04`
    * Documentación actualizada sobre compatibilidad del .deb en CI

* 1.0.2
    * Metadatos de empaquetado añadidos (descripción, licencia y URL del repo)
    * Acerca de actualizado con licencia GPL-3.0 e icono correspondiente
    * Iconos de licencia y aplicación añadidos al README

* 1.0.1
    * Ajustes de empaquetado Linux: nombre/icono correctos en el dock, entrada .desktop y acceso a "Fijar al tablero"

* 1.0.0
    * Instaladores nativos con jpackage para Linux, Windows y macOS

* 0.5.1
    * Cache de iconos y ResourceBundle
    * Fallback de notificaciones con JOptionPane si notify-send no esta disponible
    * Manejo de JSON corrupto con backup seguro
    * Lectura/escritura de JSON en UTF-8
    * Rotacion de logs en ~/.BeyondSQLExecutor
    * Comparacion semver robusta al buscar nuevas versiones
    * Tests de store, growls y logs

* 0.5.0
    * Almacenamiento seguro de credenciales con cifrado AES-GCM y proveedores de clave maestra.
    * Separación de conexiones y secretos, con migración automática desde formatos antiguos.
    * Importación y exportación de proyectos en ZIP.
    * Añadida dependencia JNA para integración con el sistema.

* 0.4.11
    * Nuevo parser de sentencias SQL (DELIMITER, comentarios y $$ en PostgreSQL)
    * Tests unitarios del parser

* 0.4.10
    * Ejecucion SQL basada en Statement.execute y deteccion de ResultSet
    * Test unitario de executeAny

* 0.4.9
    * Null-safety en equals/hashCode y listas de esquemas en Servidor

* 0.4.8
    * Cierre de conexiones al eliminar/editar servidores y al cerrar la aplicacion
    * Tests unitarios para cierre de conexiones en paneles
    * Actualización de dependencias: `aws-java-sdk-rds`, `aws-java-sdk-sts`, `commons-io`, `junit-jupiter`, `postgresql`
      y
      `rsyntaxtextarea`.

* 0.4.7
    * Arranque de la interfaz en el EDT (SwingUtilities.invokeLater)
    * Actualización de dependencias: `aws-java-sdk-rds`, `aws-java-sdk-sts`, `commons-lang3`, `junit-jupiter`,
      `mariadb-java-client` y `postgresql`

* 0.4.6
    * Carga de esquemas y comprobación de nuevas versiones en segundo plano (SwingWorker)

* 0.4.5
    * Sustitución de Firebase por GitHub Releases para la descarga de nuevas versiones

* 0.4.4
    * Mejora de seguridad en el cifrado de contraseñas con migración automática de configuraciones existentes

* 0.4.3
    * Actualización de dependencias: `commons-lang3` a 3.18.0 y `firebase-admin` a 9.7.0

* 0.4.2
    * Correción en el formulario de configuración: Desplegable de regiones AWS

* 0.4.1
    * Correción en la importación de las configuraciones
    * Optimización gestión de conexiones a las BBDD's
    * Actualización a AWS SDK V2

* 0.4.0
    * Ejecución de los scripts en postgre con un rol diferente al del usuario

* 0.3.1
    * Correción de interfaz en el dialogo de creación de conexiones de BBDD

* 0.3.0
    * Coloreado sintactico del editor y varios temas disponibles

* 0.2.1
    * Correción de textos
    * Descarga de nueva versión de acuerdo a la compilación ejecutada.

* 0.2.0
    * Login en BBDD a traves de un perfil AWS
    * Actualización de dependencias

* 0.1.0
    * Añadir solo una pestaña de resultados cuando la select devuelva algún valor.
    * Compilado para Java 17 y superiores

* 0.0.2
    * Corrección del empaquetado
    * Corrección del dialogo "Acerca de"

* 0.0.1
    * Primera versión de la aplicación
