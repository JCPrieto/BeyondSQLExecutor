package es.jklabs.gui.utilidades.filtro;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.regex.Pattern;

public class PuertoDocumentoFilter extends DocumentFilter {

    private final Pattern regexCheck = Pattern.compile("^[1-9][0-9]{0,3}$");

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {

        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.insert(offset, string);

        if (regexCheck.matcher(sb).matches()) {
            super.insertString(fb, offset, string, attr);
        }

    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text != null) {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append(doc.getText(0, doc.getLength()));
            sb.replace(offset, offset + length, text);

            if (regexCheck.matcher(sb).matches()) {
                fb.replace(offset, length, text, attrs);
            }
        } else {
            fb.replace(offset, length, "", attrs);
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.delete(offset, offset + length);

        if (sb.length() == 0 || regexCheck.matcher(sb).matches()) {
            super.remove(fb, offset, length);
        }
    }
}
