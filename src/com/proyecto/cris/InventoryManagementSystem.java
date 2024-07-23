package src.com.proyecto.cris;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class InventoryManagementSystem extends JFrame {
    private File ventasDirectory;
    private File productosDirectory;
    private static final String VENTAS_DIRECTORY = "C:\\Users\\emanu\\OneDrive\\Escritorio\\Ventas";
    private static final String PRODUCTOS_DIRECTORY = "C:\\Users\\emanu\\OneDrive\\Escritorio\\Productos";

    public InventoryManagementSystem() {
        setTitle("Sistema de Gestión de Inventario");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        ventasDirectory = new File(VENTAS_DIRECTORY);
        productosDirectory = new File(PRODUCTOS_DIRECTORY);

        // Verificar si las carpetas existen, si no, crearlas
        if (!ventasDirectory.exists()) {
            ventasDirectory.mkdirs();
        }
        if (!productosDirectory.exists()) {
            productosDirectory.mkdirs();
        }

        JPanel panelPrincipal = new JPanel(new GridLayout(5, 1));
        JButton seleccionarDirectorioButton = new JButton("Ver Directorio de Ventas");
        JButton agregarProductoButton = new JButton("Agregar Producto");
        JButton agregarVentaButton = new JButton("Agregar Venta");
        JButton buscarVentaButton = new JButton("Buscar Venta");
        JButton salirButton = new JButton("Salir");

        seleccionarDirectorioButton.addActionListener(e -> mostrarDirectorioJFileChooser());
        agregarProductoButton.addActionListener(e -> mostrarAgregarProductoFrame());
        agregarVentaButton.addActionListener(e -> mostrarAgregarVentaFrame());
        buscarVentaButton.addActionListener(e -> mostrarBuscarVentaFrame());
        salirButton.addActionListener(e -> System.exit(0));

        panelPrincipal.add(seleccionarDirectorioButton);
        panelPrincipal.add(agregarProductoButton);
        panelPrincipal.add(agregarVentaButton);
        panelPrincipal.add(buscarVentaButton);
        panelPrincipal.add(salirButton);

        add(panelPrincipal);
    }

    private void mostrarDirectorioJFileChooser(){
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(ventasDirectory);   
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isFile()) {
                mostrarContenidoArchivo(selectedFile);
            } else {
                JOptionPane.showMessageDialog(null, "Directorio seleccionado: " + selectedFile.getAbsolutePath());
            }
        }
    }

    private void mostrarAgregarProductoFrame() {
        JFrame agregarProductoFrame = new AgregarProductoFrame(productosDirectory);
        agregarProductoFrame.setVisible(true);
    }

    private void mostrarAgregarVentaFrame() {
        JFrame agregarVentaFrame = new AgregarVentaFrame(ventasDirectory);
        agregarVentaFrame.setVisible(true);
    }

    private void mostrarBuscarVentaFrame() {
        JFrame buscarVentaFrame = new buscarVentas(ventasDirectory);
        buscarVentaFrame.setVisible(true);
    }

    private void mostrarContenidoArchivo(File file) {
        JFrame frame = new JFrame("Contenido de " + file.getName());
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                textArea.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventoryManagementSystem frame = new InventoryManagementSystem();
            frame.setVisible(true);
        });
    }
}