# BeyondSQLExecutor

Lanzador de sentencias SQL de manera masivas contra varias bases de datos: Mysql, MariaDB ó PostgreSQL

### Requisitos ###

* Java 17
* LibNotify (Para las notificaciones en Linux)

### Ejecución ###

* Windows:
  * Ejecutar BeyondSQLExecutor.bat dentro del directorio bin

* Linux:
  * Ejecutar BeyondSQLExecutor.sh dentro del directorio bin

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
  * Rsyntaxtextarea https://bobbylight.github.io/RSyntaxTextArea/

### Changelog ###

* 0.4.7
  * Arranque de la interfaz en el EDT (SwingUtilities.invokeLater)
  * Actualizacion de dependencias: `commons-lang3`, `mariadb-java-client`, `postgresql`, `rds`, `sts` y `junit-jupiter`

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
