package es.jklabs.security;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;

final class WindowsCredentialStore implements WindowsCredentialManagerProvider.CredentialStore {
    @Override
    public byte[] read(String target) {
        PointerByReference pCredential = new PointerByReference();
        boolean ok = WindowsCredentialManagerProvider.Advapi32.INSTANCE.CredRead(
                new WString(target), WindowsCredentialManagerProvider.CRED_TYPE_GENERIC, 0, pCredential);
        if (!ok) {
            return null;
        }
        Pointer credentialPtr = pCredential.getValue();
        WindowsCredentialManagerProvider.CREDENTIAL credential =
                new WindowsCredentialManagerProvider.CREDENTIAL(credentialPtr);
        credential.read();
        byte[] result = credential.readCredentialBlob();
        WindowsCredentialManagerProvider.Advapi32.INSTANCE.CredFree(credentialPtr);
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
        WindowsCredentialManagerProvider.Advapi32.INSTANCE.CredWrite(credential, 0);
    }
}
