package es.jklabs.gui.panels;

import es.jklabs.gui.utilidades.Growls;
import es.jklabs.gui.utilidades.IconUtils;
import es.jklabs.gui.utilidades.filter.file.SqlFilter;
import es.jklabs.gui.utilidades.table.model.ResulSetTableModel;
import es.jklabs.gui.utilidades.worker.SqlExecutor;
import es.jklabs.json.configuracion.Servidor;
import es.jklabs.json.configuracion.TipoServidor;
import es.jklabs.utilidades.Mensajes;
import es.jklabs.utilidades.UtilidadesString;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ScriptPanel extends JSplitPane {

    private final ServersPanel serverPanel;
    private RSyntaxTextArea entrada;
    private JProgressBar progressBar;
    private JButton bntImportar;
    private JButton btnRun;
    private JTabbedPane pestanasServidor;
    private JTextArea errores;
    private Map<Servidor, JTabbedPane> pestanas;
    private JButton btnCancel;
    private SqlExecutor sqlExecutor;

    public ScriptPanel(ServersPanel serverPanel) {
        super(VERTICAL_SPLIT);
        this.serverPanel = serverPanel;
        this.pestanas = new HashMap<>();
        cargarPanel();
    }

    private void cargarPanel() {
        setTopComponent(cargarPanelEntrada());
        setBottomComponent(cargarPanelSalida());
    }

    private JTabbedPane cargarPanelSalida() {
        pestanasServidor = new JTabbedPane();
        errores = new JTextArea();
        errores.setEditable(false);
        pestanasServidor.addTab(Mensajes.getMensaje("errores"), new JScrollPane(errores));
        return pestanasServidor;
    }

    private JPanel cargarPanelEntrada() {
        JPanel jPanel = new JPanel(new BorderLayout());
        JPanel jpBotonera1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bntImportar = new JButton(IconUtils.loadIconScaled("upload.png", 24, 24));
        bntImportar.setPreferredSize(new Dimension(30, 30));
        bntImportar.setToolTipText(Mensajes.getMensaje("importar"));
        bntImportar.addActionListener(l -> importarSQL());
        jpBotonera1.add(bntImportar);
        jPanel.add(jpBotonera1, BorderLayout.NORTH);
        entrada = new RSyntaxTextArea();
        entrada.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        entrada.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(entrada);
        jPanel.add(sp, BorderLayout.CENTER);
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
            List<String> sentenciasMysql = dividirEnSentenciasMysql();
            List<String> sentenciasPostgres = dividirEnSentenciasPostgres();
            int countMysql = sentenciasMysql.size();
            countMysql *= Arrays.stream(serverPanel.getPanelServidores().getComponents())
                    .filter(c -> c instanceof ServerItem &&
                            !Objects.equals(((ServerItem) c).getServidor().getTipoServidor(), TipoServidor.POSTGRESQL))
                    .mapToInt(c -> (int) ((ServerItem) c).getEsquemas().entrySet().stream()
                            .filter(e -> e.getValue().isSelected()).count()).sum();
            int countPostreSQL = sentenciasPostgres.size();
            countPostreSQL *= Arrays.stream(serverPanel.getPanelServidores().getComponents())
                    .filter(c -> c instanceof ServerItem &&
                            Objects.equals(((ServerItem) c).getServidor().getTipoServidor(), TipoServidor.POSTGRESQL))
                    .mapToInt(c -> (int) ((ServerItem) c).getEsquemas().entrySet().stream()
                            .filter(e -> e.getValue().isSelected()).count()).sum();
            sqlExecutor = new SqlExecutor(this, serverPanel, sentenciasMysql, countMysql, sentenciasPostgres, countPostreSQL);
            sqlExecutor.addPropertyChangeListener(pcl -> changeListener(pcl.getPropertyName(), pcl.getNewValue()));
            sqlExecutor.execute();
        } catch (IOException e) {
            Growls.mostrarError("procesar.sql", e);
        }
    }

    private List<String> dividirEnSentenciasPostgres() {
        return parseStatements(entrada.getText(), false);
    }

    private void limpiarPestanas() {
        pestanas.forEach((key, value) -> pestanasServidor.remove(value));
        pestanas = new HashMap<>();
        errores.setText(StringUtils.EMPTY);
    }

    private void changeListener(String propertyName, Object newValue) {
        if (Objects.equals(propertyName, "progress")) {
            progressBar.setValue((Integer) newValue);
        }
    }

    private List<String> dividirEnSentenciasMysql() throws IOException {
        return parseStatements(entrada.getText(), true);
    }

    private List<String> parseStatements(String sql, boolean mysql) {
        List<String> sentencias = new ArrayList<>();
        String delimiter = ";";
        StringBuilder current = new StringBuilder();
        String dollarTag = null;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        int length = sql.length();
        int i = 0;
        while (i < length) {
            char ch = sql.charAt(i);
            char next = i + 1 < length ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (ch == '\n') {
                    inLineComment = false;
                    current.append(ch);
                }
                i++;
                continue;
            }

            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    inBlockComment = false;
                    i += 2;
                } else {
                    i++;
                }
                continue;
            }

            if (dollarTag != null) {
                if (sql.startsWith(dollarTag, i)) {
                    current.append(dollarTag);
                    i += dollarTag.length();
                    dollarTag = null;
                } else {
                    current.append(ch);
                    i++;
                }
                continue;
            }

            if (!inSingle && !inDouble) {
                if (ch == '-' && next == '-') {
                    inLineComment = true;
                    i += 2;
                    continue;
                }
                if (ch == '/' && next == '*') {
                    inBlockComment = true;
                    i += 2;
                    continue;
                }
            }

            if (!inDouble && ch == '\'') {
                inSingle = !inSingle;
                current.append(ch);
                i++;
                continue;
            }
            if (!inSingle && ch == '"') {
                inDouble = !inDouble;
                current.append(ch);
                i++;
                continue;
            }

            if (!mysql && !inSingle && !inDouble && ch == '$') {
                String tag = readDollarTag(sql, i);
                if (tag != null) {
                    dollarTag = tag;
                    current.append(tag);
                    i += tag.length();
                    continue;
                }
            }

            if (mysql && !inSingle && !inDouble) {
                int lineStart = current.length();
                if ((lineStart == 0 || current.charAt(lineStart - 1) == '\n') &&
                        startsWithDelimiterDirective(sql, i)) {
                    int consumed = consumeDelimiterDirective(sql, i);
                    String newDelimiter = extractDelimiter(sql.substring(i, i + consumed));
                    if (StringUtils.isNotEmpty(newDelimiter)) {
                        delimiter = newDelimiter;
                    }
                    i += consumed;
                    continue;
                }
            }

            if (!inSingle && !inDouble && delimiterMatches(sql, i, delimiter)) {
                String stmt = current.toString().trim();
                if (StringUtils.isNotEmpty(stmt)) {
                    sentencias.add(stmt);
                }
                current.setLength(0);
                i += delimiter.length();
                continue;
            }

            current.append(ch);
            i++;
        }
        String stmt = current.toString().trim();
        if (StringUtils.isNotEmpty(stmt)) {
            sentencias.add(stmt);
        }
        return sentencias;
    }

    private boolean delimiterMatches(String sql, int index, String delimiter) {
        if (StringUtils.isEmpty(delimiter)) {
            return false;
        }
        return index + delimiter.length() <= sql.length() && sql.startsWith(delimiter, index);
    }

    private boolean startsWithDelimiterDirective(String sql, int index) {
        if (!sql.regionMatches(true, index, "delimiter", 0, "delimiter".length())) {
            return false;
        }
        int end = index + "delimiter".length();
        return end < sql.length() && Character.isWhitespace(sql.charAt(end));
    }

    private int consumeDelimiterDirective(String sql, int index) {
        int i = index;
        while (i < sql.length() && sql.charAt(i) != '\n') {
            i++;
        }
        if (i < sql.length()) {
            i++;
        }
        return i - index;
    }

    private String extractDelimiter(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }

    private String readDollarTag(String sql, int index) {
        int end = index + 1;
        while (end < sql.length()) {
            char ch = sql.charAt(end);
            if (ch == '$') {
                return sql.substring(index, end + 1);
            }
            if (!Character.isLetterOrDigit(ch) && ch != '_') {
                return null;
            }
            end++;
        }
        return null;
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

    public void addResultadoQuery(Servidor servidor, String esquema, String sentencia, Map.Entry<List<String>, List<Object[]>> resultado) {
        JTabbedPane pestanasEsquema;
        int max = 20;
        String name = sentencia;
        if (sentencia.length() - 1 > max) {
            name = sentencia.substring(0, 20) + "...";
        }
        JScrollPane tableResultado = getTablaResultado(resultado);
        if (pestanas.containsKey(servidor)) {
            pestanasEsquema = pestanas.get(servidor);
            pestanasEsquema.addTab(esquema + " - " + name, tableResultado);
        } else {
            pestanasEsquema = new JTabbedPane();
            pestanas.put(servidor, pestanasEsquema);
            pestanasEsquema.addTab(esquema + " - " + name, tableResultado);
            pestanasServidor.addTab(servidor.getName(), pestanasEsquema);
        }
    }

    private JScrollPane getTablaResultado(Map.Entry<List<String>, List<Object[]>> resultado) {
        JTable tablaResultado = new JTable();
        tablaResultado.setModel(new ResulSetTableModel(resultado));
        tablaResultado.setFillsViewportHeight(true);
        tablaResultado.setAutoCreateRowSorter(true);
        if (resultado.getKey().size() > 5) {
            tablaResultado.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
        return new JScrollPane(tablaResultado, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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

    public RSyntaxTextArea getEntrada() {
        return entrada;
    }

}
