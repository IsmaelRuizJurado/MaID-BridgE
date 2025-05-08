package com.example.maidbridge.monitoring;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Map;
import java.util.regex.Pattern;

public class ErrorTablePanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final TableRowSorter<TableModel> sorter;

    public ErrorTablePanel() {
        super(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{"Class", "Errors"}, 0);
        JBTable table = new JBTable(tableModel);
        table.getEmptyText().setText("Waiting for error analysis...");
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

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

        add(searchField, BorderLayout.NORTH);
        add(new JBScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData(Map<String, Integer> errorCountByClass) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            errorCountByClass.forEach((className, count) ->
                    tableModel.addRow(new Object[]{className, count})
            );
        });
    }
}

