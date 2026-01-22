package es.jklabs.json.configuracion;

import software.amazon.awssdk.regions.Region;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Servidor implements Serializable {
    @Serial
    private static final long serialVersionUID = 6073042728652109373L;
    private String name;
    private TipoServidor tipoServidor;
    private String host;
    private String port;
    private String dataBase;
    private TipoLogin tipoLogin;
    private String user;
    private String pass;
    private String region; //Old region from AWS SDK V1
    private Region awsRegion;
    private String awsProfile;
    private Boolean executaAsRol;
    private String rol;
    private List<String> esquemasExcluidos;

    public Servidor() {
        esquemasExcluidos = new ArrayList<>();
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
        this.esquemasExcluidos = Objects.requireNonNullElseGet(esquemasExcluidos, ArrayList::new);
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Region getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(Region region) {
        this.awsRegion = region;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Servidor servidor)) return false;

        return tipoServidor == servidor.tipoServidor &&
                Objects.equals(host, servidor.host) &&
                Objects.equals(port, servidor.port) &&
                Objects.equals(dataBase, servidor.dataBase) &&
                tipoLogin == servidor.tipoLogin &&
                Objects.equals(user, servidor.user) &&
                Objects.equals(pass, servidor.pass) &&
                Objects.equals(awsProfile, servidor.awsProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipoServidor, host, port, dataBase, tipoLogin, user, pass, awsProfile);
    }
}
