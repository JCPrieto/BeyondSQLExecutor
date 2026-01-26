# ![BeyondSQLExecutor](src/main/resources/img/icons/database.png) BeyondSQLExecutor

Lanzador de sentencias SQL de manera masivas contra varias bases de datos: Mysql, MariaDB ó PostgreSQL

### Requisitos ###

* Java 21
* LibNotify (Para las notificaciones en Linux)

### Ejecución ###

* Windows:
  * Ejecutar BeyondSQLExecutor.bat dentro del directorio bin

* Linux:
  * Ejecutar BeyondSQLExecutor.sh dentro del directorio bin

### Instaladores nativos (jpackage) ###

Los instaladores se generan en el sistema operativo de destino.

* Linux (DEB por defecto):
  * `gradle jpackage`
  * Alternativa: `gradle -PinstallerType=rpm jpackage`
  * Resultado: `build/jpackage/*.deb` o `build/jpackage/*.rpm`

* Windows (MSI):
  * `gradle -PinstallerType=msi -PinstallerIcon=src/main/resources/img/icons/database-installer.ico jpackage`
  * Resultado: `build/jpackage/*.msi`

* macOS (DMG):
  * `gradle -PinstallerType=dmg -PinstallerIcon=src/main/resources/img/icons/database-installer.icns jpackage`
  * Resultado: `build/jpackage/*.dmg`

Iconos de instalador: `src/main/resources/img/icons/database-installer.png`, `.ico`, `.icns`.

### Configuración y seguridad ###

* La configuración del proyecto se guarda en `~/.BeyondSQLExecutor/connections.json`.
* Las credenciales se almacenan cifradas en `~/.BeyondSQLExecutor/.secure/`.
* Importación/Exportación de proyectos desde el menú Archivo (ZIP con `connections.json` y `.secure/`).
* Si el almacenamiento del sistema no está disponible, se solicitará contraseña maestra.

### Tecnologías utilizadas ###

* Iconos: Papirus https://github.com/PapirusDevelopmentTeam/papirus-icon-theme
* Librerias:
  * Apache Commons Lang http://commons.apache.org/proper/commons-lang
  * GSon https://github.com/google/gson
  * JAXB https://github.com/javaee/jaxb-v2
  * MySQL https://www.mysql.com
  * MariaDB https://mariadb.org
  * PostgreSQL https://www.postgresql.org
  * Apache Commons IO http://commons.apache.org/proper/commons-io
  * AWS Amazon RDS https://aws.amazon.com/sdkforjava
  * AWS Amazon STS https://aws.amazon.com/sdkforjava
  * JNA https://github.com/java-native-access/jna
  * Rsyntaxtextarea https://bobbylight.github.io/RSyntaxTextArea/

### Changelog ###

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
  * Actualización de dependencias: `aws-java-sdk-rds`, `aws-java-sdk-sts`, `commons-io`, `junit-jupiter`, `postgresql` y
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

### Licencia ###

![Licencia GPL v3](src/main/resources/img/icons/gplv3-with-text-136x68.png)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses
