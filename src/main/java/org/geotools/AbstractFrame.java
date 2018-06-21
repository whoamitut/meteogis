package org.geotools;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionListener;

public abstract class AbstractFrame {

    public abstract void renderTables();

    public abstract void setTableColumn(JTable tableName, DefaultTableModel tableModel);

    public abstract void fillTable(DefaultTableModel tableModel);

    public abstract void renderButtons();

    public abstract void setLayout();

    public void checkTableRowsCount(JTable tableName, JButton button) {
        if (tableName.getRowCount() != 0) {
            button.setEnabled(true);
        } else {
            button.setEnabled(false);
        }
    }

    public void addButton(JButton button, ActionListener handler, String text, JPanel panel) {
        button = new JButton(text);
        button.addActionListener(handler);
        panel.add(button);
    }

}

