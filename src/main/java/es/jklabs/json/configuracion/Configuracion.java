package es.jklabs.json.configuracion;

import es.jklabs.gui.themes.model.EditorTheme;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Configuracion implements Serializable {

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

    public EditorTheme getTheme() {
        return theme;
    }

    public void setTheme(EditorTheme theme) {
        this.theme = theme;
    }
}
