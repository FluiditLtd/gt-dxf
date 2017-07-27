package org.geotools.data.dxf;

import org.geotools.data.FeatureReader;
import org.geotools.data.GeometryType;
import org.geotools.data.Query;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by marku on 22.5.2017.
 */
public class DXFDataSource extends ContentFeatureSource  {
    private URL url;
    private String srs;
    private String targetCrs;
    private ArrayList dxfInsertsFilter;
    private AffineTransform transform;
    private GeometryType type;

    public DXFDataSource(URL url, String srs, String targetCrs, ArrayList dxfInsertsFilter, AffineTransform transform, GeometryType type, ContentEntry entry, Query query) {
        super(entry, query);

        this.url = url;
        this.srs = srs;
        this.targetCrs = targetCrs;
        this.dxfInsertsFilter = dxfInsertsFilter;
        this.type = type;
        this.transform = transform;
    }

    public DXFDataStore getDataStore() {
        return (DXFDataStore) super.getDataStore();
    }

    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        try {
            return new DXFFeatureReader(url, getState().getEntry().getTypeName(), srs, targetCrs, type, dxfInsertsFilter, transform);
        } catch (DXFParseException e) {
            throw new IOException("Error parsing DXF", e);
        }
    }

    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        return null;
    }

    protected int getCountInternal(Query query) throws IOException {
        return -1;
    }

    protected SimpleFeatureType buildFeatureType() throws IOException {
        return null;
    }
}
