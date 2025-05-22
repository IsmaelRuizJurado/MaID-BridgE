package com.maidbridge.monitoring.errortables;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.regex.Pattern;


public class ErrorsTablePanel extends JPanel {

    public final DefaultTableModel tableModel;
    public final TableRowSorter<TableModel> sorter;

    public ErrorsTablePanel() {
        super(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Class", "Error Type", "Line", "Cause", "Occurrences"}, 0);
        JBTable table = new JBTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.getEmptyText().setText("Waiting for error analysis...");
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Configurar cabecera
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        TableCellRenderer defaultRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            Component comp = defaultRenderer.getTableCellRendererComponent(table1, value, isSelected, hasFocus, row, column);
            if (comp instanceof JLabel label) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setHorizontalAlignment(SwingConstants.CENTER);
            }
            return comp;
        });

        // Alineación de columna numérica
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Aplicar proporciones al cambiar el tamaño
        table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int totalWidth = table.getWidth();
                int[] proportions = {20, 25, 10, 30, 15};
                TableColumnModel columnModel = table.getColumnModel();
                for (int i = 0; i < proportions.length; i++) {
                    columnModel.getColumn(i).setPreferredWidth(totalWidth * proportions[i] / 100);
                }
            }
        });

        JBTextField searchField = new JBTextField();
        searchField.getEmptyText().setText("Filter by class name...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { updateFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { updateFilter(); }
            private void updateFilter() {
                String text = searchField.getText();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0));
                }
            }
        });

        // Crear título
        JLabel titleLabel = new JLabel("Detailed errors");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Panel superior con título + filtro
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(searchField, BorderLayout.SOUTH);

        // Añadir al layout principal
        add(topPanel, BorderLayout.NORTH);
        add(new JBScrollPane(table), BorderLayout.CENTER);
    }


    public void refreshData(List<ErrorsTableEntry> data) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (ErrorsTableEntry entry : data) {
                tableModel.addRow(new Object[]{
                        entry.className,
                        entry.errorType,
                        entry.lineNumber,
                        entry.lineContent,
                        entry.occurrences
                });
            }
        });
    }

    public static class ErrorsTableEntry {
        public final String className;
        public final String errorType;
        public final int lineNumber;
        public final String lineContent;
        public int occurrences;

        public ErrorsTableEntry(String className, String errorType, int lineNumber, String lineContent, int occurrences) {
            this.className = className;
            this.errorType = errorType;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.occurrences = occurrences;
        }
    }
}