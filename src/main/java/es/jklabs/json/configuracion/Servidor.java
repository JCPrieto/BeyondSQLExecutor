package es.jklabs.json.configuracion;

import com.amazonaws.regions.Regions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Servidor implements Serializable {
    private static final long serialVersionUID = 6073042728652109373L;
    private String id;
    private String name;
    private TipoServidor tipoServidor;
    private String host;
    private String port;
    private String dataBase;
    private TipoLogin tipoLogin;
    private String user;
    private String pass;
    private Regions region;
    private String awsProfile;
    private Boolean executaAsRol;
    private String rol;
    private List<String> esquemasExcluidos;

    public Servidor() {
        id = String.valueOf(UUID.randomUUID());
        esquemasExcluidos = new ArrayList<>();
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

    public Boolean getExecutaAsRol() {
        return executaAsRol;
    }

    public void setExecutaAsRol(Boolean executaAsRol) {
        this.executaAsRol = executaAsRol;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public List<String> getEsquemasExcluidos() {
        return esquemasExcluidos;
    }

    public void setEsquemasExcluidos(List<String> esquemasExcluidos) {
        this.esquemasExcluidos = esquemasExcluidos;
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

    public TipoLogin getTipoLogin() {
        return tipoLogin;
    }

    public void setTipoLogin(TipoLogin tipoLogin) {
        this.tipoLogin = tipoLogin;
    }

    public String getAwsProfile() {
        return awsProfile;
    }

    public void setAwsProfile(String awsProfile) {
        this.awsProfile = awsProfile;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }
}
