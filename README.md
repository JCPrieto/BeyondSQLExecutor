# ![BeyondSQLExecutor](src/main/resources/img/icons/database.png) BeyondSQLExecutor

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=JCPrieto_BeyondSQLExecutor&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=JCPrieto_BeyondSQLExecutor)

Lanzador de sentencias SQL de manera masivas contra varias bases de datos: Mysql, MariaDB ó PostgreSQL

### Requisitos ###

* Java 21
* LibNotify (Para las notificaciones en Linux)
* `libsecret-tools` (Opcional, para integración con Secret Manager en Linux)
* Dependencias del instalador .deb:
  * Ubuntu 22.04: `libasound2`
  * Ubuntu 24.04: `libasound2t64`

#### Instalación de dependencias en Ubuntu: ####

* Ubuntu 22.04: `sudo apt install openjdk-21-jre libasound2 libnotify-bin`
* Ubuntu 24.04: `sudo apt install openjdk-21-jre libasound2t64 libnotify-bin`

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
  * CI: se generan dos .deb (Ubuntu 22.04 y 24.04), con sufijos `_ubuntu22.04` y `_ubuntu24.04` en el nombre del
    archivo.

* Windows (MSI):
  * `gradle -PinstallerType=msi -PinstallerIcon=src/main/resources/img/icons/database-installer.ico jpackage`
  * Resultado: `build/jpackage/*.msi`

* macOS (DMG):
  * `gradle -PinstallerType=dmg -PinstallerIcon=src/main/resources/img/icons/database-installer.icns jpackage`
  * Resultado: `build/jpackage/*.dmg`

Iconos de instalador: `src/main/resources/img/icons/database-installer.png`, `.ico`, `.icns`.

### Actualizaciones en Linux vía APT ###

El proyecto publica paquetes `.deb` en el repositorio APT:

* `https://jcprieto.github.io/jklabs-apt-repo`

Configuración en Debian/Ubuntu:

```bash
curl -fsSL https://jcprieto.github.io/jklabs-apt-repo/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/jklabs-archive-keyring.gpg >/dev/null
echo "deb [signed-by=/usr/share/keyrings/jklabs-archive-keyring.gpg] https://jcprieto.github.io/jklabs-apt-repo stable main" | sudo tee /etc/apt/sources.list.d/jklabs.list
sudo apt update
sudo apt install beyondsqlexecutor
```

Después, las nuevas versiones se reciben con:

* `sudo apt upgrade`

### Configuración y seguridad ###

* La configuración del proyecto se guarda en `~/.BeyondSQLExecutor/connections.json`.
* Las credenciales se almacenan cifradas en `~/.BeyondSQLExecutor/.secure/`.
* Logs:
  * Linux: `~/.local/share/BeyondSQLExecutor/logs/`
  * macOS: `~/Library/Application Support/BeyondSQLExecutor/logs/`
  * Windows: `%LOCALAPPDATA%\\BeyondSQLExecutor\\logs\\`
* Importación/Exportación de proyectos desde el menú Archivo (ZIP portable con `connections.json`).
* En exportación, las credenciales se serializan en formato legacy cifrado (sin incluir `.secure/`).
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

Consulta el historial de cambios en [CHANGELOG.md](CHANGELOG.md).

### Notas de CI/CD ###

La documentación de publicación automática de `.deb` hacia el repositorio APT central está en:

* [docs/apt-repository-cicd.md](docs/apt-repository-cicd.md)

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
