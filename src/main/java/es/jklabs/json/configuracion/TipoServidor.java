package es.jklabs.json.configuracion;

public enum TipoServidor {
    MYSQL("MySQL", "jdbc:mysql://", "com.mysql.jdbc.Driver", "img/icons/mysql.png"),
    MARIADB("MariaDB", "jdbc:mariadb://", "org.mariadb.jdbc.Driver", "img/icons/mariadb.png"),
    POSTGRESQL("PostgreSQL", "jdbc:postgresql://", "org.postgresql.Driver", "img/icons/postgre.png");

    private final String nombre;
    private final String jdbc;
    private final String clase;
    private final String icono;

    TipoServidor(String nombre, String jdbc, String clase, String icono) {
        this.nombre = nombre;
        this.jdbc = jdbc;
        this.clase = clase;
        this.icono = icono;
    }

    public String getIcono() {
        return icono;
    }

    @Override
    public String toString() {
        return nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
