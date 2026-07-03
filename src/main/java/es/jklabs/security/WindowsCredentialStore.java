package es.jklabs.security;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;

import java.util.function.Supplier;

final class WindowsCredentialStore implements WindowsCredentialManagerProvider.CredentialStore {
    private final Supplier<WindowsCredentialManagerProvider.Advapi32> advapi32Supplier;

    WindowsCredentialStore() {
        this(() -> WindowsCredentialManagerProvider.Advapi32.INSTANCE);
    }

    WindowsCredentialStore(WindowsCredentialManagerProvider.Advapi32 advapi32) {
        this(() -> advapi32);
    }

    private WindowsCredentialStore(Supplier<WindowsCredentialManagerProvider.Advapi32> advapi32Supplier) {
        this.advapi32Supplier = advapi32Supplier;
    }

    @Override
    public byte[] read(String target) {
        PointerByReference pCredential = new PointerByReference();
        WindowsCredentialManagerProvider.Advapi32 credentialApi = credentialApi();
        boolean ok = credentialApi.CredRead(
                new WString(target), WindowsCredentialManagerProvider.CRED_TYPE_GENERIC, 0, pCredential);
        if (!ok) {
            return null;
        }
        Pointer credentialPtr = pCredential.getValue();
        WindowsCredentialManagerProvider.CREDENTIAL credential =
                new WindowsCredentialManagerProvider.CREDENTIAL(credentialPtr);
        credential.read();
        byte[] result = credential.readCredentialBlob();
        credentialApi.CredFree(credentialPtr);
        return result;
    }

    @Override
    public void write(String target, String userName, byte[] secretBytes) {
        WindowsCredentialManagerProvider.CREDENTIAL credential = new WindowsCredentialManagerProvider.CREDENTIAL();
        credential.Type = WindowsCredentialManagerProvider.CRED_TYPE_GENERIC;
        credential.TargetName = new WString(target);
        credential.UserName = new WString(userName);
        credential.Persist = WindowsCredentialManagerProvider.CRED_PERSIST_LOCAL_MACHINE;
        credential.CredentialBlobSize = secretBytes.length;
        credential.CredentialBlob = new Memory(secretBytes.length);
        credential.CredentialBlob.write(0, secretBytes, 0, secretBytes.length);
        credential.write();
        credentialApi().CredWrite(credential, 0);
    }

    private WindowsCredentialManagerProvider.Advapi32 credentialApi() {
        return advapi32Supplier.get();
    }
}
