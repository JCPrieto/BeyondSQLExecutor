package es.jklabs.gui.utilidades.filter.file;

import es.jklabs.utilidades.Mensajes;
import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Objects;

public class SqlFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        return f.isDirectory() || Objects.equals(FilenameUtils.getExtension(f.getName()), "sql");
    }

    @Override
    public String getDescription() {
        return Mensajes.getMensaje("file.chooser.sql");
    }
}
