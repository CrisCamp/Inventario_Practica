package src.com.proyecto.cris;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class AgregarProductoFrame extends JFrame {
    private JTextField nombreField;
    private JTextField precioField;
    private JTextField cantidadField;
    private JTextArea productosArea;
    private File productosDirectory;

    public AgregarProductoFrame(File productosDirectory) {
        this.productosDirectory = productosDirectory;
        setTitle("Agregar Producto");
        setSize(400, 300);
        setLocationRelativeTo(null);

        JPanel panelEntrada = new JPanel();
        panelEntrada.setLayout(new GridLayout(4, 2));

        panelEntrada.add(new JLabel("Nombre del Producto:"));
        nombreField = new JTextField();
        panelEntrada.add(nombreField);

        panelEntrada.add(new JLabel("Precio:"));
        precioField = new JTextField();
        panelEntrada.add(precioField);

        panelEntrada.add(new JLabel("Cantidad:"));
        cantidadField = new JTextField();
        panelEntrada.add(cantidadField);

        JButton agregarButton = new JButton("Agregar Producto");
        agregarButton.addActionListener(e -> agregarProducto());
        panelEntrada.add(agregarButton);

        productosArea = new JTextArea();
        productosArea.setEditable(false);
        JScrollPane scrollProductos = new JScrollPane(productosArea);

        add(panelEntrada, BorderLayout.NORTH);
        add(scrollProductos, BorderLayout.CENTER);

        iniciarFileWatcher();
    }

    private void iniciarFileWatcher() {
        File productosFile = new File(productosDirectory, "productos.txt");
        FileWatcher watcher = new FileWatcher(productosFile, this::cargarProductos);
        watcher.start();
    }

    private void cargarProductos() {
        SwingUtilities.invokeLater(() -> {
            productosArea.setText("");
            File productosFile = new File(productosDirectory, "productos.txt");
            if (productosFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(productosFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        productosArea.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void agregarProducto() {
        String nombre = nombreField.getText();
        String precio = precioField.getText();
        String cantidad = cantidadField.getText();

        if (nombre.isEmpty() || precio.isEmpty() || cantidad.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nombre, precio y cantidad son requeridos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double precioDouble = Double.parseDouble(precio);
            int cantidadInt = Integer.parseInt(cantidad);

            File productosFile = new File(productosDirectory, "productos.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(productosFile, true))) {
                writer.write(nombre + "," + precioDouble + "," + cantidadInt);
                writer.newLine();
            }
            cargarProductos();
            nombreField.setText("");
            precioField.setText("");
            cantidadField.setText("");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Precio y cantidad deben ser valores numéricos.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}