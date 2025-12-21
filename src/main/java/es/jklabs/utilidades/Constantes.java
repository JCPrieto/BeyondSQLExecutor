package es.jklabs.utilidades;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Constantes {
    public static final String NOMBRE_APP = "BeyondSQLExecutor";
    public static final String VERSION = loadVersion();
    public static final String COMPILACION = "JDK21";

    private Constantes() {

    }

    private static String loadVersion() {
        Properties properties = new Properties();
        try (InputStream inputStream = Constantes.class.getClassLoader().getResourceAsStream("build.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
                String version = properties.getProperty("version");
                if (version != null && !version.isBlank()) {
                    return version;
                }
            }
        } catch (IOException e) {
            // Fallback to a safe placeholder when build info is unavailable.
        }
        return "dev";
    }
}
