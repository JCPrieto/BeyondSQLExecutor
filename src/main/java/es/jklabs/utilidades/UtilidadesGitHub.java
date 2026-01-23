package es.jklabs.utilidades;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.jklabs.gui.utilidades.Growls;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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

    public static void descargaNuevaVersion() {
        try {
            ReleaseInfo releaseInfo = fetchLatestRelease();
            if (releaseInfo == null || releaseInfo.htmlUrl == null) {
                Growls.mostrarError("abrir.nueva.version", new IOException("Release URL not available."));
                return;
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Growls.mostrarError("abrir.nueva.version", new IOException("Desktop browse not supported."));
                return;
            }
            Desktop.getDesktop().browse(URI.create(releaseInfo.htmlUrl));
            Growls.mostrarInfo("nueva.version.abierta");
        } catch (Exception e) {
            Growls.mostrarError("abrir.nueva.version", e);
            Logger.error("abrir.nueva.version", e);
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
            String htmlUrl = getString(json, "html_url");
            return new ReleaseInfo(version, htmlUrl);
        }
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

    private record ReleaseInfo(String version, String htmlUrl) {
    }
}
