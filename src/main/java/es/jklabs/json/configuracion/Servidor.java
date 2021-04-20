package es.jklabs.json.configuracion;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Servidor implements Serializable {
    @Serial
    private static final long serialVersionUID = 6073042728652109373L;
    private String id;
    private String name;
    private TipoServidor tipoServidor;
    private String host;
    private String port;
    private String dataBase;
    private String user;
    private String pass;

    public Servidor() {
        id = String.valueOf(UUID.randomUUID());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TipoServidor getTipoServidor() {
        return tipoServidor;
    }

    public void setTipoServidor(TipoServidor tipoServidor) {
        this.tipoServidor = tipoServidor;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDataBase() {
        return dataBase;
    }

    public void setDataBase(String dataBase) {
        this.dataBase = dataBase;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Servidor servidor = (Servidor) o;

        return Objects.equals(id, servidor.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
