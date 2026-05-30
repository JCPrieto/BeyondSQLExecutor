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
        return existeNuevaVersion(fetchLatestRelease(), Constantes.VERSION);
    }

    static boolean existeNuevaVersion(ReleaseInfo releaseInfo, String appVersion) {
        if (releaseInfo == null || releaseInfo.version == null) {
            return false;
        }
        return diferenteVersion(releaseInfo.version, appVersion);
    }

    static boolean diferenteVersion(String serverVersion, String appVersion) {
        int[] server = parseSemver(serverVersion);
        int[] app = parseSemver(appVersion);
        for (int i = 0; i < 3; i++) {
            if (server[i] != app[i]) {
                return server[i] > app[i];
            }
        }
        return false;
    }

    static int[] parseSemver(String version) {
        int[] parts = new int[]{0, 0, 0};
        if (version == null || version.isBlank()) {
            return parts;
        }
        String[] tokens = version.split("\\.");
        for (int i = 0; i < parts.length && i < tokens.length; i++) {
            parts[i] = parseLeadingInt(tokens[i]);
        }
        return parts;
    }

    static int parseLeadingInt(String value) {
        if (value == null) {
            return 0;
        }
        int i = 0;
        while (i < value.length() && Character.isDigit(value.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(0, i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void descargaNuevaVersion() {
        descargaNuevaVersion(
                UtilidadesGitHub::fetchLatestRelease,
                new DesktopBrowser(),
                new GrowlNotifier()
        );
    }

    static void descargaNuevaVersion(ReleaseFetcher releaseFetcher, Browser browser, Notifier notifier) {
        try {
            ReleaseInfo releaseInfo = releaseFetcher.fetch();
            if (releaseInfo == null || releaseInfo.htmlUrl == null) {
                notifier.error("abrir.nueva.version", new IOException("Release URL not available."));
                return;
            }
            if (!browser.isBrowseSupported()) {
                notifier.error("abrir.nueva.version", new IOException("Desktop browse not supported."));
                return;
            }
            browser.browse(releaseInfo.htmlUrl);
            notifier.info("nueva.version.abierta");
        } catch (Exception e) {
            notifier.error("abrir.nueva.version", e);
            Logger.error("abrir.nueva.version", e);
        }
    }

    private static ReleaseInfo fetchLatestRelease() throws IOException {
        URI uri = URI.create(LATEST_RELEASE_URL);
        return fetchLatestRelease(openConnection(uri));
    }

    static HttpURLConnection openConnection(URI uri) throws IOException {
        return (HttpURLConnection) uri.toURL().openConnection();
    }

    static ReleaseInfo fetchLatestRelease(HttpURLConnection connection) throws IOException {
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

    static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }

    static String normalizeVersion(String tagName) {
        if (tagName == null) {
            return null;
        }
        if (tagName.startsWith("v")) {
            return tagName.substring(1);
        }
        return tagName;
    }

    interface ReleaseFetcher {
        ReleaseInfo fetch() throws IOException;
    }

    interface Browser {
        boolean isBrowseSupported();

        void browse(String url) throws Exception;
    }

    interface Notifier {
        void error(String key, Exception e);

        void info(String key);
    }

    record ReleaseInfo(String version, String htmlUrl) {
    }

    private static class DesktopBrowser implements Browser {
        @Override
        public boolean isBrowseSupported() {
            return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        }

        @Override
        public void browse(String url) throws IOException {
            Desktop.getDesktop().browse(URI.create(url));
        }
    }

    private static class GrowlNotifier implements Notifier {
        @Override
        public void error(String key, Exception e) {
            Growls.mostrarError(key, e);
        }

        @Override
        public void info(String key) {
            Growls.mostrarInfo(key);
        }
    }
}
