package es.jklabs.json.configuracion;

import es.jklabs.gui.themes.model.EditorTheme;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Configuracion implements Serializable {

    @Serial
    private static final long serialVersionUID = -5939519262958332334L;
    private List<Servidor> servers;
    private EditorTheme theme;

    public Configuracion() {
        servers = new ArrayList<>();
    }

    public List<Servidor> getServers() {
        return servers;
    }

    public void setServers(List<Servidor> servers) {
        this.servers = servers;
    }

    public boolean ensureServerIds() {
        if (servers == null) {
            servers = new ArrayList<>();
            return true;
        }
        boolean changed = false;
        Set<UUID> ids = new HashSet<>();
        for (Servidor server : servers) {
            if (server != null && (server.getId() == null || !ids.add(server.getId()))) {
                do {
                    server.assignNewId();
                } while (!ids.add(server.getId()));
                changed = true;
            }
        }
        return changed;
    }

    public EditorTheme getTheme() {
        return theme;
    }

    public void setTheme(EditorTheme theme) {
        this.theme = theme;
    }
}
