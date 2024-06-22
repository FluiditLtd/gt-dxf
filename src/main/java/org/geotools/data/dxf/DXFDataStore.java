/*
 * $Id: DXFDataStore.java Matthijs $
 */
package org.geotools.data.dxf;

import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.database.AbstractFileDataStore;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.database.GeometryType;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * DataStore for reading a DXF file produced by Autodesk.
 * 
 * The attributes are always the same:
 * key: String
 * name: String
 * urlLink: String
 * entryLineNumber: Integer
 * parseError: Boolean
 * error: String
 *  * 
 * @author Chris van Lith B3Partners
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/build/maven/javadoc/../../../modules/unsupported/dxf/src/main/java/org/geotools/data/dxf/DXFDataStore.java $
 */
public class DXFDataStore extends AbstractFileDataStore {
    private InputStream stream;
    private URL url;
    private FeatureReader featureReader;
    private String srs;
    private String targetSrs;
    private String strippedFileName;
    private String typeName;
    private ArrayList dxfInsertsFilter = new ArrayList();
    private AffineTransform transform;

    public DXFDataStore(URL url, String srs, AffineTransform transform) throws IOException {
        this(url, null, srs, null, transform);
    }

    public DXFDataStore(InputStream stream, String srs, AffineTransform transform) throws IOException {
        this(null, stream, srs, null, transform);
    }

    public DXFDataStore(URL url, InputStream stream, String srs, String targetSrs, AffineTransform transform) throws IOException {
        this.stream = stream;
        this.url = url;
        this.strippedFileName = getURLTypeName(url);
        this.srs = srs;
        this.targetSrs = targetSrs;
        this.transform = transform;
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        if (query.getFilter().equals(Filter.INCLUDE)) {
            FeatureReader reader = getFeatureReader("");
            return ((DXFFeatureReader)reader).getBounds();
        }
        else
            return null;
    }
    
    public String[] getTypeNames() throws IOException {
        //return GeometryType.getTypeNames(strippedFileName, GeometryType.LINE, GeometryType.POINT, GeometryType.POLYGON);
        return GeometryType.getTypeNames(strippedFileName, GeometryType.ALL);
    }

    static String getURLTypeName(URL url) throws IOException {
        if (url == null) {
            return "";
        }
        String file = url.getFile();
        if (file.length() == 0) {
            return "unknown_dxf";
        } else {
            int i = file.lastIndexOf('/');
            if (i != -1) {
                file = file.substring(i + 1);
            }
            if (file.toLowerCase().endsWith(".dxf")) {
                file = file.substring(0, file.length() - 4);
            }
            else if (file.toLowerCase().endsWith(".dxf.zip")) {
                file = file.substring(0, file.length() - 8);
            }
            else if (file.toLowerCase().endsWith(".dxf.gz")) {
                file = file.substring(0, file.length() - 7);
            }            /* replace to make valid table names */
            file = file.replaceAll(" ", "_");
            return file;
        }
    }

    public void addDXFInsertFilter(String[] filteredNames) {
        dxfInsertsFilter.addAll(java.util.Arrays.asList(filteredNames));
    }

    public void addDXFInsertFilter(String filteredName) {
        dxfInsertsFilter.add(filteredName);
    }

    public SimpleFeatureType getSchema(String typeName) throws IOException {
        // Update featureReader with typename and return SimpleFeatureType
        return (SimpleFeatureType) getFeatureReader(typeName).getFeatureType();
    }

    @Override
    public SimpleFeatureType getSchema() throws IOException {
        return getSchema(typeName);
    }

    public FeatureReader getFeatureReader(String typeName) throws IOException {
        // Update featureReader for this typename
        resetFeatureReader(typeName);
        return featureReader;
    }

    @Override
    public FeatureReader getFeatureReader(String typeName, Query query) throws IOException {
        return new FilteringFeatureReader(getFeatureReader(typeName), query.getFilter());        
    }

    @Override
    public FeatureReader getFeatureReader(Query query, Transaction transaction) throws IOException {
        return new FilteringFeatureReader(getFeatureReader(typeName), query.getFilter());
    }

    @Override
    public FeatureReader getFeatureReader() throws IOException {
        if (featureReader == null) {
            resetFeatureReader(typeName);
        }
        return featureReader;
    }

    public void resetFeatureReader(String typeName) throws IOException {
        if (typeName == null) {
            typeName = "";
        }
        this.typeName = typeName;

        // Get geometryType from typeName (GeometryType)(typeName - fileName)
        String extension = typeName.replaceFirst(strippedFileName, "");
        GeometryType geometryType = GeometryType.getTypeByExtension(extension);

        if (featureReader == null) {
            try {
                featureReader = new DXFFeatureReader(url, stream, typeName, srs, targetSrs, geometryType, dxfInsertsFilter, transform);
            } catch (DXFParseException e) {
                throw new IOException("DXF parse exception" + e.getLocalizedMessage());
            }
        } else {
            ((DXFFeatureReader) featureReader).updateTypeFilter(typeName, geometryType, srs);
        }
    }

    @Override
    public ServiceInfo getInfo() {
        try {
            return ((DXFFeatureReader) getFeatureReader()).getInfo();
        } catch (IOException ex) {
            return null;
        }
    }
}
