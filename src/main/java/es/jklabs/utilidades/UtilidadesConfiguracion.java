package es.jklabs.utilidades;

import com.google.gson.Gson;
import es.jklabs.json.configuracion.Configuracion;

import java.io.FileNotFoundException;
import java.io.FileReader;

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

}
