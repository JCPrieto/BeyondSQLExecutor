package es.jklabs.gui.utilidades.filter.document;

import org.junit.jupiter.api.Test;

import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PuertoDocumentoFilterTest {

    private static PlainDocument createDocument() {
        PlainDocument document = new PlainDocument();
        ((AbstractDocument) document).setDocumentFilter(new PuertoDocumentoFilter());
        return document;
    }

    private static PlainDocument createDocumentWithText(String text) throws BadLocationException {
        PlainDocument document = createDocument();
        document.insertString(0, text, null);
        return document;
    }

    private static String textOf(PlainDocument document) throws BadLocationException {
        return document.getText(0, document.getLength());
    }

    @Test
    void insertStringAllowsValidPortText() throws BadLocationException {
        PlainDocument document = createDocument();

        document.insertString(0, "5432", null);

        assertEquals("5432", textOf(document));
    }

    @Test
    void insertStringRejectsInvalidPortText() throws BadLocationException {
        PlainDocument document = createDocument();

        document.insertString(0, "0", null);

        assertEquals("", textOf(document));
    }

    @Test
    void replaceAllowsValidPortText() throws BadLocationException {
        PlainDocument document = createDocumentWithText("5432");

        document.replace(0, 4, "3306", null);

        assertEquals("3306", textOf(document));
    }

    @Test
    void replaceRejectsInvalidPortText() throws BadLocationException {
        PlainDocument document = createDocumentWithText("5432");

        document.replace(0, 4, "65535", null);

        assertEquals("5432", textOf(document));
    }

    @Test
    void replaceWithNullTextRemovesSelectedContent() throws BadLocationException {
        PlainDocument document = createDocumentWithText("5432");

        document.replace(1, 2, null, null);

        assertEquals("52", textOf(document));
    }

    @Test
    void removeAllowsDeletionWhenRemainingTextIsStillValid() throws BadLocationException {
        PlainDocument document = createDocumentWithText("5432");

        document.remove(3, 1);

        assertEquals("543", textOf(document));
    }

    @Test
    void removeAllowsDeletionWhenDocumentBecomesEmpty() throws BadLocationException {
        PlainDocument document = createDocumentWithText("9");

        document.remove(0, 1);

        assertEquals("", textOf(document));
    }

    @Test
    void removeRejectsDeletionWhenRemainingTextWouldBeInvalid() throws BadLocationException {
        PlainDocument document = createDocumentWithText("10");

        document.remove(0, 1);

        assertEquals("10", textOf(document));
    }
}
