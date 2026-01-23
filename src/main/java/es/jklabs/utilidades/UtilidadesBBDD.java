package es.jklabs.utilidades;

import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoLogin;
import es.jklabs.security.SecureStorageException;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

import java.sql.*;
import java.util.*;

public class UtilidadesBBDD {

    private UtilidadesBBDD() {

    }

    public static Connection getConexion(Servidor servidor) throws SQLException, ClassNotFoundException {
        Class.forName(servidor.getTipoServidor().getDriver());
        Properties connectionsProperties = new Properties();
        if (servidor.getTipoLogin().equals(TipoLogin.AWS_PROFILE)) {
            connectionsProperties.setProperty("verifyServerCertificate", "true");
            connectionsProperties.setProperty("useSSL", "true");
        }
        connectionsProperties.setProperty("user", servidor.getUser());
        connectionsProperties.setProperty("password", getPass(servidor));
        return DriverManager.getConnection(getURL(servidor), connectionsProperties);
    }

    private static String getPass(Servidor servidor) throws SQLException {
        if (Objects.equals(servidor.getTipoLogin(), TipoLogin.AWS_PROFILE)) {
            return getRdsIamToken(servidor);
        }
        if (servidor.getCredentialRef() == null) {
            return null;
        }
        try {
            return UtilidadesConfiguracion.getSecureStorageManager()
                    .getPassword(servidor.getCredentialRef(), null);
        } catch (SecureStorageException e) {
            throw new SQLException("Error al obtener la credencial.", e);
        }
    }

    private static String getRdsIamToken(Servidor servidor) {
        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                .profileName(servidor.getAwsProfile())
                .build();
        try (RdsClient rdsClient = RdsClient.builder()
                .credentialsProvider(profileCredentialsProvider)
                .region(servidor.getAwsRegion())
                .build()) {
            RdsUtilities utils = rdsClient.utilities();
            GenerateAuthenticationTokenRequest request = GenerateAuthenticationTokenRequest.builder()
                    .credentialsProvider(profileCredentialsProvider)
                    .hostname(servidor.getHost())
                    .port(Integer.parseInt(servidor.getPort()))
                    .username(servidor.getUser())
                    .build();
            return utils.generateAuthenticationToken(request);
        }
    }

    public static String getURL(Servidor servidor) {
        String url = servidor.getTipoServidor().getJdbc() + servidor.getHost() + ":" + servidor.getPort();
        if (StringUtils.isNotEmpty(servidor.getDataBase())) {
            url += "/" + servidor.getDataBase();
        }
        return url;
    }

    public static void execute(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static Map.Entry<List<String>, List<Object[]>> executeAny(Connection connection,
                                                                     String sentencia) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sentencia);
            if (!hasResultSet) {
                return null;
            }
            try (ResultSet rs = statement.getResultSet()) {
                return readResultSet(rs);
            }
        }
    }

    private static Map.Entry<List<String>, List<Object[]>> readResultSet(ResultSet rs) throws SQLException {
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
        return new AbstractMap.SimpleEntry<>(cabecera, valores);
    }

    public static List<String> getEsquemas(Connection connection) throws SQLException {
        List<String> esquemas = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("select schema_name " +
                "from information_schema.schemata " +
                "order by schema_name;")) {
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                esquemas.add(rs.getString(1));
            }
        }
        return esquemas;
    }
}
