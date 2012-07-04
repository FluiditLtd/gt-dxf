/*
 * $Id: DXFFeatureReader.java Matthijs $
 */
package org.geotools.data.dxf;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.dxf.parser.DXFParseException;
import com.vividsolutions.jts.geom.Geometry;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.ArrayList;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.geotools.data.GeometryType;
import org.geotools.data.dxf.entities.DXFEntity;
import org.geotools.data.dxf.entities.DXFText;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.data.dxf.parser.DXFColor;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * @author Matthijs Laan, B3Partners
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/build/maven/javadoc/../../../modules/unsupported/dxf/src/main/java/org/geotools/data/dxf/DXFFeatureReader.java $
 */
public class DXFFeatureReader implements FeatureReader {

    private static final Log log = LogFactory.getLog(DXFFeatureReader.class);
    private SimpleFeatureType ft;
    private Iterator<SimpleFeature> entityIterator;
    private ArrayList<SimpleFeature> features;
    private GeometryType geometryType = null;
    private SimpleFeature cache;
    private ArrayList dxfInsertsFilter;
    private String info = "";
    private int featureID = 0;
    private double minX = 50, minY = 50, maxX = 100, maxY = 100;
    private MathTransform transform = null;

    public DXFFeatureReader(URL url, String typeName, String srs, GeometryType geometryType, ArrayList dxfInsertsFilter, AffineTransform transform) throws IOException, DXFParseException {
        InputStream cis = null;
        DXFLineNumberReader lnr = null;
        if (transform != null)
            this.transform = new AffineTransform2D(transform);

        try {
            cis = url.openStream();
            cis.mark(9192);
            try {
                GZIPInputStream gzip = new GZIPInputStream(cis);
                cis = gzip;
            } catch (IOException ex) {
                try {
                    cis.reset();
                    if (url.getFile().toString().toLowerCase().endsWith(".zip")) {
                        ZipInputStream zip = new ZipInputStream(cis);
                        if (zip.getNextEntry() != null)
                            cis = zip;
                        else
                            cis.reset();
                    }
                } catch (ZipException ex2) {
                    cis.reset();
                } catch (IOException ex2) {
                    cis.reset();
                }
            }
            lnr = new DXFLineNumberReader(new InputStreamReader(cis));
            DXFUnivers theUnivers = new DXFUnivers(dxfInsertsFilter);
            theUnivers.read(lnr);
            info = theUnivers.getInfo();

            // Affine transform the extents
            if (transform != null && theUnivers.getHeader() != null && theUnivers.getHeader()._EXTMIN != null && theUnivers.getHeader()._EXTMAX != null) {
                double[] extents = new double[]{theUnivers.getHeader()._EXTMIN.Y(), theUnivers.getHeader()._EXTMIN.X(),
                    theUnivers.getHeader()._EXTMAX.Y(), theUnivers.getHeader()._EXTMAX.X()};
                transform.transform(extents, 0, extents, 0, 2);
                minX = extents[1];
                minY = extents[0];
                maxX = extents[3];
                maxY = extents[2];
            }

            createFeatureType(typeName, srs);
            features = new ArrayList<SimpleFeature>(theUnivers.theEntities.size());
            for (DXFEntity entry : theUnivers.theEntities) {
                Geometry g = entry.getGeometry();
                if (this.transform != null && g != null)
                    try {
                        g = JTS.transform(g, this.transform);
                    } catch (MismatchedDimensionException ex) {
                    } catch (TransformException ex) {
                    }

                if (entry.getRefLayer().isVisible() && entry.isVisible())
                    features.add(SimpleFeatureBuilder.build(ft, new Object[]{
                                g,
                                entry.getLineTypeName(),
                                DXFColor.getColor(entry.getColor()),
                                entry.getRefLayerName(),
                                new Double(entry.getThickness()),
                                ((entry instanceof DXFText) ? ((DXFText) entry)._rotation : 0.0), // Text rotation
                                ((entry instanceof DXFText) ? ((((DXFText)entry)._value != null) && !((DXFText)entry)._value.isEmpty() ? ((DXFText)entry)._value : " ") : " "),
                                ((entry instanceof DXFText) ? ((DXFText)entry)._height : 1f),
                                ((entry instanceof DXFText) ? ((DXFText)entry)._align : 0f),
                                ((entry instanceof DXFText) ? ((DXFText)entry)._align2 : 0f),
                                new Integer(entry.isVisible() ? 1 : 0),
                            }, Integer.toString(featureID++)));
            }
        } catch (IOException ioe) {
            log.error("Error reading data in datastore: ", ioe);
            throw ioe;
        } finally {
            if (lnr != null) {
                lnr.close();
            }
            if (cis != null) {
                cis.close();
            }
        }

        // Set filter point, line, polygon, defined in datastore typenames
        updateTypeFilter(typeName, geometryType, srs);
    }

    public ReferencedEnvelope getBounds() {
        if (ft != null)
            return new ReferencedEnvelope(minY, maxY, minX, maxX, ft.getCoordinateReferenceSystem());
        else
            return null;
    }

    public void updateTypeFilter(String typeName, GeometryType geometryType, String srs) {
        this.geometryType = geometryType;
        entityIterator = features.iterator();
    }

    private void createFeatureType(String typeName, String srs) throws DataSourceException {
        CoordinateReferenceSystem crs = null;
        try {
            String authority = "EPSG";
            int colonPos = srs.indexOf(':');
            if (colonPos > 0) {
                authority = srs.substring(0, colonPos);
                //srs = srs.substring(colonPos + 1);
            }

            //CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory(authority, null); // new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));
            crs = CRS.decode(srs);//factory.createProjectedCRS(srs);
        } catch (Exception e) {
            throw new DataSourceException("Error parsing CoordinateSystem srs: \"" + srs + "\"");
        }

        int SRID = -1;
        if (crs != null) {
            try {
                Set ident = crs.getIdentifiers();
                if ((ident != null && !ident.isEmpty())) {
                    String code = ((NamedIdentifier) ident.toArray()[0]).getCode();
                    SRID = Integer.parseInt(code);
                }
            } catch (Exception e) {
                log.error("SRID could not be determined from crs!");
            }
        }

        try {

            SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
            ftb.setName(typeName);
            ftb.setCRS(crs);

            ftb.add("the_geom", Geometry.class);
            ftb.add("lineType", String.class);
            ftb.add("color", Color.class);
            ftb.add("layer", String.class);
            ftb.add("thickness", Double.class);
            ftb.add("rotation", Double.class);
            ftb.add("text", String.class);
            ftb.add("height", Float.class);
            ftb.add("align1", Float.class);
            ftb.add("align2", Float.class);
            ftb.add("visible", Integer.class);

            ft = ftb.buildFeatureType();

        } catch (Exception e) {
            throw new DataSourceException("Error creating SimpleFeatureType: " + typeName, e);
        }
    }

    public SimpleFeatureType getFeatureType() {
        return ft;
    }

    public SimpleFeature next() throws IOException, IllegalAttributeException, NoSuchElementException {
        return entityIterator.next();
    }

    public boolean hasNext() throws IOException {
        return entityIterator.hasNext();
    }

    public ServiceInfo getInfo() {
        DefaultServiceInfo serviceInfo = new DefaultServiceInfo();
        serviceInfo.setTitle("DXF FeatureReader");
        serviceInfo.setDescription(info);

        return serviceInfo;
    }

    public void close() throws IOException {
    }
}
