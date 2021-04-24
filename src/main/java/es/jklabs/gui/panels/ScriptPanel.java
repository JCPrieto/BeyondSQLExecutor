package es.jklabs.gui.panels;

import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.UtilidadesImagenes;
import es.jklabs.gui.utilidades.filter.SqlFilter;
import es.jklabs.gui.utilidades.table.model.ResulSetTableModel;
import es.jklabs.gui.utilidades.worker.SqlExecutor;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesString;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

public class ScriptPanel extends JSplitPane {

    private final ServersPanel serverPanel;
    private JTextArea entrada;
    private JProgressBar progressBar;
    private JButton bntImportar;
    private JButton btnRun;
    private JTabbedPane panelesSalida;
    private JTextArea errores;
    private Map<Servidor, JTabbedPane> pestanas;
    private Map<Servidor, Map<String, JPanel>> subPestanas;
    private JButton btnCancel;
    private SqlExecutor sqlExecutor;

    public ScriptPanel(ServersPanel serverPanel) {
        super(VERTICAL_SPLIT);
        this.serverPanel = serverPanel;
        this.pestanas = new HashMap<>();
        this.subPestanas = new HashMap<>();
        cargarPanel();
    }

    private void cargarPanel() {
        setTopComponent(cargarPanelEntrada());
        setBottomComponent(cargarPanelSalida());
    }

    private JTabbedPane cargarPanelSalida() {
        panelesSalida = new JTabbedPane();
        errores = new JTextArea();
        errores.setEditable(false);
        panelesSalida.addTab(Mensajes.getMensaje("errores"), new JScrollPane(errores));
        return panelesSalida;
    }

    private JPanel cargarPanelEntrada() {
        JPanel jPanel = new JPanel(new BorderLayout());
        JPanel jpBotonera1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bntImportar = new JButton(UtilidadesImagenes.getIcono("upload.png"));
        bntImportar.setPreferredSize(new Dimension(30, 30));
        bntImportar.setToolTipText(Mensajes.getMensaje("importar"));
        bntImportar.addActionListener(l -> importarSQL());
        jpBotonera1.add(bntImportar);
        jPanel.add(jpBotonera1, BorderLayout.NORTH);
        entrada = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(entrada);
        jPanel.add(jScrollPane, BorderLayout.CENTER);
        JPanel jpBotonera2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRun = new JButton(Mensajes.getMensaje("ejecutar"));
        btnRun.addActionListener(l -> ejecutarSQL());
        jpBotonera2.add(btnRun);
        btnCancel = new JButton(Mensajes.getMensaje("cancelar"));
        btnCancel.addActionListener(l -> cancalerEjecucion());
        btnCancel.setEnabled(false);
        jpBotonera2.add(btnCancel);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        jpBotonera2.add(progressBar);
        jPanel.add(jpBotonera2, BorderLayout.SOUTH);
        return jPanel;
    }

    private void cancalerEjecucion() {
        sqlExecutor.cancel(true);
    }

    private void ejecutarSQL() {
        try {
            limpiarPestanas();
            serverPanel.getMainUI().bloquearPantalla();
            List<String> sentencias = dividirEnSentencias();
            int count = sentencias.size();
            count *= Arrays.stream(serverPanel.getPanelServidores().getComponents())
                    .filter(ServerItem.class::isInstance).mapToInt(c -> (int) ((ServerItem) c).getEsquemas().entrySet().stream()
                            .filter(e -> e.getValue().isSelected()).count()).sum();
            sqlExecutor = new SqlExecutor(this, serverPanel, sentencias, count);
            sqlExecutor.addPropertyChangeListener(pcl -> changeListener(pcl.getPropertyName(), pcl.getNewValue()));
            sqlExecutor.execute();
        } catch (IOException e) {
            Growls.mostrarError("procesar.sql", e);
        }
    }

    private void limpiarPestanas() {
        pestanas.forEach((key, value) -> panelesSalida.remove(value));
        pestanas = new HashMap<>();
        subPestanas = new HashMap<>();
        errores.setText(StringUtils.EMPTY);
    }

    private void changeListener(String propertyName, Object newValue) {
        if (Objects.equals(propertyName, "progress")) {
            progressBar.setValue((Integer) newValue);
        }
    }

    private List<String> dividirEnSentencias() throws IOException {
        List<String> sentencias = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new StringReader(entrada.getText()))) {
            String line;
            StringBuilder nueva = new StringBuilder();
            String delimitador = ";";
            while ((line = br.readLine()) != null) {
                if (StringUtils.isNotEmpty(line)) {
                    if (StringUtils.isEmpty(nueva)) {
                        if (line.toLowerCase().startsWith("delimiter")) {
                            String[] split = line.split(" ");
                            delimitador = split[split.length - 1];
                        } else {
                            nueva.append(line);
                            if (line.endsWith(delimitador)) {
                                sentencias.add(nueva.toString().replace(delimitador, ";"));
                                nueva = new StringBuilder();
                            }
                        }
                    } else {
                        nueva.append(line);
                        if (line.endsWith(delimitador)) {
                            sentencias.add(nueva.toString().replace(delimitador, ";"));
                            nueva = new StringBuilder();
                        }
                    }
                }
            }
        }
        return sentencias;
    }

    private void importarSQL() {
        JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new SqlFilter());
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int retorno = fc.showOpenDialog(this);
        if (retorno == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try (FileReader fr = new FileReader(file);
                 BufferedReader br = new BufferedReader(fr)) {
                entrada.setText(StringUtils.EMPTY);
                String line;
                while ((line = br.readLine()) != null) {
                    entrada.append(line + "\n");
                }
            } catch (IOException e) {
                Growls.mostrarError(Mensajes.getError("importar.configuracion"), e);
            }
        }
    }

    public void desbloquearPantalla() {
        entrada.setEnabled(true);
        entrada.setCursor(null); //turn off the wait cursor
        bntImportar.setEnabled(true);
        btnRun.setEnabled(true);
        btnCancel.setEnabled(false);
    }

    public void bloquearPantalla(Cursor waitCursor) {
        entrada.setEnabled(false);
        entrada.setCursor(waitCursor);
        bntImportar.setEnabled(false);
        btnRun.setEnabled(false);
        btnCancel.setEnabled(true);
    }

    public void addResultadoQuery(Servidor servidor, String esquema, Map.Entry<List<String>, List<Object[]>> resultado) {
        JTabbedPane pestana;
        JPanel subPestana;
        if (pestanas.containsKey(servidor)) {
            pestana = pestanas.get(servidor);
            if (subPestanas.get(servidor).containsKey(esquema)) {
                subPestana = subPestanas.get(servidor).get(esquema);
            } else {
                subPestana = new JPanel();
                subPestana.setLayout(new BoxLayout(subPestana, BoxLayout.Y_AXIS));
                subPestanas.get(servidor).put(esquema, subPestana);
                pestana.addTab(esquema, subPestana);
            }
        } else {
            pestana = new JTabbedPane();
            pestanas.put(servidor, pestana);
            Map<String, JPanel> map = new HashMap<>();
            subPestana = new JPanel();
            subPestana.setLayout(new BoxLayout(subPestana, BoxLayout.Y_AXIS));
            map.put(esquema, subPestana);
            subPestanas.put(servidor, map);
            pestana.addTab(esquema, subPestana);
            panelesSalida.addTab(servidor.getName(), pestana);
        }
        JTable tabla = new JTable();
        tabla.setModel(new ResulSetTableModel(resultado));
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        subPestana.add(new JScrollPane(tabla, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    public void refresSplit() {
        setDividerLocation(0.75);
    }

    public void addError(String servidor, String esquema, String sentencia, String message) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!UtilidadesString.isEmpty(errores)) {
            stringBuilder.append("\n");
        }
        stringBuilder.append(servidor).append(" - ").append(esquema).append("\n");
        stringBuilder.append(Mensajes.getMensaje("sentencia", new String[]{sentencia}));
        stringBuilder.append(message).append("\n");
        errores.setText(errores.getText() + stringBuilder);
    }
}
