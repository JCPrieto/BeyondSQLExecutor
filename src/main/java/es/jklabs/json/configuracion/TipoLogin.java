package es.jklabs.json.configuracion;

import es.jklabs.utilidades.Mensajes;

public enum TipoLogin {

    USUARIO_CONTRASENA("usuario.contrasena"),
    AWS_PROFILE("perfil.aws");

    private final String key;

    TipoLogin(String key) {
        this.key = key;
    }

    public String getDescripcion() {
        return Mensajes.getMensaje(this.key);
    }
}
