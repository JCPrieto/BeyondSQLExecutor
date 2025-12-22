package es.jklabs.utilidades;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;

public class UtilidadesGitHub {

    private static final String REPO_OWNER = "JCPrieto";
    private static final String REPO_NAME = "BeyondSQLExecutor";
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;

    private UtilidadesGitHub() {

    }

    public static boolean existeNuevaVersion() throws IOException {
        ReleaseInfo releaseInfo = fetchLatestRelease();
        if (releaseInfo == null || releaseInfo.version == null) {
            return false;
        }
        return diferenteVersion(releaseInfo.version);
    }

    private static boolean diferenteVersion(String serverVersion) {
        String[] sv = serverVersion.split("\\.");
        String[] av = Constantes.VERSION.split("\\.");
        return Integer.parseInt(sv[0]) > Integer.parseInt(av[0]) || Integer.parseInt(sv[0]) == Integer.parseInt(av[0]) && (Integer.parseInt(sv[1]) > Integer.parseInt(av[1]) || Integer.parseInt(sv[1]) == Integer.parseInt(av[1]) && Integer.parseInt(sv[2]) > Integer.parseInt(av[2]));
    }

    public static void descargaNuevaVersion(MainUI mainUI) throws InterruptedException {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int retorno = fc.showSaveDialog(mainUI);
        if (retorno == JFileChooser.APPROVE_OPTION) {
            File directorio = fc.getSelectedFile();
            try {
                ReleaseInfo releaseInfo = fetchLatestRelease();
                if (releaseInfo != null && releaseInfo.downloadUrl != null) {
                    FileUtils.copyURLToFile(
                            URI.create(releaseInfo.downloadUrl).toURL(),
                            new File(directorio.getPath() + FileSystems.getDefault().getSeparator() + releaseInfo.assetName));
                    Growls.mostrarInfo("nueva.version.descargada");
                } else {
                    Logger.info("descargar.nueva.version");
                }
            } catch (AccessDeniedException e) {
                Growls.mostrarError("path.sin.permiso.escritura", e);
                descargaNuevaVersion(mainUI);
            } catch (IOException e) {
                Logger.error("descargar.nueva.version", e);
            }
        }
    }

    private static ReleaseInfo fetchLatestRelease() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", REPO_NAME);
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }
        try (InputStream inputStream = connection.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String tag = getString(json, "tag_name");
            String version = normalizeVersion(tag);
            String assetName = getAssetName(version);
            String downloadUrl = getAssetUrl(json.getAsJsonArray("assets"), assetName);
            return new ReleaseInfo(version, assetName, downloadUrl);
        }
    }

    private static String getAssetUrl(JsonArray assets, String assetName) {
        if (assets == null) {
            return null;
        }
        for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            String name = getString(asset, "name");
            if (assetName.equals(name)) {
                return getString(asset, "browser_download_url");
            }
        }
        return null;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }

    private static String normalizeVersion(String tagName) {
        if (tagName == null) {
            return null;
        }
        if (tagName.startsWith("v")) {
            return tagName.substring(1);
        }
        return tagName;
    }

    private static String getAssetName(String version) {
        if (version == null) {
            return null;
        }
        return Constantes.NOMBRE_APP + "-" + version + "_" + Constantes.COMPILACION + ".zip";
    }

    private static class ReleaseInfo {
        private final String version;
        private final String assetName;
        private final String downloadUrl;

        private ReleaseInfo(String version, String assetName, String downloadUrl) {
            this.version = version;
            this.assetName = assetName;
            this.downloadUrl = downloadUrl;
        }
    }
}
