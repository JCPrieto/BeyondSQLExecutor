package es.jklabs.gui.utilidades.table.model;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.Serial;
import java.util.List;
import java.util.Map;

public class ResulSetTableModel extends DefaultTableModel implements TableModel {
    @Serial
    private static final long serialVersionUID = 5033776013523885275L;

    public ResulSetTableModel(Map.Entry<List<String>, List<Object[]>> resultado) {
        super();
        Object[][] data = new Object[resultado.getValue().size()][resultado.getKey().size()];
        for (int i = 0; i < resultado.getValue().size(); i++) {
            System.arraycopy(resultado.getValue().get(i), 0, data[i], 0, resultado.getValue().get(i).length);
        }
        super.setDataVector(data, resultado.getKey().toArray(new String[0]));
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

}
