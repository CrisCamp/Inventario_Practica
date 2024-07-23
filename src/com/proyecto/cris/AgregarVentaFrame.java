package src.com.proyecto.cris;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgregarVentaFrame extends JFrame {
    private JComboBox<String> productosComboBox;
    private JTextField cantidadField;
    private JTextArea ventasArea;
    private JTextArea productosArea;
    private File ventasDirectory;
    private List<String> productosList;
    private Map<String, Producto> productosMap;
    private Map<String, Integer> ventasAcumuladasMap;

    public AgregarVentaFrame(File ventasDirectory) {
        this.ventasDirectory = ventasDirectory;
        productosList = new ArrayList<>();
        productosMap = new HashMap<>();

        setTitle("Agregar Venta");
        setSize(600, 400);
        setLocationRelativeTo(null);

        cargarProductos();

        JPanel panelEntrada = new JPanel();
        panelEntrada.setLayout(new GridLayout(4, 2));

        panelEntrada.add(new JLabel("Producto:"));
        productosComboBox = new JComboBox<>(productosList.toArray(new String[0]));
        panelEntrada.add(productosComboBox);

        panelEntrada.add(new JLabel("Cantidad:"));
        cantidadField = new JTextField();
        panelEntrada.add(cantidadField);

        JButton agregarButton = new JButton("Agregar Venta");
        agregarButton.addActionListener(e -> agregarVenta());
        panelEntrada.add(agregarButton);

        JButton finalizarButton = new JButton("Finalizar Ventas");
        finalizarButton.addActionListener(e -> finalizarVentas());
        panelEntrada.add(finalizarButton);

        ventasArea = new JTextArea();
        ventasArea.setEditable(false);
        JScrollPane scrollVentas = new JScrollPane(ventasArea);

        productosArea = new JTextArea();
        productosArea.setEditable(false);
        JScrollPane scrollProductos = new JScrollPane(productosArea);

        actualizarProductosArea();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollVentas, scrollProductos);
        splitPane.setDividerLocation(300);

        add(panelEntrada, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        ventasAcumuladasMap = new HashMap<>();

        iniciarFileWatcher();
    }

    private void iniciarFileWatcher() {
        File productosFile = new File("C:\\Users\\emanu\\OneDrive\\Escritorio\\Productos", "productos.txt");
        FileWatcher watcher = new FileWatcher(productosFile, this::cargarProductos);
        watcher.start();
    }

    private void cargarProductos() {
        SwingUtilities.invokeLater(() -> {
            productosList.clear();
            productosMap.clear();
            File productosFile = new File("C:\\Users\\emanu\\OneDrive\\Escritorio\\Productos", "productos.txt");
            if (productosFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(productosFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        String nombre = parts[0];
                        double precio = Double.parseDouble(parts[1]);
                        int cantidad = Integer.parseInt(parts[2]);
                        productosList.add(nombre);
                        productosMap.put(nombre, new Producto(nombre, precio, cantidad));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "No se encontraron productos. Agregue productos primero.", "Error", JOptionPane.ERROR_MESSAGE);
            }

            productosComboBox.setModel(new DefaultComboBoxModel<>(productosList.toArray(new String[0])));
            actualizarProductosArea();
        });
    }

    private void agregarVenta() {
        String producto = (String) productosComboBox.getSelectedItem();
        String cantidadStr = cantidadField.getText();
        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Producto prod = productosMap.get(producto);
        int stockProducto = prod.getCantidad();
        int productosAcumulados = stockProducto - ventasAcumuladasMap.getOrDefault(producto, 0);

        if (cantidad > productosAcumulados) {
            JOptionPane.showMessageDialog(this, "Cantidad insuficiente en el stock para \"" + producto + "\". Actualmente solo hay " + productosAcumulados + " en stock.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ventasAcumuladasMap.put(producto, ventasAcumuladasMap.getOrDefault(producto, 0) + cantidad);
        actualizarVentasArea();

        cantidadField.setText("");
    }

    private void actualizarVentasArea() {
        ventasArea.setText("");
        for (Map.Entry<String, Integer> entry : ventasAcumuladasMap.entrySet()) {
            String producto = entry.getKey();
            int cantidad = entry.getValue();
            double precio = productosMap.get(producto).getPrecio();
            double total = cantidad * precio;
            ventasArea.append("Venta: Producto: " + producto + ", Cantidad: " + cantidad + ", Total: $" + total + "\n");
        }
    }

    private void actualizarProductosArea() {
        productosArea.setText("");
        for (Producto producto : productosMap.values()) {
            productosArea.append("Producto: " + producto.getNombre() + " - Precio: $" + producto.getPrecio() + " - Cantidad: " + producto.getCantidad() + "\n");
            // productosArea.append("Producto: " + producto.getNombre() + "\n");
            // productosArea.append("Precio: $" + producto.getPrecio() + "\n");
            // productosArea.append("Cantidad: " + producto.getCantidad() + "\n");
            // productosArea.append("------------------------------\n");
        }
    }

    private void finalizarVentas() {
        if (ventasAcumuladasMap.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay ventas para finalizar.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (Map.Entry<String, Integer> entry : ventasAcumuladasMap.entrySet()) {
            String producto = entry.getKey();
            int cantidad = entry.getValue();
            Producto prod = productosMap.get(producto);
            int stockProducto = prod.getCantidad();
            prod.reducirCantidad(cantidad);
            if (prod.getCantidad() < 0) {
                int value = ventasAcumuladasMap.remove(producto); // Elimina la clave 'producto' del map y devuelve su valor
                prod.aumentarCantidad(value);
                JOptionPane.showMessageDialog(this, "Error en la transaccion del producto \"" + producto + "\" \nHubo una transaccion antes que esta \nSolo hay  " + stockProducto + " productos en el stock", "Error", JOptionPane.ERROR_MESSAGE);
                // En caso de que el producto eliminado fuera el unico en el map
                if (ventasAcumuladasMap.isEmpty()) {
                    return;
                }
            }
        }

        actualizarArchivoProductos();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        LocalDateTime now = LocalDateTime.now();

        String fechaArchivo = dateFormatter.format(now);
        String fecha = dateTimeFormatter.format(now);
        String hora = timeFormatter.format(now);

        File ventasFile = new File(ventasDirectory, "ventas_" + fechaArchivo + ".txt");
        double totalPagar = ventasAcumuladasMap.entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * productosMap.get(entry.getKey()).getPrecio()).sum();
        int cantidadVentas = ventasAcumuladasMap.size();

        try {
            boolean isNewFile = ventasFile.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ventasFile, true))) {
                if (isNewFile) {
                    writer.write("Fecha: " + fecha);
                    writer.newLine();
                    writer.newLine();
                }
                writer.write("ventas," + cantidadVentas);
                writer.newLine();
                writer.write("Hora: " + hora);
                writer.newLine();
                for (Map.Entry<String, Integer> entry : ventasAcumuladasMap.entrySet()) {
                    String producto = entry.getKey();
                    int cantidad = entry.getValue();
                    double total = cantidad * productosMap.get(producto).getPrecio();
                    writer.write(producto + "," + cantidad + ",$" + total);
                    writer.newLine();
                }
                writer.write("total: $" + totalPagar);
                writer.newLine();
                writer.newLine();
            }

            ventasArea.append("Ventas finalizadas. Total a pagar: $" + totalPagar + "\n");
            ventasAcumuladasMap.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

        actualizarProductosArea();
    }

    private void actualizarArchivoProductos() {
        File productosFile = new File("C:\\Users\\emanu\\OneDrive\\Escritorio\\Productos", "productos.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(productosFile))) {
            for (Producto prod : productosMap.values()) {
                writer.write(prod.getNombre() + "," + prod.getPrecio() + "," + prod.getCantidad());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
