package es.jklabs.json.configuracion;

import java.io.Serial;
import java.io.Serializable;

public class Servidor implements Serializable {
    @Serial
    private static final long serialVersionUID = 6073042728652109373L;
    private String name;
    private TipoServidor tipoServidor;
    private String host;
    private String port;
    private String dataBase;
    private String user;
    private String pass;

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
}
