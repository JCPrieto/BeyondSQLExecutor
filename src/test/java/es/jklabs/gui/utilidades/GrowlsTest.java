package es.jklabs.gui.utilidades;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowlsTest {

    @Test
    void isNotifySendAvailableUsesCachedValue() throws Exception {
        Field cacheField = Growls.class.getDeclaredField("notifySendAvailable");
        cacheField.setAccessible(true);
        Method method = Growls.class.getDeclaredMethod("isNotifySendAvailable");
        method.setAccessible(true);

        Object previous = cacheField.get(null);
        try {
            cacheField.set(null, Boolean.TRUE);
            assertTrue((Boolean) method.invoke(null));

            cacheField.set(null, Boolean.FALSE);
            assertFalse((Boolean) method.invoke(null));
        } finally {
            cacheField.set(null, previous);
        }
    }
}
