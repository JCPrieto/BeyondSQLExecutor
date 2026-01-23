package es.jklabs.storage;

import es.jklabs.json.configuracion.Configuracion;

import java.io.File;

public interface ProjectStore {
    Configuracion load();

    void save(Configuracion configuracion);

    Configuracion importProject(File file, Configuracion existing);

    void exportProject(File file);
}
