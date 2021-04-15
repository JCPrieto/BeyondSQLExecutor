package es.jklabs.json.firebase;

public class Aplicacion {
    private int numDescargas;
    private String ultimaVersion;

    public int getNumDescargas() {
        return numDescargas;
    }

    public void setNumDescargas(int numDescargas) {
        this.numDescargas = numDescargas;
    }

    public String getUltimaVersion() {
        return ultimaVersion;
    }

    public void setUltimaVersion(String ultimaVersion) {
        this.ultimaVersion = ultimaVersion;
    }
}
