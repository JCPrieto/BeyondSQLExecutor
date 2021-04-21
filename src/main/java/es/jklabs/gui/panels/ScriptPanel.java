package es.jklabs.gui.panels;

import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.UtilidadesImagenes;
import es.jklabs.gui.utilidades.filter.SqlFilter;
import es.jklabs.gui.utilidades.worker.SqlExecutor;
import es.jklabs.utilidades.Mensajes;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ScriptPanel extends JPanel {

    private final ServersPanel serverPanel;
    private JTextArea jTextArea;
    private JProgressBar progressBar;
    private JButton bntImportar;
    private JButton btnRun;

    public ScriptPanel(ServersPanel serverPanel) {
        super(new BorderLayout());
        this.serverPanel = serverPanel;
        cargarPanel();
    }

    private void cargarPanel() {
        add(cargarPanelEntrada(), BorderLayout.CENTER);
        add(cargarPanelSalida(), BorderLayout.SOUTH);
    }

    private JPanel cargarPanelSalida() {
        return new JPanel();
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
        jTextArea = new JTextArea();
        JScrollPane jScrollPane = new JScrollPane(jTextArea);
        jPanel.add(jScrollPane, BorderLayout.CENTER);
        JPanel jpBotonera2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRun = new JButton("ejecutar");
        btnRun.addActionListener(l -> ejecutarSQL());
        jpBotonera2.add(btnRun);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        jpBotonera2.add(progressBar);
        jPanel.add(jpBotonera2, BorderLayout.SOUTH);
        return jPanel;
    }

    private void ejecutarSQL() {
        try {
            serverPanel.getMainUI().bloquearPantalla();
            List<String> sentencias = dividirEnSentencias();
            int count = sentencias.size();
            count *= Arrays.stream(serverPanel.getPanelServidores().getComponents())
                    .filter(c -> c instanceof ServerItem).mapToInt(c -> (int) ((ServerItem) c).getEsquemas().entrySet().stream()
                            .filter(e -> e.getValue().isSelected()).count()).sum();
            SqlExecutor sqlExecutor = new SqlExecutor(serverPanel, sentencias, count);
            sqlExecutor.addPropertyChangeListener(pcl -> changeListener(pcl.getPropertyName(), pcl.getNewValue()));
            sqlExecutor.execute();
        } catch (IOException e) {
            Growls.mostrarError("procesar.sql", e);
        }
    }

    private void changeListener(String propertyName, Object newValue) {
        if (Objects.equals(propertyName, "progress")) {
            progressBar.setValue((Integer) newValue);
        }
    }

    private List<String> dividirEnSentencias() throws IOException {
        List<String> sentencias = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new StringReader(jTextArea.getText()))) {
            String line;
            StringBuilder nueva = new StringBuilder();
            String delimitador = ";";
            while ((line = br.readLine()) != null) {
                if (StringUtils.isNotEmpty(line)) {
                    if (StringUtils.isEmpty(nueva)) {
                        if (line.startsWith("delimiter") || line.startsWith("DELIMITER")) {
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
                jTextArea.setText(StringUtils.EMPTY);
                String line;
                while ((line = br.readLine()) != null) {
                    jTextArea.append(line + "\n");
                }
            } catch (IOException e) {
                Growls.mostrarError(Mensajes.getError("importar.configuracion"), e);
            }
        }
    }

    public void desbloquearPantalla() {
        jTextArea.setEnabled(true);
        jTextArea.setCursor(null); //turn off the wait cursor
        bntImportar.setEnabled(true);
        btnRun.setEnabled(true);
    }

    public void bloquearPantalla(Cursor waitCursor) {
        jTextArea.setEnabled(false);
        jTextArea.setCursor(waitCursor);
        bntImportar.setEnabled(false);
        btnRun.setEnabled(false);
    }
}
