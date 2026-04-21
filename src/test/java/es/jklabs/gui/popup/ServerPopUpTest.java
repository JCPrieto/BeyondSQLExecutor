package es.jklabs.gui.popup;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerPopUpTest {

    @Test
    void deleteConfirmationUsesServerNameInsteadOfSwingComponentName() {
        Servidor servidor = new Servidor();
        servidor.setName("Conexion Oracle");
        servidor.setTipoServidor(TipoServidor.MYSQL);
        ServerItem serverItem = new ServerItem(null, servidor);

        ServerPopUp popUp = new ServerPopUp(null, serverItem);

        assertEquals("¿Está seguro de desear eliminar Conexion Oracle?", popUp.getDeleteConfirmationMessage());
    }
}
