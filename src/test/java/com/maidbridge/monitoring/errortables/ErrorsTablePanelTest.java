package com.maidbridge.monitoring.errortables;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ErrorsTablePanelTest {

    private ErrorsTablePanel panel;

    @BeforeEach
    public void setup() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> panel = new ErrorsTablePanel());
    }

    @Test
    public void testInitialTableIsEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TableModel model = panel.tableModel;
            assertEquals(0, model.getRowCount(), "La tabla debe estar inicialmente vacía");
            assertEquals(5, model.getColumnCount(), "La tabla debe tener 5 columnas");
        });
    }

    @Test
    public void testRefreshDataAddsRows() throws Exception {
        ErrorsTablePanel.ErrorsTableEntry e1 = new ErrorsTablePanel.ErrorsTableEntry("ClassA", "NullPointerException", 10, "Cause A", 3);
        ErrorsTablePanel.ErrorsTableEntry e2 = new ErrorsTablePanel.ErrorsTableEntry("ClassB", "IndexOutOfBoundsException", 20, "Cause B", 5);

        SwingUtilities.invokeAndWait(() -> {
            panel.refreshData(List.of(e1, e2));
        });

        SwingUtilities.invokeAndWait(() -> {
            TableModel model = panel.tableModel;
            assertEquals(2, model.getRowCount(), "La tabla debe tener 2 filas tras refreshData");

            assertEquals("ClassA", model.getValueAt(0, 0));
            assertEquals("NullPointerException", model.getValueAt(0, 1));
            assertEquals(10, model.getValueAt(0, 2));
            assertEquals("Cause A", model.getValueAt(0, 3));
            assertEquals(3, model.getValueAt(0, 4));

            assertEquals("ClassB", model.getValueAt(1, 0));
            assertEquals("IndexOutOfBoundsException", model.getValueAt(1, 1));
            assertEquals(20, model.getValueAt(1, 2));
            assertEquals("Cause B", model.getValueAt(1, 3));
            assertEquals(5, model.getValueAt(1, 4));
        });
    }

    @Test
    public void testFilterByClassName() throws Exception {
        ErrorsTablePanel.ErrorsTableEntry e1 = new ErrorsTablePanel.ErrorsTableEntry("MyClass", "ErrorType1", 1, "Cause1", 1);
        ErrorsTablePanel.ErrorsTableEntry e2 = new ErrorsTablePanel.ErrorsTableEntry("OtherClass", "ErrorType2", 2, "Cause2", 2);

        SwingUtilities.invokeAndWait(() -> panel.refreshData(List.of(e1, e2)));

        SwingUtilities.invokeAndWait(() -> {
            TableRowSorter<?> sorter = panel.sorter;
            // Inicialmente no hay filtro
            assertNull(sorter.getRowFilter());

            // Aplicar filtro para "MyClass"
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + "MyClass", 0));
            assertEquals(1, sorter.getViewRowCount(), "Debe filtrar filas que coincidan con MyClass");

            // Aplicar filtro para texto que no existe
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + "NonExistentClass", 0));
            assertEquals(0, sorter.getViewRowCount(), "No debe mostrar filas que no coincidan");
        });
    }
}
