package es.jklabs.gui.popup;

import es.jklabs.gui.panels.ServerItem;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import org.junit.jupiter.api.Test;

import javax.swing.*;

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

    @Test
    void menuShowsCloneBetweenEditAndDelete() {
        Servidor servidor = new Servidor();
        servidor.setName("Conexion Oracle");
        servidor.setTipoServidor(TipoServidor.MYSQL);

        ServerPopUp popUp = new ServerPopUp(null, new ServerItem(null, servidor));

        assertEquals(3, popUp.getComponentCount());
        assertEquals("Editar", ((JMenuItem) popUp.getComponent(0)).getText());
        assertEquals("Clonar", ((JMenuItem) popUp.getComponent(1)).getText());
        assertEquals("Eliminar", ((JMenuItem) popUp.getComponent(2)).getText());
    }
}
