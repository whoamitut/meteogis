package org.geotools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.geotools.data.*;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.*;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RenderComponents extends AbstractFrame {

    public JTable AllStantionsTable,
            ChoosenStantionsTable;
    public JPanel middleButtonsPane,
            downButtonsPane;
    public JScrollPane AllStantionsTableScrollPane,
            ChoosenStantionsTableScrollPane;
    public JLabel choosenCountTextStart,
            choosenCountDown,
            choosenCountTextMiddle,
            choosenCountUp,
            choosenCountTextStart1,
            chooseForRequestRowsCount;
    public JButton chooseAll,
            clearSelectedRows,
            addStantions,
            clearChoosenStantions,
            deleteStantions,
            submitChooseStantions;
    public DefaultFeatureCollection stnCollection;
    public GeometryFactory stantionsGeometryFactory;
    public SimpleFeatureBuilder stantionsFeatureBuilder;
    public SimpleFeatureType stantionsType = DataUtilities.createType(
            "Location",
            "the_geom:Point:srid=4326," + "s_index:Integer," + "name:String," + "ugms:Integer," + "latitude:Double,"
                    + "longitude:Double," + "height:Integer," + "obl:String"
    );
    private String[] columnNames = {"Индекс", "Станция", "УГМС", "Широта", "Долгота", "Высота", "Республика, область", "FLAG"};
    public JMapFrame mapFrame;
    public MapContent map;
    public JPanel tablePanel;
    public Set<String> stantions = new HashSet<String>();
    public Layer oblLayer;
    public Layer ugmsLayer;
    private Layer stantionsLayer;
    private SimpleFeature featureStn;
    private FeatureSource featureSourceOBL;
    private FeatureSource featureSourceUGMS;
    private CursorTool myCursorTool;
    public String layerFlag = "ugms";

    public RenderComponents() throws HeadlessException, SchemaException {
        try {
            stnCollection = new DefaultFeatureCollection();
            renderMaps();
            renderMenu();
        } catch (IOException ex) {
            Logger.getLogger(RenderComponents.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void renderMaps() throws IOException {
        map = new MapContent();
        File country = new File(System.getProperty("user.dir") + "\\src\\main\\data\\maps2", "Country.shp");
        File ugms = new File(System.getProperty("user.dir") + "\\src\\main\\data\\maps2", "ugms.shp");
        File obl = new File(System.getProperty("user.dir") + "\\src\\main\\data\\maps2", "obl.shp");
        FileDataStore storeUGMS = FileDataStoreFinder.getDataStore(ugms);
        featureSourceUGMS = storeUGMS.getFeatureSource();
        Style styleUGMS = StyleLab.createStyle(ugms, featureSourceUGMS);
        ugmsLayer = new FeatureLayer(featureSourceUGMS, styleUGMS);
        FileDataStore storeOBL = FileDataStoreFinder.getDataStore(obl);
        featureSourceOBL = storeOBL.getFeatureSource();
        Style styleOBL = StyleLab.createStyle(obl, featureSourceOBL);
        oblLayer = new FeatureLayer(featureSourceOBL, styleOBL);
        Layer countryLayer = StyleLab.createLayer(country, Color.LIGHT_GRAY, 0.7);
        map.addLayer(countryLayer);
        map.addLayer(ugmsLayer);
        map.addLayer(oblLayer);
        oblLayer.setVisible(false);
        renderLeftPanel(map);
        AllStantionsTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                showStantions();
            }
        });

        myCursorTool = new CursorTool() {
            @Override
            public void onMouseClicked(MapMouseEvent ev) {
                try {
                    if (layerFlag == "ugms") getID(featureSourceUGMS, ev);
                    if (layerFlag == "obl") getID(featureSourceOBL, ev);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    mapFrame.getMapPane().setCursorTool(myCursorTool);
    }

    private void getID(FeatureSource fs, MapMouseEvent ev) throws IOException {
        GeometryDescriptor geomDesc = fs.getSchema().getGeometryDescriptor();
        String geometryAttributeName = geomDesc.getLocalName();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Point screenPos = ev.getPoint();
        Rectangle screenRect = new Rectangle(screenPos.x - 2, screenPos.y - 2, 5, 5);
        AffineTransform screenToWorld = mapFrame.getMapPane().getScreenToWorldTransform();
        Rectangle2D worldRect = screenToWorld.createTransformedShape(screenRect).getBounds2D();
        ReferencedEnvelope bbox = new ReferencedEnvelope(worldRect, mapFrame.getMapContent().getCoordinateReferenceSystem());
        Filter filter = ff.intersects(ff.property(geometryAttributeName), ff.literal(bbox));
        FeatureCollection selectedFeatures = null;
        try {
            selectedFeatures = fs.getFeatures(filter);
            FeatureIterator iter = selectedFeatures.features();
            Feature feature = iter.next();
            if (layerFlag == "ugms") {
                String ugms = String.valueOf(feature.getProperty("UGMS"));
                ugms = ugms.split("=")[2];
                mapUgms2table(ugms);
            }
            if (layerFlag == "obl") {
                String name = String.valueOf(feature.getProperty("NAME"));
                name = new String(name.getBytes("cp1252"), "cp1251");
                name = name.split("=")[2];
                mapObl2table(name);
            }
        } catch (NoSuchElementException e) {
        }
    }

    private void mapObl2table(String obl) {
        Set<Integer> rows = new HashSet<>();
        for (int i = 0; i < AllStantionsTable.getRowCount(); i++) {
            for (int j = 0; j < columnNames.length; j++) {
                if (AllStantionsTable.getValueAt(i, 6).equals(obl)) {
                    rows.add(i);
                }
            }
        }
        for (int i : rows) {
            AllStantionsTable.addRowSelectionInterval(i, i);
        }
        showStantions();
    }

    private void mapUgms2table(String u) {
        int ugms = Integer.parseInt(u);
        Set<Integer> rows = new HashSet<>();
        for (int i = 0; i < AllStantionsTable.getRowCount(); i++) {
            for (int j = 0; j < columnNames.length; j++) {
                if (Integer.parseInt(AllStantionsTable.getValueAt(i, 2).toString()) == ugms) {
                    rows.add(i);
                }
            }
        }
        for (int i : rows) {
            AllStantionsTable.addRowSelectionInterval(i, i);
        }
        showStantions();
    }

    private void showStantions() {
        List<SimpleFeature> features = new ArrayList<>();
        stantionsGeometryFactory = JTSFactoryFinder.getGeometryFactory();
        Style style = SLD.createPointStyle("Circle", Color.BLACK, Color.BLACK, 1, 5);
        TableModel AllStantionsTableModel = AllStantionsTable.getModel();
        int[] indexs = AllStantionsTable.getSelectedRows();
        Object[] row = new Object[columnNames.length];
        stnCollection.clear();
        if (stnCollection.isEmpty()) {
            stantionsFeatureBuilder = new SimpleFeatureBuilder(stantionsType);
            for (int index : indexs) {
                String rowww = "";
                for (int j = 0; j < columnNames.length; j++) {
                    row[j] = AllStantionsTableModel.getValueAt(index, j).toString();
                    rowww += row[j];
                    rowww += ";";
                }
                String col[] = rowww.split(";");
                int s_index = Integer.parseInt(col[0]);
                String name = col[1].trim();
                int ugms = Integer.parseInt(col[2]);
                double latitude = Double.parseDouble(col[3]);
                double longitude = Double.parseDouble(col[4]);
                int height = Integer.parseInt(col[5]);
                String obl = col[6].trim();
                com.vividsolutions.jts.geom.Point point = stantionsGeometryFactory.createPoint(new Coordinate(latitude, longitude));
                stantionsFeatureBuilder.add(point);
                stantionsFeatureBuilder.add(s_index);
                stantionsFeatureBuilder.add(name);
                stantionsFeatureBuilder.add(ugms);
                stantionsFeatureBuilder.add(latitude);
                stantionsFeatureBuilder.add(longitude);
                stantionsFeatureBuilder.add(height);
                stantionsFeatureBuilder.add(obl);
                featureStn = stantionsFeatureBuilder.buildFeature(null);
                features.add(featureStn);
                stnCollection.add(featureStn);
            }
            stantionsLayer = new FeatureLayer(stnCollection, style);
            map.addLayer(stantionsLayer);
        }
    }

    private void renderLeftPanel(MapContent map) {
        tablePanel = new JPanel(new GridBagLayout());
        renderButtons();
        renderTables();
        setLayout();
        mapFrame = new JMapFrame(map);
        mapFrame.add(tablePanel, BorderLayout.WEST);
        mapFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        mapFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mapFrame.enableTool(JMapFrame.Tool.RESET, JMapFrame.Tool.SCROLLWHEEL);
        JToolBar toolBar = mapFrame.getToolBar();
        JButton changeLayer = new JButton("Переключить слой");
        toolBar.addSeparator();
        toolBar.add(changeLayer);
        JLabel currentLayer = new JLabel("  Текущий слой: УГМС");
        toolBar.add(currentLayer);
        JMenuBar menuBar = new JMenuBar();
        mapFrame.setJMenuBar(menuBar);
        mapFrame.setVisible(true);

        try {
            changeLayer.addActionListener(e -> {
                        if (layerFlag == "ugms") {
                            oblLayer.setVisible(true);
                            ugmsLayer.setVisible(false);
                            layerFlag = "obl";
                            currentLayer.setText("  Текущий слой: СУБЪЕКТЫ РФ");
                            reRender();
                        } else if (layerFlag == "obl") {
                            ugmsLayer.setVisible(true);
                            oblLayer.setVisible(false);
                            layerFlag = "ugms";
                            currentLayer.setText("  Текущий слой: УГМС");
                            reRender();
                        }
                    }
            );
        } catch (NullPointerException e) {
        }
    }

    public void setLayout() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        tablePanel.add(AllStantionsTableScrollPane, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        tablePanel.add(middleButtonsPane, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.weighty = 0;
        tablePanel.add(ChoosenStantionsTableScrollPane, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        c.weighty = 0;
        tablePanel.add(downButtonsPane, c);
    }

    public void renderTables() {
        AllStantionsTable = new JTable() {
            @Override
            public boolean isCellEditable(int arg0, int arg1) {
                return false;
            }
        };
        ChoosenStantionsTable = new JTable() {
            @Override
            public boolean isCellEditable(int arg0, int arg1) {
                return false;
            }
        };
        Dimension sSize = Toolkit.getDefaultToolkit ().getScreenSize ();
        int height1 = (int) Math.round(sSize.height / 2);
        int height2 = (int) Math.round(sSize.height / 5);
        AllStantionsTable.setPreferredScrollableViewportSize(new Dimension(450, height1));
        ChoosenStantionsTable.setPreferredScrollableViewportSize(new Dimension(450, height2));
        AllStantionsTableScrollPane = new JScrollPane(AllStantionsTable);
        ChoosenStantionsTableScrollPane = new JScrollPane(ChoosenStantionsTable);
        DefaultTableModel AllStantionsTableModel = (DefaultTableModel) AllStantionsTable.getModel();
        DefaultTableModel ChoosenStantionsTableModel = (DefaultTableModel) ChoosenStantionsTable.getModel();
        setTableColumn(AllStantionsTable, AllStantionsTableModel);
        setTableColumn(ChoosenStantionsTable, ChoosenStantionsTableModel);
        fillTable(AllStantionsTableModel);
    }

    public void fillTable(DefaultTableModel tableModel) {
        FileInputStream fis = null;
        DbaseFileReader dbfReader = null;
        try {
            fis = new FileInputStream(System.getProperty("user.dir") + "\\src\\main\\data\\maps2\\all_stabtions.dbf");
            dbfReader = new DbaseFileReader(fis.getChannel(), false, Charset.forName("Cp1251"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DbaseFileHeader dbfHeader = dbfReader.getHeader();
        Vector<Object[]> rowsList = new Vector<>();
        while (dbfReader.hasNext()) {
            try {
                rowsList.add(dbfReader.readEntry());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int numOfCol = dbfHeader.getNumFields();
        Object[][] dataRows = new Object[rowsList.size()][numOfCol];
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < rowsList.size(); i++) {
            dataRows[i] = rowsList.get(i);
            tableModel.addRow(dataRows[i]);
        }
        int rowCount = AllStantionsTable.getRowCount();
        String b = String.valueOf(rowCount);
        choosenCountUp = new JLabel();
        choosenCountUp.setText(b);
        middleButtonsPane.add(choosenCountUp);
        int rowsCount = tableModel.getRowCount();
        for (int i = 0; i < rowsCount; i++) {
            dataRows[i] = rowsList.get(i);
            tableModel.setValueAt(0, i, 7);
        }
        ListSelectionModel selModel = AllStantionsTable.getSelectionModel();
        selModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int count = AllStantionsTable.getSelectedRows().length;
                String c = String.valueOf(count);
                choosenCountDown.setText(c);
            }
        });
    }

    public void setTableColumn(JTable tableName, DefaultTableModel tableModel) {
        tableName.setAutoResizeMode(tableName.AUTO_RESIZE_OFF);
        tableName.getTableHeader().setReorderingAllowed(false);
        for (String columnName : columnNames) {
            tableModel.addColumn(columnName);
        }
        TableColumn index = tableName.getColumnModel().getColumn(0);
        index.setMinWidth(60);
        index.setMaxWidth(60);
        TableColumn station = tableName.getColumnModel().getColumn(1);
        station.setMinWidth(140);
        station.setMaxWidth(140);
        TableColumn ugms = tableName.getColumnModel().getColumn(2);
        ugms.setMinWidth(50);
        ugms.setMaxWidth(50);
        TableColumn latitude = tableName.getColumnModel().getColumn(3);
        latitude.setMinWidth(70);
        latitude.setMaxWidth(70);
        TableColumn longitude = tableName.getColumnModel().getColumn(4);
        longitude.setMinWidth(70);
        longitude.setMaxWidth(70);
        TableColumn height = tableName.getColumnModel().getColumn(5);
        height.setMinWidth(70);
        height.setMaxWidth(70);
        TableColumn region = tableName.getColumnModel().getColumn(6);
        region.setMinWidth(200);
        region.setMaxWidth(200);
        TableColumn flag = tableName.getColumnModel().getColumn(7);
        flag.setMinWidth(0);
        flag.setMaxWidth(0);
        RowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(tableModel);
        tableName.setRowSorter(sorter);
    }

    public void renderButtons() {
        middleButtonsPane = new JPanel();
        downButtonsPane = new JPanel();
        addButton(chooseAll, new RenderComponents.ChooseAllStantions(), "Выбрать всё", middleButtonsPane);
        addButton(clearSelectedRows, new RenderComponents.clearSelectedStantions(), "Снять выделение", middleButtonsPane);
        addButton(addStantions, new RenderComponents.addStantionsToList(), "Добавить в выборку", middleButtonsPane);
        addButton(clearChoosenStantions, new RenderComponents.clearChoosenStantionsHandler(), "Очистить выборку", downButtonsPane);
        addButton(deleteStantions, new RenderComponents.deleteStantionsHandler(), "Удалить", downButtonsPane);
        submitChooseStantions = new JButton("Подтвердить");
        submitChooseStantions.addActionListener(new RenderComponents.submitChooseStantionsHandler());
        downButtonsPane.add(submitChooseStantions);
        submitChooseStantions.setEnabled(false);
        renderLabels();
    }

    private void renderLabels() {
        choosenCountTextStart = new JLabel("Выбрано ");
        choosenCountDown = new JLabel("0");
        choosenCountTextMiddle = new JLabel(" из ");
        choosenCountTextStart1 = new JLabel("Выбрано для запроса ");
        chooseForRequestRowsCount = new JLabel("0");
        middleButtonsPane.add(choosenCountTextStart);
        middleButtonsPane.add(choosenCountDown);
        middleButtonsPane.add(choosenCountTextMiddle);
        downButtonsPane.add(choosenCountTextStart1);
        downButtonsPane.add(chooseForRequestRowsCount);
    }

    private void renderMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("Файл");
        JMenuItem exit = new JMenuItem(new ExitAction());
        exit.setText("Выйти");
        file.add(exit);
        menuBar.add(file);
        mapFrame.setJMenuBar(menuBar);
    }

    private void reRender() {
        DefaultFeatureCollection fakeCollection = new DefaultFeatureCollection();
        stantionsFeatureBuilder = new SimpleFeatureBuilder(stantionsType);

        try {
            com.vividsolutions.jts.geom.Point point = stantionsGeometryFactory.createPoint(new Coordinate(0, 0));
            stantionsFeatureBuilder.add(point);
        } catch (NullPointerException e) {
        }
        SimpleFeature feature = stantionsFeatureBuilder.buildFeature(null);
        fakeCollection.add(feature);
        Style style = SLD.createPointStyle("Circle", Color.BLACK, Color.BLACK, 0, 0);
        Layer fakeLayer = new FeatureLayer(fakeCollection, style);
        map.addLayer(fakeLayer);
    }

    private class ChooseAllStantions implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            AllStantionsTable.selectAll();
            showStantions();
        }
    }

    private class clearSelectedStantions implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            AllStantionsTable.clearSelection();
            stnCollection.clear();
            reRender();
        }
    }

    private class addStantionsToList implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            TableModel AllStantionsTableModel = AllStantionsTable.getModel();
            DefaultTableModel ChoosenStantionsTableModel = (DefaultTableModel) ChoosenStantionsTable.getModel();
            int[] indexs = AllStantionsTable.getSelectedRows();
            Object[] row = new Object[columnNames.length];
            for (int index : indexs) {
                for (int j = 0; j < columnNames.length; j++) row[j] = AllStantionsTableModel.getValueAt(index, j);
                ChoosenStantionsTableModel.addRow(row);
                AllStantionsTableModel.setValueAt(1, index, 7);
            }
            AllStantionsTable.clearSelection();
            int chooseForRequestRows = ChoosenStantionsTableModel.getRowCount();
            String c = String.valueOf(chooseForRequestRows);
            chooseForRequestRowsCount.setText(c);
            checkTableRowsCount(ChoosenStantionsTable, submitChooseStantions);
            stnCollection.clear();
            reRender();
        }
    }

    private class clearChoosenStantionsHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) ChoosenStantionsTable.getModel();
            while (model.getRowCount() > 0) {
                model.removeRow(0);
            }
            int chooseForRequestRows = ChoosenStantionsTable.getRowCount();
            String c = String.valueOf(chooseForRequestRows);
            chooseForRequestRowsCount.setText(c);
            checkTableRowsCount(ChoosenStantionsTable, submitChooseStantions);
        }
    }

    private class deleteStantionsHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) ChoosenStantionsTable.getModel();
            int[] indexs = ChoosenStantionsTable.getSelectedRows();
            for (int i = 0; i < indexs.length; i++) {
                model.removeRow(indexs[i] - i);
            }
            int chooseForRequestRows = ChoosenStantionsTable.getRowCount();
            String c = String.valueOf(chooseForRequestRows);
            chooseForRequestRowsCount.setText(c);
            checkTableRowsCount(ChoosenStantionsTable, submitChooseStantions);
        }
    }

    private class submitChooseStantionsHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = (DefaultTableModel) ChoosenStantionsTable.getModel();
            int lenght = ChoosenStantionsTable.getRowCount();
            for (int i = 0; i < lenght; i++) {
                String stn = String.valueOf(model.getValueAt(i, 0));
                stantions.add(stn);
            }
            try {
                CreateRequest cr = new CreateRequest(stantions, mapFrame);
                mapFrame.setEnabled(false);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private class ExitAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    public static void main(String args[]) throws IOException {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    RenderComponents MainFrame = new RenderComponents();
                } catch (SchemaException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
