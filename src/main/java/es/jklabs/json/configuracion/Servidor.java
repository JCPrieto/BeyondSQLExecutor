package es.jklabs.json.configuracion;

import software.amazon.awssdk.regions.Region;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Servidor implements Serializable {
    @Serial
    private static final long serialVersionUID = 6073042728652109373L;
    private UUID id;
    private String name;
    private TipoServidor tipoServidor;
    private String host;
    private String port;
    private String dataBase;
    private TipoLogin tipoLogin;
    private String user;
    private String pass;
    private String credentialRef;
    private String region; //Old region from AWS SDK V1
    private transient Region awsRegion;
    private String awsProfile;
    private Boolean executaAsRol;
    private String rol;
    private List<String> esquemasExcluidos;

    public Servidor() {
        id = UUID.randomUUID();
        esquemasExcluidos = new ArrayList<>();
    }

    public Servidor(Servidor source) {
        this.id = UUID.randomUUID();
        this.name = source.name;
        this.tipoServidor = source.tipoServidor;
        this.host = source.host;
        this.port = source.port;
        this.dataBase = source.dataBase;
        this.tipoLogin = source.tipoLogin;
        this.user = source.user;
        this.pass = source.pass;
        this.credentialRef = source.credentialRef;
        this.region = source.region;
        this.awsRegion = source.awsRegion;
        this.awsProfile = source.awsProfile;
        this.executaAsRol = source.executaAsRol;
        this.rol = source.rol;
        this.esquemasExcluidos = new ArrayList<>(
                Objects.requireNonNullElseGet(source.esquemasExcluidos, ArrayList::new));
    }

    public UUID getId() {
        return id;
    }

    void assignNewId() {
        id = UUID.randomUUID();
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

    public String getCredentialRef() {
        return credentialRef;
    }

    public void setCredentialRef(String credentialRef) {
        this.credentialRef = credentialRef;
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
        return id != null && id.equals(servidor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
