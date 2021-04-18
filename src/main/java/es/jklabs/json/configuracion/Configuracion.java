package es.jklabs.json.configuracion;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Configuracion implements Serializable {

    @Serial
    private static final long serialVersionUID = -5939519262958332334L;
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
