package es.jklabs.security;

public class UiKdfParams {
    private String saltB64;
    private int iterations;
    private String algorithm;

    public String getSaltB64() {
        return saltB64;
    }

    public void setSaltB64(String saltB64) {
        this.saltB64 = saltB64;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
