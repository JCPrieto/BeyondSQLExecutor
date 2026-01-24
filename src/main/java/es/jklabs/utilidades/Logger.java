package es.jklabs.utilidades;

import org.apache.commons.lang3.Strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Logger {

    private static final String ARCHIVO = "log_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".log";
    private static final int LOG_ROTATION_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int LOG_ROTATION_COUNT = 3;
    private static final String LOG_PATTERN = "log_%g_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".log";
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Logger.class.getName());
    private static Logger logger;

    private Logger() {
        FileHandler fh;
        try {
            UtilidadesFichero.createBaseFolder();
            Path logDir = Path.of(UtilidadesFichero.HOME, UtilidadesFichero.APP_FOLDER);
            fh = new FileHandler(logDir.resolve(LOG_PATTERN).toString(), LOG_ROTATION_SIZE_BYTES, LOG_ROTATION_COUNT, true);
            LOG.addHandler(fh);
            LOG.setUseParentHandlers(false);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            fh.setLevel(Level.ALL);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Crear archivo logs", e);
        }
    }

    public static void eliminarLogsVacios() {
        File carpeta = new File(UtilidadesFichero.HOME + UtilidadesFichero.SEPARADOR + UtilidadesFichero.APP_FOLDER);
        File[] lista = carpeta.listFiles();
        if (lista != null) {
            Arrays.stream(lista).filter(f -> f.isFile() && f.getName().endsWith(".log") && !Strings.CS.equals
                    (f.getName(), ARCHIVO)).forEach(Logger::eliminarLogsVacios);
        }
    }

    private static void eliminarLogsVacios(File file) {
        try (FileReader fr = new FileReader(file)) {
            BufferedReader br = new BufferedReader(fr);
            String linea = br.readLine();
            if (linea == null) {
                br.close();
                Files.delete(file.toPath());
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, Mensajes.getError("lectura.logs"), e);
        }
    }

    public static void init() {
        if (logger == null) {
            logger = new Logger();
        }
    }

    public static void error(String key, Exception e) {
        LOG.log(Level.SEVERE, Mensajes.getError(key), e);
    }

    public static void error(Exception e) {
        LOG.log(Level.SEVERE, null, e);
    }

    public static void info(String key, String[] strings, Exception e) {
        LOG.log(Level.INFO, Mensajes.getError(key, strings), e);
    }
}
