package es.jklabs.utilidades;

import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilidadesBBDD {

    private UtilidadesBBDD() {

    }

    public static List<String> getEsquemas(Servidor servidor) throws ClassNotFoundException, SQLException {
        Class.forName(servidor.getTipoServidor().getDriver());
        List<String> esquemas = new ArrayList<>();
        String url = getURL(servidor);
        try (Connection connection = DriverManager.getConnection(url, servidor.getUser(), UtilidadesEncryptacion.decrypt(servidor.getPass()))) {
            PreparedStatement preparedStatement = null;
            if (servidor.getTipoServidor() == TipoServidor.MYSQL ||
                    servidor.getTipoServidor() == TipoServidor.MARIADB) {
                preparedStatement = connection.prepareStatement("select schema_name as database_name " +
                        "from information_schema.schemata " +
                        "order by schema_name;");
            } else if (servidor.getTipoServidor() == TipoServidor.POSTGRESQL) {
                preparedStatement = connection.prepareStatement("SELECT schema_name FROM information_schema.schemata;");
            }
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
}
