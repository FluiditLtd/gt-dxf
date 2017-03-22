package org.geotools.data.dxf.entities;

import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.GeometryType;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFParseException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import java.util.Map;

public class DXFSolid extends DXFEntity {
    public DXFPoint _p1 = new DXFPoint();
    public DXFPoint _p2 = new DXFPoint();
    public DXFPoint _p3 = new DXFPoint();
    public DXFPoint _p4 = null;

    public DXFSolid(DXFSolid newSolid) {
        this(new DXFPoint(newSolid._p1.X(), newSolid._p1.Y(), newSolid._p4.Z(), newSolid.getColor(), null, 0, newSolid.getThickness()),
                new DXFPoint(newSolid._p2.X(), newSolid._p2.Y(), newSolid._p4.Z(), newSolid.getColor(), null, 0, newSolid.getThickness()),
                new DXFPoint(newSolid._p3.X(), newSolid._p3.Y(), newSolid._p4.Z(), newSolid.getColor(), null, 0, newSolid.getThickness()),
                new DXFPoint(newSolid._p4.X(), newSolid._p4.Y(), newSolid._p4.Z(), newSolid.getColor(), null, 0, newSolid.getThickness()),
                newSolid.getThickness(), newSolid.getColor(), newSolid.getRefLayer(), newSolid.visibility, newSolid.getLineType());

        setType(newSolid.getType());
        setUnivers(newSolid.getUnivers());
    }

    public DXFSolid(DXFPoint p1, DXFPoint p2, DXFPoint p3, DXFPoint p4,
            double thickness, int c, DXFLayer l, int visibility, DXFLineType lineType) {
        super(c, l, visibility, lineType, thickness);

        _p1 = p1;
        _p2 = p2;
        _p3 = p3;

        if (p4 == null) {
            _p4 = p3;
        } else {
            _p4 = p4;
        }
    }

    public static DXFEntity read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        double p1_x = 0, p2_x = 0, p3_x = 0, p4_x = 0,
                p1_y = 0, p2_y = 0, p3_y = 0, p4_y = 0,
                p1_z = 0, p2_z = 0, p3_z = 0, p4_z = 0;
        double thickness = 0;
        int visibility = 0, c = -1;
        DXFLayer l = null;
        DXFLineType lineType = null;

        DXFCodeValuePair cvp = null;
        DXFGroupCode gc = null;
        Map<String, List<String>> xdata = null;

        boolean doLoop = true;
        while (doLoop) {
            cvp = new DXFCodeValuePair();
            try {
                gc = cvp.read(br);
            } catch (DXFParseException ex) {
                throw new IOException("DXF parse error" + ex.getLocalizedMessage());
            } catch (EOFException e) {
                doLoop = false;
                break;
            }

            switch (gc) {
                case TYPE:
                    String type = cvp.getStringValue();
                    // geldt voor alle waarden van type
                    br.reset();
                    doLoop = false;
                    break;
                case X_1: //"10"
                    p1_x = cvp.getDoubleValue();
                    break;
                case X_2: //"11"
                    p2_x = cvp.getDoubleValue();
                    break;
                case X_3: //"12"
                    p3_x = cvp.getDoubleValue();
                    break;
                case X_4: //"13"
                    p4_x = cvp.getDoubleValue();
                    break;
                case Y_1: //"20"
                    p1_y = cvp.getDoubleValue();
                    break;
                case Y_2: //"21"
                    p2_y = cvp.getDoubleValue();
                    break;
                case Y_3: //"22"
                    p3_y = cvp.getDoubleValue();
                    break;
                case Y_4: //"23"
                    p4_y = cvp.getDoubleValue();
                    break;
                case Z_1: //"20"
                    p1_z = cvp.getDoubleValue();
                    break;
                case Z_2: //"21"
                    p2_z = cvp.getDoubleValue();
                    break;
                case Z_3: //"22"
                    p3_z = cvp.getDoubleValue();
                    break;
                case Z_4: //"23"
                    p4_z = cvp.getDoubleValue();
                    break;
                case THICKNESS: //"39"
                    thickness = cvp.getDoubleValue();
                    break;
                case LAYER_NAME: //"8"
                    l = univers.findLayer(cvp.getStringValue());
                    break;
                case COLOR: //"62"
                    c = cvp.getShortValue();
                    break;
                case LINETYPE_NAME: //"6"
                    lineType = univers.findLType(cvp.getStringValue());
                    break;
                case VISIBILITY: //"60"
                    visibility = cvp.getShortValue();
                    break;
                case XDATA_APPLICATION_NAME:
                    xdata = readXdata(cvp.getStringValue(), br, univers, xdata);
                    break;
                default:
                    break;
            }

        }
        DXFSolid e = new DXFSolid(
                new DXFPoint(p1_x, p1_y, p1_z, c, null, visibility, 1),
                new DXFPoint(p2_x, p2_y, p2_z, c, null, visibility, 1),
                new DXFPoint(p3_x, p3_y, p3_z, c, null, visibility, 1),
                new DXFPoint(p4_x, p4_y, p4_z, c, null, visibility, 1),
                thickness,
                c,
                l,
                visibility,
                lineType);
        e.setType(GeometryType.POLYGON);
        e.setXData(xdata);
        e.setUnivers(univers);
        return e;
    }

    public Coordinate[] toCoordinateArray() {
        List<Coordinate> lc = new ArrayList<Coordinate>();

        DXFPoint[] points = {_p1, _p2, _p3, _p4};
        for (DXFPoint point : points) {
            Coordinate c = new Coordinate(point.X(), point.Y());
            lc.add(c);
        }

        // Fix last line in LinearRing
        lc.add(lc.get(0));

        return rotateAndPlace(lc.toArray(new Coordinate[]{}));
    }

    @Override
    public Geometry getGeometry() {
        if (geometry == null) {
            updateGeometry();
        }

        return super.getGeometry();
    }

    @Override
    public void updateGeometry() {
        Coordinate[] ca = toCoordinateArray();
        if (ca != null && ca.length > 1) {
            LinearRing lr = getUnivers().getGeometryFactory().createLinearRing(ca);
            geometry = getUnivers().getGeometryFactory().createPolygon(lr, null);
        } else {

        }
    }

    public String toString(double p1_x,
            double p2_x,
            double p3_x,
            double p4_x,
            double p1_y,
            double p2_y,
            double p3_y,
            double p4_y,
            double thickness,
            int c,
            int visibility) {
        StringBuilder s = new StringBuilder();
        s.append("DXFSolid [");
        s.append("p1_x: ");
        s.append(p1_x + ", ");
        s.append("p2_x: ");
        s.append(p2_x + ", ");
        s.append("p3_x: ");
        s.append(p3_x + ", ");
        s.append("p4_x: ");
        s.append(p4_x + ", ");
        s.append("p1_y: ");
        s.append(p1_y + ", ");
        s.append("p2_y: ");
        s.append(p2_y + ", ");
        s.append("p3_y: ");
        s.append(p3_y + ", ");
        s.append("p4_y: ");
        s.append(p4_y + ", ");
        s.append("thickness: ");
        s.append(thickness + ", ");
        s.append("color: ");
        s.append(c + ", ");
        s.append("visibility: ");
        s.append(visibility);
        s.append("]");
        return s.toString();
    }

    @Override
    public DXFEntity clone() {
        return new DXFSolid(this);
    }
}
