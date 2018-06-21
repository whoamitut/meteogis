package org.geotools;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.*;
import org.geotools.swing.dialog.JExceptionReporter;
import org.geotools.swing.styling.JSimpleStyleDialog;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;

public class StyleLab {
    static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();

    public static Style createStyle(File file, FeatureSource featureSource) {
        File sld = toSLDFile(file);
        if (sld != null) {
            return createFromSLD(sld);
        }
        SimpleFeatureType schema = (SimpleFeatureType) featureSource.getSchema();
        return JSimpleStyleDialog.showDialog(null, schema);
    }

    public static File toSLDFile(File file) {
        String path = file.getAbsolutePath();
        String base = path.substring(0, path.length() - 4);
        String newPath = base + ".sld";
        File sld = new File(newPath);
        if (sld.exists()) {
            return sld;
        }
        newPath = base + ".SLD";
        sld = new File(newPath);
        if (sld.exists()) {
            return sld;
        }
        return null;
    }

    private static Style createFromSLD(File sld) {
        try {
            SLDParser stylereader = new SLDParser(styleFactory, sld.toURI().toURL());
            Style[] style = stylereader.readXML();
            return style[0];
        } catch (Exception e) {
            JExceptionReporter.showDialog(e, "Problem creating style");
        }
        return null;
    }

    public static Layer createPointLayer(File fileName) {
        StyleBuilder styleBuilder = new StyleBuilder();
        PointSymbolizerImpl pointSymb = (PointSymbolizerImpl) styleBuilder.createPointSymbolizer();
        FilterFactory filterFactory = new FilterFactoryImpl();
        pointSymb.getGraphic().setSize(filterFactory.literal(3));
        org.geotools.styling.Style style = styleBuilder.createStyle(pointSymb);
        FileDataStore store = null;
        try {
            store = FileDataStoreFinder.getDataStore(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SimpleFeatureSource featureSource = null;
        try {
            featureSource = store.getFeatureSource();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Layer layer = new FeatureLayer(featureSource, style);
        return layer;
    }

    public static Layer createLayer(File fileName, Color fillColor,
                                    double opasity) throws IOException {
        StyleBuilder styleBuilder = new StyleBuilder();
        PolygonSymbolizer restrictedSymb = styleBuilder.createPolygonSymbolizer(fillColor,
                Color.BLACK, 1);
        restrictedSymb.getFill().setOpacity(styleBuilder.literalExpression(opasity));
        org.geotools.styling.Style style = styleBuilder.createStyle(restrictedSymb);
        FileDataStore store = null;
        store = FileDataStoreFinder.getDataStore(fileName);
        SimpleFeatureSource featureSource = null;
        featureSource = store.getFeatureSource();
        Layer layer = new FeatureLayer(featureSource, style);
        return layer;
    }
}