package src.com.proyecto.cris;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Clase Productos para almacenar los detalles del productos
class Productos {
    String nombre;
    int cantidad;
    double total;

    Productos(String nombre, int cantidad, double total) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.total = total;
    }

    @Override
    public String toString() {
        return nombre + "," + cantidad + ",$" + total;
    }
}

// Clase Venta para almacenar los detalles de cada venta
class Venta {
    int cantidadVentas;
    String hora;
    List<Productos> productoss;
    double total;

    Venta(int cantidadVentas, String hora, List<Productos> productoss, double total) {
        this.cantidadVentas = cantidadVentas;
        this.hora = hora;
        this.productoss = productoss;
        this.total = total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ventas,").append(cantidadVentas).append("\n");
        sb.append("Hora: ").append(hora).append("\n");
        for (Productos p : productoss) {
            sb.append(p.toString()).append("\n");
        }
        sb.append("total: $").append(total).append("\n");
        return sb.toString();
    }
}

// Clase principal para la aplicación de ventas
public class buscarVentas extends JFrame {
    private static final int BUFFER_SIZE = 10;
    
    private File ventasDirectory;
    private JTextField fileNameField;
    private JTextField cantidadField;
    private JTextField horaField;
    private JTextField totalField;
    private JTextField productossField;
    private JTextArea textArea;
    private JLabel contadorLabel; // Label para mostrar el conteo de coincidencias

    public buscarVentas(File ventasDirectory) {
        this.ventasDirectory = ventasDirectory;
        setTitle("Ventas App");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(7, 2));

        panel.add(new JLabel("Nombre del archivo:"));
        fileNameField = new JTextField();
        panel.add(fileNameField);

        panel.add(new JLabel("Cantidad de ventas:"));
        cantidadField = new JTextField();
        panel.add(cantidadField);

        panel.add(new JLabel("Hora de venta:"));
        horaField = new JTextField();
        panel.add(horaField);

        panel.add(new JLabel("Total de ventas:"));
        totalField = new JTextField();
        panel.add(totalField);

        panel.add(new JLabel("Productoss vendidos (nombre,cantidad,total por cantidad;...):"));
        productossField = new JTextField();
        panel.add(productossField);

        panel.add(new JLabel("Número de coincidencias:"));
        contadorLabel = new JLabel("0");
        panel.add(contadorLabel);

        JButton buscarButton = new JButton("Buscar");
        panel.add(buscarButton);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        add(panel, BorderLayout.NORTH);

        buscarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buscarVentas();
            }
        });
    }

    private void buscarVentas() {
        textArea.setText("");
        String searchFileName = fileNameField.getText().trim();
        String searchCantidad = cantidadField.getText().trim();
        String searchHora = horaField.getText().trim();
        String searchTotal = totalField.getText().trim();
        String searchProductoss = productossField.getText().trim();
        AtomicInteger contador = new AtomicInteger(0);

        if (ventasDirectory.exists() && ventasDirectory.isDirectory()) {
            File[] archivos = ventasDirectory.listFiles((dir, name) -> name.endsWith(".txt"));

            if (archivos != null) {
                BlockingQueue<File> buffer = new ArrayBlockingQueue<>(BUFFER_SIZE);

                ExecutorService producerService = Executors.newSingleThreadExecutor();
                ExecutorService consumerService = Executors.newFixedThreadPool(4);

                StringBuilder resultadosBuilder = new StringBuilder();

                producerService.submit(new FileProducer(buffer, archivos));

                for (int i = 0; i < 4; i++) {
                    consumerService.submit(new FileConsumer(buffer, searchFileName, searchCantidad, searchHora, searchTotal, searchProductoss, resultadosBuilder, contador));
                }

                producerService.shutdown();
                consumerService.shutdown();

                try {
                    producerService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    consumerService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                textArea.setText(resultadosBuilder.toString());
                highlightText(textArea, searchFileName, searchCantidad, searchHora, searchTotal, searchProductoss);
                contadorLabel.setText(String.valueOf(contador.get()));
            } else {
                textArea.append("No se encontraron archivos en la ruta especificada.\n");
            }
        } else {
            textArea.append("La ruta especificada no existe o no es un directorio.\n");
        }
    }

    private void highlightText(JTextArea area, String searchFileName, String searchCantidad, String searchHora, String searchTotal, String searchProductoss) {
        DefaultHighlighter.DefaultHighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
        Highlighter h = area.getHighlighter();
        h.removeAllHighlights();

        String text = area.getText();
        highlight(text, searchFileName, h, highlightPainter);
        highlight(text, "ventas," + searchCantidad, h, highlightPainter);
        highlight(text, "Hora: " + searchHora, h, highlightPainter);
        highlight(text, "total: $" + searchTotal, h, highlightPainter);

        if (!searchProductoss.isEmpty()) {
            String[] productoss = searchProductoss.split(";");
            for (String productos : productoss) {
                highlight(text, productos, h, highlightPainter);
            }
        }
    }

    private void highlight(String content, String pattern, Highlighter highlighter, Highlighter.HighlightPainter painter) {
        if (pattern != null && !pattern.isEmpty()) {
            Pattern p = Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            while (m.find()) {
                try {
                    highlighter.addHighlight(m.start(), m.end(), painter);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

class FileProducer implements Runnable {
    private final BlockingQueue<File> buffer;
    private final File[] archivos;

    public FileProducer(BlockingQueue<File> buffer, File[] archivos) {
        this.buffer = buffer;
        this.archivos = archivos;
    }

    @Override
    public void run() {
        try {
            for (File archivo : archivos) {
                buffer.put(archivo);
            }
            for (int i = 0; i < 4; i++) {
                buffer.put(new File("POISON"));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class FileConsumer implements Runnable {
    private final BlockingQueue<File> buffer;
    private final String searchFileName;
    private final String searchCantidad;
    private final String searchHora;
    private final String searchTotal;
    private final String searchProductoss;
    private final StringBuilder resultadosBuilder;
    private final AtomicInteger contador;

    public FileConsumer(BlockingQueue<File> buffer, String searchFileName, String searchCantidad, String searchHora, String searchTotal, String searchProductoss, StringBuilder resultadosBuilder, AtomicInteger contador) {
        this.buffer = buffer;
        this.searchFileName = searchFileName;
        this.searchCantidad = searchCantidad;
        this.searchHora = searchHora;
        this.searchTotal = searchTotal;
        this.searchProductoss = searchProductoss;
        this.resultadosBuilder = resultadosBuilder;
        this.contador = contador;
    }

    @Override
    public void run() {
        try {
            while (true) {
                File archivo = buffer.take();
                if (archivo.getName().equals("POISON")) {
                    break;
                }
                procesarArchivo(archivo);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void procesarArchivo(File archivo) throws IOException {
        List<String> lines = Files.readAllLines(archivo.toPath());
        StringBuilder contenido = new StringBuilder();
        for (String line : lines) {
            contenido.append(line).append("\n");
        }
        String content = contenido.toString();

        boolean match = true;
        if (!searchFileName.isEmpty() && !archivo.getName().contains(searchFileName)) {
            match = false;
        }
        if (match && !searchCantidad.isEmpty() && !content.contains("ventas," + searchCantidad)) {
            match = false;
        }
        if (match && !searchHora.isEmpty() && !content.contains("Hora: " + searchHora)) {
            match = false;
        }
        if (match && !searchTotal.isEmpty() && !content.contains("total: $" + searchTotal)) {
            match = false;
        }
        if (match && !searchProductoss.isEmpty()) {
            String[] productoss = searchProductoss.split(";");
            for (String productos : productoss) {
                if (!content.contains(productos)) {
                    match = false;
                    break;
                }
            }
        }

        if (match) {
            synchronized (resultadosBuilder) {
                resultadosBuilder.append("Archivo: ").append(archivo.getName()).append("\n");
                resultadosBuilder.append(content).append("\n\n");
                contador.incrementAndGet();
            }
        }
    }
}
