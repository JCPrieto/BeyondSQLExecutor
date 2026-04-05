package es.jklabs.security;

public final class LinuxSecretServiceProviderAvailabilityFixture {

    private LinuxSecretServiceProviderAvailabilityFixture() {
    }

    public static void main(String[] args) {
        boolean available = new LinuxSecretServiceProvider().isAvailable();
        System.out.print(Boolean.toString(available));
    }
}
