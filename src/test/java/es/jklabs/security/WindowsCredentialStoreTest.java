package es.jklabs.security;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WindowsCredentialStoreTest {

    private static WindowsCredentialManagerProvider.CREDENTIAL credentialWithBlob(byte[] secret) {
        WindowsCredentialManagerProvider.CREDENTIAL credential = new WindowsCredentialManagerProvider.CREDENTIAL();
        credential.CredentialBlob = new Memory(secret.length);
        credential.CredentialBlob.write(0, secret, 0, secret.length);
        credential.CredentialBlobSize = secret.length;
        credential.write();
        return credential;
    }

    @Test
    void defaultConstructorDoesNotLoadNativeApiImmediately() {
        assertNotNull(new WindowsCredentialStore());
    }

    @Test
    void readReturnsNullWhenCredentialDoesNotExist() {
        FakeAdvapi32 advapi32 = new FakeAdvapi32(false, null);
        WindowsCredentialStore store = new WindowsCredentialStore(advapi32);

        byte[] result = store.read("target");

        assertNull(result);
        assertEquals("target", advapi32.readTarget);
        assertEquals(WindowsCredentialManagerProvider.CRED_TYPE_GENERIC, advapi32.readType);
        assertEquals(0, advapi32.readFlags);
        assertFalse(advapi32.credentialFreed);
    }

    @Test
    void readReturnsCredentialBlobAndFreesCredential() {
        byte[] secret = "stored-secret".getBytes(StandardCharsets.UTF_8);
        WindowsCredentialManagerProvider.CREDENTIAL credential = credentialWithBlob(secret);
        FakeAdvapi32 advapi32 = new FakeAdvapi32(true, credential);
        WindowsCredentialStore store = new WindowsCredentialStore(advapi32);

        byte[] result = store.read("target");

        assertArrayEquals(secret, result);
        assertEquals("target", advapi32.readTarget);
        assertEquals(Pointer.nativeValue(credential.getPointer()), Pointer.nativeValue(advapi32.freedCredential));
        assertTrue(advapi32.credentialFreed);
    }

    @Test
    void writeStoresGenericLocalMachineCredential() {
        FakeAdvapi32 advapi32 = new FakeAdvapi32(false, null);
        WindowsCredentialStore store = new WindowsCredentialStore(advapi32);
        byte[] secret = "new-secret".getBytes(StandardCharsets.UTF_8);

        store.write("target", "user", secret);

        assertEquals(0, advapi32.writeFlags);
        assertEquals(WindowsCredentialManagerProvider.CRED_TYPE_GENERIC, advapi32.writeType);
        assertEquals("target", advapi32.writeTarget);
        assertEquals("user", advapi32.writeUserName);
        assertEquals(WindowsCredentialManagerProvider.CRED_PERSIST_LOCAL_MACHINE, advapi32.writePersist);
        assertArrayEquals(secret, advapi32.writeSecret);
    }

    private static final class FakeAdvapi32 implements WindowsCredentialManagerProvider.Advapi32 {
        private final boolean readResult;
        private final WindowsCredentialManagerProvider.CREDENTIAL credential;
        private String readTarget;
        private int readType;
        private int readFlags;
        private Pointer freedCredential;
        private boolean credentialFreed;
        private int writeFlags;
        private int writeType;
        private String writeTarget;
        private String writeUserName;
        private int writePersist;
        private byte[] writeSecret;

        private FakeAdvapi32(boolean readResult, WindowsCredentialManagerProvider.CREDENTIAL credential) {
            this.readResult = readResult;
            this.credential = credential;
        }

        @Override
        public boolean CredRead(WString targetName, int type, int flags, PointerByReference pCredential) {
            readTarget = targetName.toString();
            readType = type;
            readFlags = flags;
            if (credential != null) {
                pCredential.setValue(credential.getPointer());
            }
            return readResult;
        }

        @Override
        public void CredWrite(WindowsCredentialManagerProvider.CREDENTIAL credential, int flags) {
            writeFlags = flags;
            writeType = credential.Type;
            writeTarget = credential.TargetName.toString();
            writeUserName = credential.UserName.toString();
            writePersist = credential.Persist;
            writeSecret = credential.CredentialBlob.getByteArray(0, credential.CredentialBlobSize);
        }

        @Override
        public void CredFree(Pointer credential) {
            freedCredential = credential;
            credentialFreed = true;
        }
    }
}
