package es.jklabs;

import es.jklabs.gui.MainUI;
import es.jklabs.gui.utilidades.Growls;
import es.jklabs.json.configuracion.Configuracion;
import es.jklabs.utilidades.Logger;
import es.jklabs.utilidades.UtilidadesConfiguracion;

import javax.swing.*;
import java.awt.*;

public class BeyondSQLExecutor {

    public static void main(String[] args) {
        Logger.eliminarLogsVacios();
        Logger.init();
        SwingUtilities.invokeLater(() -> {
            try {
                Growls.init();
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Configuracion configuracion = UtilidadesConfiguracion.loadConfig();
                MainUI mainUI = new MainUI(configuracion);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> mainUI.getServerPanel().closeAllConnections()));
                mainUI.setVisible(true);
                mainUI.setExtendedState(mainUI.getExtendedState() | Frame.MAXIMIZED_BOTH);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     UnsupportedLookAndFeelException e) {
                Logger.error("cargar.apariencia", e);
            }
        });
    }
}
