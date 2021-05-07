package es.jklabs.utilidades;

import es.jklabs.gui.MainUI;

import java.util.List;

public class LazadorHilos extends Thread {
    private final List<Thread> hilos;
    private final MainUI mainUI;

    public LazadorHilos(List<Thread> hilos, MainUI mainUI) {
        this.hilos = hilos;
        this.mainUI = mainUI;
    }

    @Override
    public void run() {
        hilos.forEach(this::run);
        while (hilos.stream().anyMatch(Thread::isAlive)) ;
        mainUI.refresSplit();
    }

    private void run(Thread h) {
        h.start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Logger.error(e);
            Thread.currentThread().interrupt();
        }
    }
}
