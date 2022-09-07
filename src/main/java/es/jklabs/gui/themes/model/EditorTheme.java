package es.jklabs.gui.themes.model;

import java.io.InputStream;

public enum EditorTheme {
    DARK("dark.xml", "Notepad++'s Obsidian"),
    DEFAULT("default.xml", "Default"),
    DEFAULT_ALT("default-alt.xml", "Default (Standar)"),
    DRUID("druid.xml", "Druid"),
    ECLIPSE("eclipse.xml", "Eclipse"),
    IDEA("idea.xml", "IntelliJ IDEA"),
    MONOKAI("monokai.xml", "Monokai"),
    VS("vs.xml", "Visual Studio");

    private final String nombre;
    private final String file;

    EditorTheme(String file, String nombre) {
        this.file = file;
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public InputStream getTheme() {
        return getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/" + file);
    }
}
