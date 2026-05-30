package es.jklabs.utilidades;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UtilidadesGitHubTest {

    @Test
    void existeNuevaVersionReturnsFalseWithoutReleaseOrVersion() {
        assertFalse(UtilidadesGitHub.existeNuevaVersion(null, "1.1.16"));
        assertFalse(UtilidadesGitHub.existeNuevaVersion(new UtilidadesGitHub.ReleaseInfo(null, "https://example.test"), "1.1.16"));
    }

    @Test
    void existeNuevaVersionComparesReleaseWithApplicationVersion() {
        assertTrue(UtilidadesGitHub.existeNuevaVersion(new UtilidadesGitHub.ReleaseInfo("1.1.17", "https://example.test"), "1.1.16"));
        assertFalse(UtilidadesGitHub.existeNuevaVersion(new UtilidadesGitHub.ReleaseInfo("1.1.16", "https://example.test"), "1.1.16"));
    }

    @Test
    void diferenteVersionDetectsOnlyServerVersionsGreaterThanApplicationVersion() {
        assertTrue(UtilidadesGitHub.diferenteVersion("2.0.0", "1.9.9"));
        assertTrue(UtilidadesGitHub.diferenteVersion("1.2.0", "1.1.9"));
        assertTrue(UtilidadesGitHub.diferenteVersion("1.1.17", "1.1.16"));
        assertFalse(UtilidadesGitHub.diferenteVersion("1.1.16", "1.1.16"));
        assertFalse(UtilidadesGitHub.diferenteVersion("1.1.15", "1.1.16"));
        assertFalse(UtilidadesGitHub.diferenteVersion("1.0.9", "1.1.0"));
        assertFalse(UtilidadesGitHub.diferenteVersion("0.9.9", "1.0.0"));
    }

    @Test
    void parseSemverTreatsMissingOrInvalidPartsAsZero() {
        assertArrayEquals(new int[]{0, 0, 0}, UtilidadesGitHub.parseSemver(null));
        assertArrayEquals(new int[]{0, 0, 0}, UtilidadesGitHub.parseSemver("   "));
        assertArrayEquals(new int[]{1, 2, 0}, UtilidadesGitHub.parseSemver("1.2"));
        assertArrayEquals(new int[]{1, 2, 3}, UtilidadesGitHub.parseSemver("1.2.3.4"));
        assertArrayEquals(new int[]{1, 2, 3}, UtilidadesGitHub.parseSemver("1.2.3-beta"));
        assertArrayEquals(new int[]{1, 0, 3}, UtilidadesGitHub.parseSemver("1.beta.3"));
    }

    @Test
    void parseLeadingIntReturnsZeroWhenValueDoesNotStartWithAValidInteger() {
        assertEquals(0, UtilidadesGitHub.parseLeadingInt(null));
        assertEquals(0, UtilidadesGitHub.parseLeadingInt("beta"));
        assertEquals(123, UtilidadesGitHub.parseLeadingInt("123-beta"));
        assertEquals(0, UtilidadesGitHub.parseLeadingInt("999999999999999999999999"));
    }

    @Test
    void getStringReturnsNullForMissingNullOrJsonNullValues() {
        JsonObject json = new JsonObject();
        json.addProperty("name", "release");
        json.add("empty", null);

        assertNull(UtilidadesGitHub.getString(null, "name"));
        assertNull(UtilidadesGitHub.getString(json, "missing"));
        assertNull(UtilidadesGitHub.getString(json, "empty"));
        assertEquals("release", UtilidadesGitHub.getString(json, "name"));
    }

    @Test
    void normalizeVersionRemovesOnlyLowercaseVPrefix() {
        assertNull(UtilidadesGitHub.normalizeVersion(null));
        assertEquals("1.2.3", UtilidadesGitHub.normalizeVersion("v1.2.3"));
        assertEquals("V1.2.3", UtilidadesGitHub.normalizeVersion("V1.2.3"));
        assertEquals("1.2.3", UtilidadesGitHub.normalizeVersion("1.2.3"));
    }

    @Test
    void descargaNuevaVersionShowsErrorWhenReleaseOrUrlIsUnavailable() {
        TrackingNotifier notifier = new TrackingNotifier();

        UtilidadesGitHub.descargaNuevaVersion(() -> null, new TrackingBrowser(true), notifier);
        assertEquals("abrir.nueva.version", notifier.errorKey);
        assertInstanceOf(IOException.class, notifier.error);

        notifier = new TrackingNotifier();
        UtilidadesGitHub.descargaNuevaVersion(
                () -> new UtilidadesGitHub.ReleaseInfo("1.2.3", null),
                new TrackingBrowser(true),
                notifier
        );
        assertEquals("abrir.nueva.version", notifier.errorKey);
        assertInstanceOf(IOException.class, notifier.error);
    }

    @Test
    void descargaNuevaVersionShowsErrorWhenBrowseIsNotSupported() {
        TrackingNotifier notifier = new TrackingNotifier();
        TrackingBrowser browser = new TrackingBrowser(false);

        UtilidadesGitHub.descargaNuevaVersion(
                () -> new UtilidadesGitHub.ReleaseInfo("1.2.3", "https://example.test/release"),
                browser,
                notifier
        );

        assertEquals("abrir.nueva.version", notifier.errorKey);
        assertInstanceOf(IOException.class, notifier.error);
        assertNull(browser.browsedUrl);
    }

    @Test
    void descargaNuevaVersionBrowsesReleaseUrlAndShowsInfo() {
        TrackingNotifier notifier = new TrackingNotifier();
        TrackingBrowser browser = new TrackingBrowser(true);

        UtilidadesGitHub.descargaNuevaVersion(
                () -> new UtilidadesGitHub.ReleaseInfo("1.2.3", "https://example.test/release"),
                browser,
                notifier
        );

        assertEquals("https://example.test/release", browser.browsedUrl);
        assertEquals("nueva.version.abierta", notifier.infoKey);
        assertNull(notifier.errorKey);
    }

    @Test
    void descargaNuevaVersionReportsUnexpectedErrors() {
        TrackingNotifier notifier = new TrackingNotifier();
        TrackingBrowser browser = new TrackingBrowser(true);
        IOException failure = new IOException("Cannot open browser");
        browser.failure = failure;

        UtilidadesGitHub.descargaNuevaVersion(
                () -> new UtilidadesGitHub.ReleaseInfo("1.2.3", "https://example.test/release"),
                browser,
                notifier
        );

        assertEquals("abrir.nueva.version", notifier.errorKey);
        assertEquals(failure, notifier.error);
    }

    @Test
    void fetchLatestReleaseReturnsNullWhenGitHubDoesNotReturnOk() throws Exception {
        FakeConnection connection = new FakeConnection(HttpURLConnection.HTTP_NOT_FOUND, "{}");

        assertNull(UtilidadesGitHub.fetchLatestRelease(connection));
        assertEquals("GET", connection.requestMethod);
        assertEquals("BeyondSQLExecutor", connection.userAgent);
    }

    @Test
    void fetchLatestReleaseParsesVersionAndHtmlUrlFromGitHubResponse() throws Exception {
        FakeConnection connection = new FakeConnection(
                HttpURLConnection.HTTP_OK,
                "{\"tag_name\":\"v1.2.3\",\"html_url\":\"https://example.test/release\"}"
        );

        UtilidadesGitHub.ReleaseInfo releaseInfo = UtilidadesGitHub.fetchLatestRelease(connection);

        if (releaseInfo != null) {
            assertEquals("1.2.3", releaseInfo.version());
            assertEquals("https://example.test/release", releaseInfo.htmlUrl());
        }
        assertEquals("GET", connection.requestMethod);
        assertEquals(8000, connection.connectTimeoutMs);
        assertEquals(15000, connection.readTimeoutMs);
        assertEquals("BeyondSQLExecutor", connection.userAgent);
    }

    private static class TrackingBrowser implements UtilidadesGitHub.Browser {
        private final boolean supported;
        private String browsedUrl;
        private Exception failure;

        private TrackingBrowser(boolean supported) {
            this.supported = supported;
        }

        @Override
        public boolean isBrowseSupported() {
            return supported;
        }

        @Override
        public void browse(String url) throws Exception {
            if (failure != null) {
                throw failure;
            }
            browsedUrl = url;
        }
    }

    private static class TrackingNotifier implements UtilidadesGitHub.Notifier {
        private String errorKey;
        private Exception error;
        private String infoKey;

        @Override
        public void error(String key, Exception e) {
            errorKey = key;
            error = e;
        }

        @Override
        public void info(String key) {
            infoKey = key;
        }
    }

    private static class FakeConnection extends HttpURLConnection {
        private final int responseCode;
        private final String body;
        private String requestMethod;
        private String userAgent;
        private int connectTimeoutMs;
        private int readTimeoutMs;

        private FakeConnection(int responseCode, String body) throws IOException {
            super(URI.create("https://example.test/latest").toURL());
            this.responseCode = responseCode;
            this.body = body;
        }

        @Override
        public void setRequestMethod(String method) {
            requestMethod = method;
        }

        @Override
        public void setConnectTimeout(int timeout) {
            connectTimeoutMs = timeout;
        }

        @Override
        public void setReadTimeout(int timeout) {
            readTimeoutMs = timeout;
        }

        @Override
        public void setRequestProperty(String key, String value) {
            if ("User-Agent".equals(key)) {
                userAgent = value;
            }
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void disconnect() {

        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {

        }
    }
}
