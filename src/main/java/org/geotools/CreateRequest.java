package org.geotools;

import org.geotools.swing.JMapFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class CreateRequest extends AbstractFrame {
    private JButton chooseAll,
            deleteSelectedRows,
            addParams,
            clearChoosenParams,
            createRequest;
    private JTable AllParamsTable,
            ChoosenParamsTable;
    private JScrollPane AllParamsTableScrollPane,
            ChoosenParamsTableScrollPane;
    public JFrame CRframe, mapFrame;
    public Set<String> stantionss;
    private boolean yearFlag, mounthFlag, dayFlag, srokFlag;
    private String years, mounths, days, sroks;
    private JPanel buttonsPanel, timePanel;
    private JLabel year, yearFrom, yearTo,
            mounth, mounthFrom, mounthTo,
            day, dayFrom, dayTo,
            srok, srokFrom, srokTo;
    private JPanel yearPeriod, mounthPeriod, dayPeriod, srokPeriod;
    private JTextField yearFromTxt, yearToTxt,
            mounthFromTxt, mounthToTxt,
            dayFromTxt, dayToTxt,
            srokFromTxt, srokToTxt;

    public CreateRequest(Set<String> stantions, JMapFrame MapFrame) throws IOException {
        CRframe = new JFrame("Создание запроса");
        CRframe.setPreferredSize(new Dimension(1075, 560));
        CRframe.setVisible(true);
        stantionss = stantions;
        mapFrame = MapFrame;
        renderTables();
        renderButtons();
        setLayout();
        CRframe.setLocationRelativeTo(null);
    }

    public void renderTables() {
        AllParamsTable = new JTable() {
            @Override
            public boolean isCellEditable(int arg0, int arg1) {
                return false;
            }
        };
        ChoosenParamsTable = new JTable() {
            @Override
            public boolean isCellEditable(int arg0, int arg1) {
                return false;
            }
        };
        AllParamsTableScrollPane = new JScrollPane(AllParamsTable);
        ChoosenParamsTableScrollPane = new JScrollPane(ChoosenParamsTable);
        DefaultTableModel AllParamsTableModel = (DefaultTableModel) AllParamsTable.getModel();
        DefaultTableModel ChoosenParamsTableModel = (DefaultTableModel) ChoosenParamsTable.getModel();
        AllParamsTableModel.addColumn("Параметр");
        AllParamsTableModel.addColumn("Параметры для выбора");
        ChoosenParamsTableModel.addColumn("Параметр");
        ChoosenParamsTableModel.addColumn("Параметры запроса");
        setTableColumn(AllParamsTable, AllParamsTableModel);
        setTableColumn(ChoosenParamsTable, ChoosenParamsTableModel);
        fillTable(AllParamsTableModel);
        createTimePanel();
        CRframe.add(AllParamsTableScrollPane);
        CRframe.add(ChoosenParamsTableScrollPane);
        CRframe.add(timePanel);
        CRframe.pack();
        CRframe.addWindowListener(new WindowListener() {
            public void windowClosing(WindowEvent event) {
                CRframe.setVisible(false);
                mapFrame.setEnabled(true);
                mapFrame.setVisible(true);
            }

            public void windowIconified(WindowEvent event) {
            }

            public void windowActivated(WindowEvent event) {
            }

            public void windowClosed(WindowEvent event) {
            }

            public void windowDeactivated(WindowEvent event) {
            }

            public void windowOpened(WindowEvent event) {
            }

            public void windowDeiconified(WindowEvent event) {
            }
        });
    }

    private void createTimePanel() {
        timePanel = new JPanel(new GridLayout(12, 1));
        JLabel header = new JLabel("Временные параметры");
        header.setHorizontalAlignment(JLabel.CENTER);
        JSeparator sep = new JSeparator();
        timePanel.add(sep);
        timePanel.add(header);

        year = new JLabel("Год");
        year.setHorizontalAlignment(JLabel.CENTER);
        timePanel.add(year);
        yearPeriod = new JPanel(new GridLayout(1, 4));
        yearFrom = new JLabel("От"); yearFrom.setHorizontalAlignment(JLabel.CENTER);
        yearFromTxt = new JTextField(); yearTo = new JLabel("До");
        yearTo.setHorizontalAlignment(JLabel.CENTER); yearToTxt = new JTextField(); yearPeriod.add(yearFrom); yearPeriod.add(yearFromTxt);
        yearPeriod.add(yearTo); yearPeriod.add(yearToTxt); timePanel.add(yearPeriod);

        mounth = new JLabel("Месяц");
        mounth.setHorizontalAlignment(JLabel.CENTER);
        timePanel.add(mounth);
        mounthPeriod = new JPanel(new GridLayout(1, 4));
        mounthFrom = new JLabel("От"); mounthFrom.setHorizontalAlignment(JLabel.CENTER);
        mounthFromTxt = new JTextField(); mounthTo = new JLabel("До"); mounthTo.setHorizontalAlignment(JLabel.CENTER);
        mounthToTxt = new JTextField(); mounthPeriod.add(mounthFrom); mounthPeriod.add(mounthFromTxt);
        mounthPeriod.add(mounthTo); mounthPeriod.add(mounthToTxt); timePanel.add(mounthPeriod);

        day = new JLabel("День");
        day.setHorizontalAlignment(JLabel.CENTER);
        timePanel.add(day);
        dayPeriod = new JPanel(new GridLayout(1, 4));
        dayFrom = new JLabel("От"); dayFrom.setHorizontalAlignment(JLabel.CENTER);
        dayFromTxt = new JTextField(); dayTo = new JLabel("До"); dayTo.setHorizontalAlignment(JLabel.CENTER);
        dayToTxt = new JTextField(); dayPeriod.add(dayFrom); dayPeriod.add(dayFromTxt);
        dayPeriod.add(dayTo); dayPeriod.add(dayToTxt); timePanel.add(dayPeriod);

        srok = new JLabel("Срок");
        srok.setHorizontalAlignment(JLabel.CENTER);
        timePanel.add(srok);
        srokPeriod = new JPanel(new GridLayout(1, 4));
        srokFrom = new JLabel("От"); srokFrom.setHorizontalAlignment(JLabel.CENTER);
        srokFromTxt = new JTextField(); srokTo = new JLabel("До"); srokTo.setHorizontalAlignment(JLabel.CENTER);
        srokToTxt = new JTextField(); srokPeriod.add(srokFrom); srokPeriod.add(srokFromTxt);
        srokPeriod.add(srokTo); srokPeriod.add(srokToTxt); timePanel.add(srokPeriod);
        JSeparator sep4 = new JSeparator(); timePanel.add(sep4);
    }

    private void getTimePeriods() {
        if (!yearFromTxt.getText().equals("") && !yearToTxt.getText().equals("")) {
            int yearFromVal = Integer.parseInt(yearFromTxt.getText());
            int yearToVal = Integer.parseInt(yearToTxt.getText());
            years = getStepsInPeriod(yearFromVal, yearToVal);
            yearFlag = true;
        } else yearFlag = false;

        if (!mounthFromTxt.getText().equals("") && !mounthToTxt.getText().equals("")) {
            int mounthFromVal = Integer.parseInt(mounthFromTxt.getText());
            int mounthToVal = Integer.parseInt(mounthToTxt.getText());
            mounths = getStepsInPeriod(mounthFromVal, mounthToVal);
            mounthFlag = true;
        } else mounthFlag = false;

        if (!dayFromTxt.getText().equals("") && !dayToTxt.getText().equals("")) {
            int dayFromVal = Integer.parseInt(dayFromTxt.getText());
            int dayToVal = Integer.parseInt(dayToTxt.getText());
            days = getStepsInPeriod(dayFromVal, dayToVal);
            dayFlag = true;
        } else dayFlag = false;

        if (!srokFromTxt.getText().equals("") && !srokToTxt.getText().equals("")) {
            int srokFromVal = Integer.parseInt(srokFromTxt.getText());
            int srokToVal = Integer.parseInt(srokToTxt.getText());
            sroks = getStepsInPeriod(srokFromVal, srokToVal);
            srokFlag = true;
        } else srokFlag = false;
    }

    private String getStepsInPeriod(int from, int to) {
            Set<String> period = new HashSet<String>();
            int len = to - from;
            int step = 0;
            while (step < len + 1) {
                period.add(String.valueOf(from + step));
                step ++;
            }
            String periodString = "";
            for (String s : period) {
                periodString += s + ", ";
            }
            int commaIndex = periodString.lastIndexOf(",");
            periodString = periodString.substring(0, commaIndex);
            return periodString;
    }

    public void setTableColumn(JTable tableName, DefaultTableModel tableModel) {
        tableName.setAutoResizeMode(tableName.AUTO_RESIZE_OFF);
        RowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(tableModel);
        tableName.setRowSorter(sorter);
        TableColumn index = tableName.getColumnModel().getColumn(0);
        index.setMinWidth(0);
        index.setMaxWidth(0);
        TableColumn paramN = tableName.getColumnModel().getColumn(1);
        paramN.setMinWidth(400);
        paramN.setMaxWidth(400);
        tableName.setRowHeight(20);
    }

    public void fillTable(DefaultTableModel tableModel) {
        Map params = new HashMap<String, String>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(System.getProperty("user.dir") + "\\src\\main\\data\\Sro8c_All.aql"), "Cp1251"));
            String line = reader.readLine();
            while (line != null) {
                line = line.replace("/", " ")
                        .trim()
                        .replaceAll("\\d+|FC|\\)|\\(|,", "")
                        .replaceAll("\\s+", " ")
                        .replaceFirst(" ", ";");
                if (!line.isEmpty()) {
                    if (!line.contains(".Q")
                            && !line.contains(".D")
                            && !line.contains("Гринвич")
                            && !line.startsWith("IF")
                            && !line.startsWith("WA")
                            && !line.startsWith("IN")
                            && !line.startsWith("PORTION")
                            && !line.startsWith("END")
                            && !line.startsWith("RECORD")) {
                        Object[] paramNames = line.split(";");
                        tableModel.addRow(paramNames);
                        params.put(paramNames[0], paramNames[1]);
                        Object paramName = paramNames[1];
                    }
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void renderButtons() {
        buttonsPanel = new JPanel(new GridLayout(5, 1));
        addButton(addParams, new CreateRequest.addParamsHandler(), "Добавить к запросу", buttonsPanel);
        addButton(chooseAll, new CreateRequest.chooseAllHandler(), "Выбрать всё", buttonsPanel);
        addButton(deleteSelectedRows, new CreateRequest.deleteSelectedRowsHandler(), "Удалить", buttonsPanel);
        addButton(clearChoosenParams, new CreateRequest.clearChoosenParamsHandler(), "Очистить", buttonsPanel);
        createRequest = new JButton("Сделать запрос");
        createRequest.addActionListener(new CreateRequest.createRequestHandler());
        buttonsPanel.add(createRequest);
        createRequest.setEnabled(false);
    }

    public void setLayout() {
        Container contentPane = CRframe.getContentPane();
        GroupLayout layout = new GroupLayout(contentPane);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        contentPane.setLayout(layout);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(AllParamsTableScrollPane, 419, GroupLayout.DEFAULT_SIZE, 419)
                .addGroup(layout.createParallelGroup()
                        .addComponent(buttonsPanel, 200, GroupLayout.DEFAULT_SIZE, 200)
                        .addComponent(timePanel, 200, GroupLayout.DEFAULT_SIZE, 200))
                .addComponent(ChoosenParamsTableScrollPane, 419, GroupLayout.DEFAULT_SIZE, 419));
        layout.setVerticalGroup(layout.createParallelGroup()
                .addComponent(AllParamsTableScrollPane, 500, GroupLayout.DEFAULT_SIZE, 600)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonsPanel, 130, GroupLayout.DEFAULT_SIZE, 200)
                        .addComponent(timePanel, 300, GroupLayout.DEFAULT_SIZE, 419))
                .addComponent(ChoosenParamsTableScrollPane, 500, GroupLayout.DEFAULT_SIZE, 600));
    }

    private int getLineNumber() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(System.getProperty("user.dir") + "\\src\\main\\data\\Sro8c_All.aql"), "Cp1251"));
        String line = reader.readLine();
        int linesnum = 1;
        while (line != null) {
            line = reader.readLine();
            linesnum++;
            if (line.contains(" ГОД ")) {
                break;
            }
        }
        return linesnum;
    }

    private class chooseAllHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            AllParamsTable.selectAll();
        }
    }

    private class addParamsHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            TableModel AllParamsTableModel = AllParamsTable.getModel();
            DefaultTableModel ChoosenParamsTableModel = (DefaultTableModel) ChoosenParamsTable.getModel();
            int[] indexs = AllParamsTable.getSelectedRows();
            Object[] row = new Object[2];
            for (int index : indexs) {
                for (int j = 0; j < 2; j++) row[j] = AllParamsTableModel.getValueAt(index, j);
                ChoosenParamsTableModel.addRow(row);
            }
            AllParamsTable.clearSelection();
            checkTableRowsCount(ChoosenParamsTable, createRequest);
        }
    }

    private class deleteSelectedRowsHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) ChoosenParamsTable.getModel();
            int[] indexs = ChoosenParamsTable.getSelectedRows();
            for (int i = 0; i < indexs.length; i++) {
                model.removeRow(indexs[i] - i);
            }
            AllParamsTable.clearSelection();
            checkTableRowsCount(ChoosenParamsTable, createRequest);
        }
    }

    private class clearChoosenParamsHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) ChoosenParamsTable.getModel();
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }
            AllParamsTable.clearSelection();
            checkTableRowsCount(ChoosenParamsTable, createRequest);
        }
    }

    private class createRequestHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            getTimePeriods();
            DefaultTableModel model = (DefaultTableModel) ChoosenParamsTable.getModel();
            List<String> params = new ArrayList<String>();
            for (int i = 0; i < ChoosenParamsTable.getRowCount(); i++) {
                params.add((String) ChoosenParamsTable.getValueAt(i, 0));
            }
            String stnString = "";
            for (String s : stantionss) {
                stnString += s + ", ";
            }
            int commaIndex = stnString.lastIndexOf(",");
            stnString = stnString.substring(0, commaIndex);
            try {
                Path path = Paths.get(System.getProperty("user.dir"), "\\src\\main\\data\\Sro8c_All_NEW.aql");
                Charset charset = Charset.forName("Cp1251");
                List<String> lines = Files.readAllLines(path, charset);
                FileWriter writer = new FileWriter("src\\main\\query\\query.aql", false);
                int linenum = getLineNumber();
                for (int i = 0; i <= lines.size() - 1; i++) {
                    if (i < linenum - 1) {
                        if (lines.get(i).contains("WA1 = СТАНЦИЯ")) {
                            String str = lines.get(i);
                            str = str.replace("СТАНЦИЯ", stnString);
                            writer.write(str);
                            writer.write("\r\n");
                            continue;
                        }
                        if (lines.get(i).contains("ГОДГР             FC(4)") && yearFlag) {
                            String str = lines.get(i);
                            str = str.replace("ГОДГР", years);
                            writer.write(str);
                            writer.write("\r\n");
                            continue;
                        }
                        if (lines.get(i).contains("МЕСЯЦГР           FC(2)") && mounthFlag) {
                            String str = lines.get(i);
                            str = str.replace("МЕСЯЦГР", mounths);
                            writer.write(str);
                            writer.write("\r\n");
                            continue;
                        }
                        if (lines.get(i).contains("ДЕНЬГР            FC(2)") && dayFlag) {
                            String str = lines.get(i);
                            str = str.replace("ДЕНЬГР", days);
                            writer.write(str);
                            writer.write("\r\n");
                            continue;
                        }
                        if (lines.get(i).contains("СРОКГР            FC(2)") && srokFlag) {
                            String str = lines.get(i);
                            str = str.replace("СРОКГР", sroks);
                            writer.write(str);
                            writer.write("\r\n");
                            continue;
                        }
                        writer.write(lines.get(i));
                        writer.write("\r\n");
                        continue;
                    }
                    for (int j = 0; j <= params.size() - 1; j++) {
                        String par = params.get(j);
                        String line = lines.get(i);
                        if (!line.contains(" " + par)) {
                            continue;
                        }
                        writer.write(lines.get(i) + "\n");
                    }
                    if (lines.get(i).contains("   END НАБЛСРОК;")) {
                        writer.write(lines.get(i));
                        writer.write("\r\n");
                        writer.flush();
                    }
                }
                stantionss.clear();
            } catch (FileNotFoundException ee) {
                ee.printStackTrace();
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
    }
}

