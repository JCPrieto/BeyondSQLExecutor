package es.jklabs.json.configuracion;

public enum TipoServidor {
    MYSQL("MySQL", "", ""),
    MARIADB("MariaDB", "", ""),
    POSTGRESQL("PostgreSQL", "", "");

    private final String nombre;
    private final String jdbc;
    private String icono;

    TipoServidor(String nombre, String jdbc, String icono) {
        this.nombre = nombre;
        this.jdbc = jdbc;
        this.icono = icono;
    }

    public String getIcono() {
        return icono;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
