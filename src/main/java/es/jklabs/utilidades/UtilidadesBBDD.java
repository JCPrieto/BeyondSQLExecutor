package es.jklabs.utilidades;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.*;

public class UtilidadesBBDD {

    private UtilidadesBBDD() {

    }

    public static List<String> getEsquemas(Servidor servidor) throws ClassNotFoundException, SQLException {
        Class.forName(servidor.getTipoServidor().getDriver());
        List<String> esquemas = new ArrayList<>();
        try (Connection connection = getConexion(servidor);
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

    public static Connection getConexion(Servidor servidor) throws SQLException {
        String url = getURL(servidor);
        String pass = getPass(servidor);
        return DriverManager.getConnection(url, servidor.getUser(), pass);
    }

    private static String getPass(Servidor servidor) {
        String pass;
        if (Objects.equals(servidor.getTipoLogin(), TipoLogin.AWS_PROFILE)) {
            pass = getRdsIamToken(servidor);
        } else {
            pass = UtilidadesEncryptacion.decrypt(servidor.getPass());
        }
        return pass;
    }

    private static String getRdsIamToken(Servidor servidor) {
        RdsIamAuthTokenGenerator generator = RdsIamAuthTokenGenerator.builder()
                .credentials(new ProfileCredentialsProvider(servidor.getAwsProfile()))
                .region(servidor.getRegion().getName())
                .build();
        return generator.getAuthToken(
                GetIamAuthTokenRequest.builder()
                        .hostname(servidor.getHost())
                        .port(Integer.parseInt(servidor.getPort()))
                        .userName(servidor.getUser())
                        .build());
    }

    public static String getURL(Servidor servidor) {
        String url = servidor.getTipoServidor().getJdbc() + servidor.getHost() + ":" + servidor.getPort();
        if (StringUtils.isNotEmpty(servidor.getDataBase())) {
            url += "/" + servidor.getDataBase();
        }
        return url;
    }

    public static void execute(Connection connection, String sql) throws ClassNotFoundException, SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static Map.Entry<List<String>, List<Object[]>> executeSelect(Connection connection,
                                                                        String sentencia) throws ClassNotFoundException, SQLException {
        Map.Entry<List<String>, List<Object[]>> entry;
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
        return entry;
    }
}
