package com.example.maidbridge.monitoring;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TotalErrorsTablePanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final TableRowSorter<TableModel> sorter;

    public TotalErrorsTablePanel() {
        super(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Class", "Errors"}, 0);
        JBTable table = new JBTable(tableModel);
        table.getEmptyText().setText("Waiting for error analysis...");
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Orden inicial: errores descendente, clases ascendente
        sorter.setComparator(0, Comparator.naturalOrder());
        sorter.setComparator(1, Comparator.naturalOrder());
        sorter.setSortKeys(List.of(
                new RowSorter.SortKey(1, SortOrder.DESCENDING),
                new RowSorter.SortKey(0, SortOrder.ASCENDING)
        ));
        sorter.sort();

        // Desactivar orden por clic
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Desactiva la ordenación al hacer clic
                e.consume();
                sorter.setSortKeys(List.of(
                        new RowSorter.SortKey(1, SortOrder.DESCENDING),
                        new RowSorter.SortKey(0, SortOrder.ASCENDING)
                ));
            }
        });

        // Ajuste de proporción de columnas: 85% / 15%
        table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int totalWidth = table.getWidth();
                table.getColumnModel().getColumn(0).setPreferredWidth((int) (totalWidth * 0.85));
                table.getColumnModel().getColumn(1).setPreferredWidth(totalWidth - table.getColumnModel().getColumn(0).getPreferredWidth());
            }
        });

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);

        TableCellRenderer originalRenderer = table.getTableHeader().getDefaultRenderer();

        table.getTableHeader().setDefaultRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            Component comp = originalRenderer.getTableCellRendererComponent(table1, value, isSelected, hasFocus, row, column);

            if (comp instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }

            return comp;
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

        // Crear cabecera visual en negrita
        JLabel titleLabel = new JLabel("Total errors for each class ordered");
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

    public void refreshData(Map<String, Integer> errorCountByClass) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            errorCountByClass.forEach((className, count) ->
                    tableModel.addRow(new Object[]{className, count})
            );
            sorter.sort();
        });
    }
}
