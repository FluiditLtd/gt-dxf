/*
 * $Id: DXFFeatureReader.java Matthijs $
 */
package org.geotools.data.dxf;

import org.geotools.data.dxf.parser.DXFParseException;
import com.vividsolutions.jts.geom.Geometry;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.ArrayList;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
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

import org.geotools.referencing.NamedIdentifier;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.data.dxf.entities.DXFInsert;
import org.geotools.data.dxf.parser.DXFColor;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;

/**
 * @author Matthijs Laan, B3Partners
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/build/maven/javadoc/../../../modules/unsupported/dxf/src/main/java/org/geotools/data/dxf/DXFFeatureReader.java $
 */
public class DXFFeatureReader implements FeatureReader {
    private SimpleFeatureType ft;
    private Iterator<SimpleFeature> entityIterator;
    private ArrayList<SimpleFeature> features;
    private GeometryType geometryType = null;
    private SimpleFeature cache;
    private ArrayList dxfInsertsFilter;
    private String info = "";
    private int featureID = 0;
    private double minX = 50, minY = 50, maxX = 100, maxY = 100;
    private AffineTransform2D transform = null;
    private MathTransform crsTransform = null;

    public DXFFeatureReader(URL url, String typeName, String srs, String targetCrs, GeometryType geometryType, ArrayList dxfInsertsFilter, AffineTransform transform) throws IOException, DXFParseException {
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
            if (theUnivers.getHeader() != null && theUnivers.getHeader()._EXTMIN != null && theUnivers.getHeader()._EXTMAX != null) {
                double[] extents = new double[]{theUnivers.getHeader()._EXTMIN.X(), theUnivers.getHeader()._EXTMIN.Y(),
                    theUnivers.getHeader()._EXTMAX.X(), theUnivers.getHeader()._EXTMAX.Y()};
                if (transform != null)
                    transform.transform(extents, 0, extents, 0, 2);
                minX = extents[0];
                minY = extents[1];
                maxX = extents[2];
                maxY = extents[3];
            }

            createFeatureType(typeName, srs);
            features = new ArrayList<SimpleFeature>(theUnivers.theEntities.size());
            AffineTransform tr;
            if (this.transform != null)
                tr = new AffineTransform(this.transform);
            else
                tr = new AffineTransform();
            
            tr.translate(-theUnivers.getHeader()._UCSORG.X(), -theUnivers.getHeader()._UCSORG.Y());
            
            if (targetCrs != null)
                try {
                    crsTransform = CRS.findMathTransform(ft.getCoordinateReferenceSystem(), CRS.decode(targetCrs, true), true);
                } catch (NoSuchAuthorityCodeException ex) {
                } catch (FactoryException ex) {
                }
            
            AffineTransform2D tr2 = new AffineTransform2D(tr);
            for (DXFEntity entry : theUnivers.theEntities)
                processEntity(entry, tr2, ft, -1);
        } catch (IOException ioe) {
            Logger.getLogger(DXFFeatureReader.class.getName()).log(Level.WARNING, "Error reading data in datastore: ", ioe);
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

    private void processEntity(DXFEntity ent, AffineTransform2D transform, SimpleFeatureType ft, int insertColor) {
        if (ent instanceof DXFInsert) {
            transform = ((DXFInsert)ent).getTransform(transform);
            for (DXFEntity child : ((DXFInsert)ent).getChildren())
                processEntity(child, transform, ft, ((DXFInsert)ent).getColor());
        }
        else {
            Geometry g = ent.getGeometry();
            if (g == null)
                return;
            
            try {
                g = JTS.transform(g, transform);
            } catch (MismatchedDimensionException ex) {
            } catch (TransformException ex) {
            }

            double rotation = 0;
            if (ent instanceof DXFText) {
                double orig = ((DXFText)ent)._rotation;
                double x = ((DXFText)ent)._point.X();
                double y = ((DXFText)ent)._point.Y();
                Matrix matrix = transform.derivative(new Point2D.Double(x, y));
                double x2 = matrix.getElement(0, 0) * x + matrix.getElement(0, 1) * y;
                double y2 = matrix.getElement(1, 0) * x + matrix.getElement(1, 1) * y;
                rotation = orig + Math.toDegrees(Math.atan2(y2 - y, x2 - x));

                if (crsTransform != null) 
                    try {
                        DirectPosition pos1 = crsTransform.transform(new DirectPosition2D(ft.getCoordinateReferenceSystem(), x2, y2), null);
                        DirectPosition pos2 = crsTransform.transform(new DirectPosition2D(ft.getCoordinateReferenceSystem(), x2 + 1, y2 + 1), null);
                        rotation = rotation + (-45. + Math.toDegrees(Math.atan2(pos2.getOrdinate(1) - pos1.getOrdinate(1), pos2.getOrdinate(0) - pos1.getOrdinate(0))));
                    } catch (MismatchedDimensionException ex) {
                        Logger.global.log(Level.SEVERE, "ex", ex);
                    } catch (TransformException ex) {
                        Logger.global.log(Level.SEVERE, "ex", ex);
                    }
            }
            
            if (ent.getRefLayer().isVisible() && ent.isVisible())
                features.add(SimpleFeatureBuilder.build(ft, new Object[]{
                            g,
                            ent.getLineTypeName(),
                            DXFColor.getColor(ent.getActualColor(insertColor)),
                            ent.getRefLayerName(),
                            new Double(ent.getThickness()),
                            rotation, // Text rotation
                            ((ent instanceof DXFText) ? ((((DXFText)ent)._value != null) && !((DXFText)ent)._value.isEmpty() ? ((DXFText)ent)._value : " ") : " "),
                            ((ent instanceof DXFText) ? ((DXFText)ent)._height : 1f),
                            ((ent instanceof DXFText) ? ((DXFText)ent)._align : 0f),
                            ((ent instanceof DXFText) ? ((DXFText)ent)._align2 : 0f),
                            new Integer(ent.isVisible() ? 1 : 0),
                            formatXData(ent.getXData()),
                            ent.getClass().getSimpleName(),
                            ent,
                        }, Integer.toString(featureID++)));
        }
    }
    
    private String formatXData(Map<String, List<String>> xdata) {
        if (xdata == null)
            return null;
        
        List<String> entries = new ArrayList<String>(xdata.size());
        for (Entry<String, List<String>> entry : xdata.entrySet()) {
            String value = '"' + entry.getKey() + "\":[";
            List<String> quoted = new ArrayList<String>(entry.getValue().size());
            for (String val : entry.getValue())
                quoted.add('"' + val + '"');
            value += String.join(",", quoted) + "]";
            entries.add(value);
        }
        return String.join(",", entries);
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
            crs = CRS.decode(srs, true);//factory.createProjectedCRS(srs);
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
                Logger.getLogger(DXFFeatureReader.class.getName()).log(Level.WARNING, "SRID could not be determined from crs!", e);
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
            ftb.add("xdata", String.class);
            ftb.add("class", String.class);
            ftb.add("entity", DXFEntity.class);

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

    public int getSize() {
        return features.size();
    }
}
