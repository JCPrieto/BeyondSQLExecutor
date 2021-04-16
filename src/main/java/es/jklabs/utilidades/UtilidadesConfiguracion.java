package es.jklabs.utilidades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.jklabs.json.configuracion.Configuracion;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
        } catch (FileNotFoundException e) {
            Logger.info("fichero.configuracion.no.encontrado", e);
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
                CONFIG_JSON);) {
            fw.write(gson.toJson(configuracion));
        }
    }
}
