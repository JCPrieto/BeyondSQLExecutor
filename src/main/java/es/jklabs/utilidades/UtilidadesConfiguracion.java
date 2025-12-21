package es.jklabs.utilidades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.json.configuracion.Configuracion;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.regions.Region;

import java.io.*;

public class UtilidadesConfiguracion {

    private static final String CONFIG_JSON = "config.json";

    private UtilidadesConfiguracion() {

    }

    public static Configuracion loadConfig() {
        Configuracion configuracion = null;
        try {
            UtilidadesFichero.createBaseFolder();
            configuracion = new Gson().fromJson(new FileReader(UtilidadesFichero.HOME +
                    UtilidadesFichero.SEPARADOR +
                    UtilidadesFichero.APP_FOLDER +
                    UtilidadesFichero.SEPARADOR +
                    CONFIG_JSON), Configuracion.class);
            if (configuracion != null) {
                upgradeAwsSDK(configuracion);
                if (migratePasswords(configuracion)) {
                    guardar(configuracion);
                }
            }
        } catch (FileNotFoundException e) {
            Logger.info("fichero.configuracion.no.encontrado", e);
        } catch (IOException e) {
            Logger.error(e);
        }
        return configuracion;
    }

    public static void guardar(Configuracion configuracion) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        try (FileWriter fw = new FileWriter(UtilidadesFichero.HOME +
                UtilidadesFichero.SEPARADOR +
                UtilidadesFichero.APP_FOLDER +
                UtilidadesFichero.SEPARADOR +
                CONFIG_JSON)) {
            fw.write(gson.toJson(configuracion));
        }
    }

    public static void guardarConfiguracion(Configuracion configuracion, File file) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(gson.toJson(configuracion));
        }
    }

    public static Configuracion loadConfig(File file) throws IOException {
        Configuracion configuracion;
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String linea;
            StringBuilder json = new StringBuilder(StringUtils.EMPTY);
            while ((linea = br.readLine()) != null) {
                json.append(linea);
            }
            configuracion = new Gson().fromJson(json.toString(), Configuracion.class);
            if (configuracion != null) {
                upgradeAwsSDK(configuracion);
                migratePasswords(configuracion);
            }
        }
        return configuracion;
    }

    private static void upgradeAwsSDK(Configuracion configuracion) {
        configuracion.getServers().stream()
                .filter(servidor -> servidor.getRegion() != null &&
                        servidor.getAwsRegion() == null)
                .forEach(servidor -> servidor.setAwsRegion(Region.of(StringUtils.lowerCase(servidor.getRegion()).replace("_", "-"))));
    }

    private static boolean migratePasswords(Configuracion configuracion) {
        boolean changed = false;
        for (var servidor : configuracion.getServers()) {
            String pass = servidor.getPass();
            if (StringUtils.isBlank(pass) || pass.startsWith("v2:")) {
                continue;
            }
            String plain = UtilidadesEncryptacion.decrypt(pass);
            if (plain == null) {
                continue;
            }
            try {
                servidor.setPass(UtilidadesEncryptacion.encrypt(plain));
                changed = true;
            } catch (Exception e) {
                Logger.error(e);
            }
        }
        return changed;
    }
}
