package es.jklabs.gui.utilidades.filter.file;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ZipFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        return f.getName().toLowerCase().endsWith(".zip");
    }

    @Override
    public String getDescription() {
        return "Archivos ZIP (*.zip)";
    }
}
