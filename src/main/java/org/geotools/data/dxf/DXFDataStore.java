/*
 * $Id: DXFDataStore.java Matthijs $
 */
package org.geotools.data.dxf;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URL;

import org.geotools.data.*;
import org.geotools.data.dxf.parser.DXFParseException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

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
public class DXFDataStore extends ContentDataStore implements FileDataStore {
    private URL url;
    private FeatureReader featureReader;
    private String srs;
    private String targetSrs;
    private String strippedFileName;
    private String typeName;
    private ArrayList dxfInsertsFilter = new ArrayList();
    private AffineTransform transform;

    public DXFDataStore(URL url, String srs, AffineTransform transform) throws IOException {
        this(url, srs, null, transform);
    }

    public DXFDataStore(URL url, String srs, String targetSrs, AffineTransform transform) throws IOException {
        this.url = url;
        this.strippedFileName = getURLTypeName(url);
        this.srs = srs;
        this.targetSrs = targetSrs;
        this.transform = transform;
    }

    static String getURLTypeName(URL url) throws IOException {
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

    public FeatureReader getFeatureReader(String typeName) throws IOException {
        // Update featureReader for this typename
        resetFeatureReader(typeName);
        return featureReader;
    }

    @Override
    public FeatureReader getFeatureReader(Query query, Transaction transaction) throws IOException {
        return new FilteringFeatureReader(getFeatureReader(typeName), query.getFilter());
    }

    public FeatureReader getFeatureReader() throws IOException {
        if (featureReader == null)
            resetFeatureReader(typeName);
        return featureReader;
    }

    public void resetFeatureReader(String typeName) throws IOException {
        if (typeName == null)
            typeName = "";
        this.typeName = typeName;

        // Get geometryType from typeName (GeometryType)(typeName - fileName)
        String extension = typeName.replaceFirst(strippedFileName, "");
        GeometryType geometryType = GeometryType.getTypeByExtension(extension);

        if (featureReader == null) {
            try {
                featureReader = new DXFFeatureReader(url, typeName, srs, targetSrs, geometryType, dxfInsertsFilter, transform);
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

    protected List<Name> createTypeNames() throws IOException {
        LinkedList<Name> ret = new LinkedList<>();
        for (String typeName : GeometryType.getTypeNames(strippedFileName, GeometryType.ALL))
            ret.add(new NameImpl(typeName));
        return ret;
    }

    protected ContentFeatureSource createFeatureSource(ContentEntry contentEntry) throws IOException {
        String extension = typeName.replaceFirst(strippedFileName, "");
        GeometryType geometryType = GeometryType.getTypeByExtension(extension);

        return new DXFDataSource(url, srs, targetSrs, dxfInsertsFilter, transform, geometryType, contentEntry, Query.ALL);
    }

    @Override
    public SimpleFeatureType getSchema() throws IOException {
        return null;
    }

    @Override
    public void updateSchema(SimpleFeatureType simpleFeatureType) throws IOException {

    }

    @Override
    public SimpleFeatureSource getFeatureSource() throws IOException {
        return new DXFDataSource(url, srs, targetSrs, dxfInsertsFilter, transform, GeometryType.ALL, ensureEntry(new NameImpl("")), Query.ALL);
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Filter filter, Transaction transaction) throws IOException {
        return null;
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Transaction transaction) throws IOException {
        return null;
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(Transaction transaction) throws IOException {
        return null;
    }
}
