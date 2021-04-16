package es.jklabs.json.configuracion;

import java.util.ArrayList;
import java.util.List;

public class Configuracion {

    private List<Servidor> servers;

    public Configuracion() {
        servers = new ArrayList<>();
    }

    public List<Servidor> getServers() {
        return servers;
    }

    public void setServers(List<Servidor> servers) {
        this.servers = servers;
    }
}
