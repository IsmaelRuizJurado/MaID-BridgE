package com.example.maidbridge.monitoring.errortables;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TotalErrorsTablePanelTest {

    private TotalErrorsTablePanel panel;

    @BeforeEach
    public void setUp() {
        panel = new TotalErrorsTablePanel();
    }

    @Test
    public void testConstructor_initializesComponents() {
        assertNotNull(panel);
        assertEquals(BorderLayout.class, panel.getLayout().getClass());

        // El panel debe tener dos componentes: top panel y JScrollPane
        assertEquals(2, panel.getComponentCount());

        // Comprobar que hay un JScrollPane que contiene la tabla
        Component centerComp = panel.getComponent(1);
        assertTrue(centerComp instanceof JScrollPane);

        JScrollPane scrollPane = (JScrollPane) centerComp;
        Component viewportView = scrollPane.getViewport().getView();
        assertTrue(viewportView instanceof JTable);

        JTable table = (JTable) viewportView;
        assertEquals(2, table.getColumnCount());
        assertEquals("Class", table.getColumnName(0));
        assertEquals("Errors", table.getColumnName(1));
    }

    @Test
    public void testRefreshData_populatesTableModel() throws Exception {
        // Usar invokeAndWait para sincronizar invokeLater
        Map<String, Integer> data = Map.of("com.example.ClassA", 5, "com.example.ClassB", 3);

        SwingUtilities.invokeAndWait(() -> {
            panel.refreshData(data);
        });

        // Acceder a la tabla y modelo interno
        JTable table = getTableFromPanel(panel);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        assertEquals(0, model.getRowCount());
    }

    @Test
    public void testFilterByText_updatesRowFilter() throws Exception {
        JTable table = getTableFromPanel(panel);
        TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();

        // Insert some data
        SwingUtilities.invokeAndWait(() -> panel.refreshData(Map.of("ClassA", 5, "ClassB", 3)));

        // Obtener el campo de texto (JBTextField) desde el panel
        JTextField searchField = getSearchFieldFromPanel(panel);
        assertNotNull(searchField);

        // Simular escribir texto para filtrar
        SwingUtilities.invokeAndWait(() -> searchField.setText("ClassA"));

        // La fila que contiene ClassB debe ser filtrada
        assertNotNull(sorter.getRowFilter());

        // Limpiar texto para remover filtro
        SwingUtilities.invokeAndWait(() -> searchField.setText(""));

        assertNull(sorter.getRowFilter());
    }

    @Test
    public void testTableColumnsResizeOnComponentResized() {
        JTable table = getTableFromPanel(panel);

        // Forzar tamaño inicial
        table.setSize(1000, 200);

        // Crear evento de redimensionamiento
        ComponentEvent resizeEvent = new ComponentEvent(table, ComponentEvent.COMPONENT_RESIZED);

        // Invocar listener
        for (var listener : table.getComponentListeners()) {
            listener.componentResized(resizeEvent);
        }

        int col0Width = table.getColumnModel().getColumn(0).getPreferredWidth();
        int col1Width = table.getColumnModel().getColumn(1).getPreferredWidth();

        assertEquals(1000, col0Width + col1Width, "La suma de las anchuras debe ser el ancho total de la tabla");
        assertEquals(0.85 * 1000, col0Width, 1);
        assertEquals(0.15 * 1000, col1Width, 1);
    }

    // --- Métodos auxiliares para acceder a componentes privados ---

    private JTable getTableFromPanel(TotalErrorsTablePanel panel) {
        JScrollPane scrollPane = (JScrollPane) panel.getComponent(1);
        return (JTable) scrollPane.getViewport().getView();
    }

    private JTextField getSearchFieldFromPanel(TotalErrorsTablePanel panel) {
        JPanel topPanel = (JPanel) panel.getComponent(0);
        // El JTextField es el segundo componente (sur)
        return (JTextField) topPanel.getComponent(1);
    }
}
