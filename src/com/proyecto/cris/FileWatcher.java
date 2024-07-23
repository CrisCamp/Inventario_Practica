package src.com.proyecto.cris;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class FileWatcher extends Thread {
    private Path path;
    private Runnable callback;

    public FileWatcher(File file, Runnable callback) {
        this.path = file.toPath().getParent();
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException ex) {
                    handleInterruptedException(ex);
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    if (fileName.toString().equals("productos.txt") || fileName.toString().startsWith("ventas_")) {
                        callback.run();
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void handleInterruptedException(InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
}
