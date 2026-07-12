package es.jklabs.json.configuracion;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ServidorTest {
    @Test
    void identityRemainsStableWhenEditableFieldsChange() {
        Servidor server = new Servidor();
        Map<Servidor, String> values = new HashMap<>();
        values.put(server, "result");

        server.setName("Renamed");
        server.setHost("new-host");
        server.setPort("5432");
        server.setUser("new-user");

        assertEquals("result", values.get(server));
    }

    @Test
    void clonedServerHasIndependentIdentity() {
        Servidor original = new Servidor();
        Servidor clone = new Servidor(original);

        assertNotEquals(original.getId(), clone.getId());
        assertNotEquals(original, clone);
    }
}
