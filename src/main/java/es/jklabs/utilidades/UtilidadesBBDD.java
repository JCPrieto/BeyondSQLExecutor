package es.jklabs.utilidades;

import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.*;

public class UtilidadesBBDD {

    private UtilidadesBBDD() {

    }

    public static List<String> getEsquemas(Servidor servidor) throws ClassNotFoundException, SQLException {
        Class.forName(servidor.getTipoServidor().getDriver());
        List<String> esquemas = new ArrayList<>();
        String url = getURL(servidor);
        try (Connection connection = DriverManager.getConnection(url, servidor.getUser(), UtilidadesEncryptacion.decrypt(servidor.getPass()));
             PreparedStatement preparedStatement = connection.prepareStatement("select schema_name " +
                     "from information_schema.schemata " +
                     "order by schema_name;");) {
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                esquemas.add(rs.getString(1));
            }
        }
        return esquemas;
    }

    public static String getURL(Servidor servidor) {
        String url = servidor.getTipoServidor().getJdbc() + servidor.getHost() + ":" + servidor.getPort();
        if (StringUtils.isNotEmpty(servidor.getDataBase())) {
            url += "/" + servidor.getDataBase();
        }
        return url;
    }

    public static void execute(Servidor servidor, String esquema, String sql) throws ClassNotFoundException, SQLException {
        Class.forName(servidor.getTipoServidor().getDriver());
        String url = getURL(servidor);
        try (Connection connection = DriverManager.getConnection(url, servidor.getUser(), UtilidadesEncryptacion.decrypt(servidor.getPass()))) {
            if (Objects.equals(servidor.getTipoServidor(), TipoServidor.MYSQL) || Objects.equals(servidor.getTipoServidor(), TipoServidor.MARIADB)) {
                connection.setCatalog(esquema);
            } else {
                connection.setSchema(esquema);
            }
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    public static Map.Entry<List<String>, List<Object[]>> executeSelect(Servidor servidor, String esquema,
                                                                        String sentencia) throws ClassNotFoundException, SQLException {
        Class.forName(servidor.getTipoServidor().getDriver());
        String url = getURL(servidor);
        Map.Entry<List<String>, List<Object[]>> entry;
        try (Connection connection = DriverManager.getConnection(url, servidor.getUser(), UtilidadesEncryptacion.decrypt(servidor.getPass()))) {
            if (Objects.equals(servidor.getTipoServidor(), TipoServidor.MYSQL) || Objects.equals(servidor.getTipoServidor(), TipoServidor.MARIADB)) {
                connection.setCatalog(esquema);
            } else {
                connection.setSchema(esquema);
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement(sentencia)) {
                ResultSet rs = preparedStatement.executeQuery();
                List<String> cabecera = new ArrayList<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    cabecera.add(rs.getMetaData().getColumnName(i));
                }
                List<Object[]> valores = new ArrayList<>();
                while (rs.next()) {
                    Object[] registro = new Object[cabecera.size()];
                    for (int i = 0; i < cabecera.size(); i++) {
                        registro[i] = rs.getObject(i + 1);
                    }
                    valores.add(registro);
                }
                entry = new AbstractMap.SimpleEntry<>(cabecera, valores);
            }
        }
        return entry;
    }
}
