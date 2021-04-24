package es.jklabs.utilidades;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

public class UtilidadesFichero {

    static final String HOME = System.getProperty("user.home");
    static final String SEPARADOR = System.getProperty("file.separator");
    static String APP_FOLDER = ".BeyondSQLExecutor";

    private UtilidadesFichero() {

    }

    public static void createBaseFolder() {
        File base = new File(HOME + SEPARADOR + APP_FOLDER);
        if (!base.exists()) {
            try {
                Files.createDirectory(FileSystems.getDefault().getPath(HOME + SEPARADOR + APP_FOLDER));
            } catch (IOException e) {
                Logger.error("crear.carpeta.base", e);
            }
        }
    }
}
