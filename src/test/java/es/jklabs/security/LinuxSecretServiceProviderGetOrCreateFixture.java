package es.jklabs.security;

import java.util.Base64;

public final class LinuxSecretServiceProviderGetOrCreateFixture {

    private LinuxSecretServiceProviderGetOrCreateFixture() {
    }

    public static void main(String[] args) {
        boolean allowCreate = args.length > 0 && Boolean.parseBoolean(args[0]);
        LinuxSecretServiceProvider provider = new LinuxSecretServiceProvider();
        try {
            byte[] key = provider.getOrCreateMasterKey(new SecureMetadata(), null, allowCreate);
            if (key == null) {
                System.out.println("NULL");
            } else {
                System.out.println("OK:" + Base64.getEncoder().encodeToString(key));
            }
        } catch (Exception e) {
            System.out.println("ERROR:" + e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }
}
