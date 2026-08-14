package es.jklabs.gui.utilidades.listener;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ServidorListenerTest {

    private final JButton eventSource = new JButton();

    @Test
    void rightClickShowsPopupWhenListenerIsEnabled() {
        TrackingPopup popup = new TrackingPopup();
        ServidorListener listener = new ServidorListener(null, null, (mainUI, serverItem) -> popup);

        listener.mousePressed(mousePressed(MouseEvent.BUTTON3));

        assertTrue(popup.shown);
        assertSame(eventSource, popup.invoker);
        assertEquals(10, popup.x);
        assertEquals(20, popup.y);
    }

    @Test
    void leftClickDoesNotCreatePopup() {
        AtomicBoolean popupCreated = new AtomicBoolean(false);
        ServidorListener listener = new ServidorListener(null, null, (mainUI, serverItem) -> {
            popupCreated.set(true);
            return new JPopupMenu();
        });

        listener.mousePressed(mousePressed(MouseEvent.BUTTON1));

        assertFalse(popupCreated.get());
    }

    @Test
    void disabledListenerDoesNotCreatePopupOnRightClick() {
        AtomicBoolean popupCreated = new AtomicBoolean(false);
        ServidorListener listener = new ServidorListener(null, null, (mainUI, serverItem) -> {
            popupCreated.set(true);
            return new JPopupMenu();
        });

        listener.setEnable(false);
        listener.mousePressed(mousePressed(MouseEvent.BUTTON3));

        assertFalse(listener.getEnable());
        assertFalse(popupCreated.get());
    }

    @Test
    void listenerIsEnabledByDefaultAndCanBeReenabled() {
        ServidorListener listener = new ServidorListener(null, null, (mainUI, serverItem) -> new JPopupMenu());

        assertTrue(listener.getEnable());
        listener.setEnable(false);
        listener.setEnable(true);

        assertTrue(listener.getEnable());
    }

    private MouseEvent mousePressed(int button) {
        return new MouseEvent(eventSource, MouseEvent.MOUSE_PRESSED, 0, 0,
                10, 20, 1, false, button);
    }

    private static class TrackingPopup extends JPopupMenu {
        private boolean shown;
        private Component invoker;
        private int x;
        private int y;

        @Override
        public void show(Component invoker, int x, int y) {
            shown = true;
            this.invoker = invoker;
            this.x = x;
            this.y = y;
        }
    }
}
