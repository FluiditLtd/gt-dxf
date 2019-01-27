package org.geotools.data.dxf.entities;

import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.database.GeometryType;
import org.geotools.data.dxf.header.DXFLineType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;

import java.io.EOFException;
import java.io.IOException;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DXFCircle extends DXFEntity {
    public DXFPoint _point = new DXFPoint();
    public double _radius = 0;

    public DXFCircle(DXFCircle newCircle) {
        this(new DXFPoint(newCircle._point.X(), newCircle._point.Y(), newCircle._point.Z(), newCircle.getColor(), newCircle.getRefLayer(), newCircle.visibility, newCircle.getThickness()),
                newCircle._radius, newCircle.getLineType(), newCircle.getColor(), newCircle.getRefLayer(), 0, newCircle.getThickness());

        setType(newCircle.getType());
        setUnivers(newCircle.getUnivers());
    }

    public DXFCircle(DXFPoint p, double r, DXFLineType lineType, int c, DXFLayer l, int visibility, double thickness) {
        super(c, l, visibility, lineType, thickness);
        _point = p;
        _radius = r;
    }

    public static DXFCircle read(DXFLineNumberReader br, DXFUnivers univers) throws NumberFormatException, IOException {

        int visibility = 0, c = 0;
        double x = 0, y = 0, z = 0, r = 0, thickness = 1;
        DXFLayer l = null;
        DXFLineType lineType = null;

        int sln = br.getLineNumber();
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
                case LINETYPE_NAME: //"6"
                    lineType = univers.findLType(cvp.getStringValue());
                    break;
                case LAYER_NAME: //"8"
                    l = univers.findLayer(cvp.getStringValue());
                    break;
                case THICKNESS: //"39"
                    thickness = cvp.getDoubleValue();
                    break;
                case VISIBILITY: //"60"
                    visibility = cvp.getShortValue();
                    break;
                case COLOR: //"62"
                    c = cvp.getShortValue();
                    break;
                case X_1: //"10"
                    x = cvp.getDoubleValue();
                    break;
                case Y_1: //"20"
                    y = cvp.getDoubleValue();
                    break;
                case Z_1: //"30"
                    z = cvp.getDoubleValue();
                    break;
                case DOUBLE_1: //"40"
                    r = cvp.getDoubleValue();
                    break;
                case XDATA_APPLICATION_NAME:
                    xdata = readXdata(cvp.getStringValue(), br, univers, xdata);
                    break;
                default:
                    break;
            }

        }
        DXFCircle e = new DXFCircle(new DXFPoint(x, y, z, c, l, visibility, 1), r, lineType, c, l, visibility, thickness);
        e.setType(GeometryType.POLYGON);
        e.setXData(xdata);
        e.setUnivers(univers);
        return e;
    }

    public Coordinate[] toCoordinateArray() {
        if (_point == null || _point._point == null || _radius <= 0) {
            return null;
        }
        List<Coordinate> lc = new ArrayList<Coordinate>();
        double startAngle = 0.0;
        double endAngle = 2 * Math.PI;
        double segAngle = 2 * Math.PI / _radius;

        if(_radius < DXFUnivers.NUM_OF_SEGMENTS){
            segAngle = DXFUnivers.MIN_ANGLE;
        }

        double angle = startAngle;
        for (;;) {
            double x = _point.X() + _radius * Math.cos(angle);
            double y = _point.Y() + _radius * Math.sin(angle);
            Coordinate c = new Coordinate(x, y, _point.Z());
            lc.add(c);

            if (angle >= endAngle) {
                break;
            }

            angle += segAngle;
            if (angle > endAngle) {
                angle = endAngle;
            }
        }

        // Fix to avoid error while creating LinearRing
        // If first coord is not equal to last coord
        if (!lc.get(0).equals(lc.get(lc.size() - 1))) {
            // Set last coordinate == first
            lc.set(lc.size() - 1, lc.get(0));
        }

        return rotateAndPlace(lc.toArray(new Coordinate[lc.size()]));
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

    public String toString(double x, double y, int c, int visibility, double thickness) {
        StringBuilder s = new StringBuilder();
        s.append("DXFCircle [");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y + ", ");
        s.append("color: ");
        s.append(c + ", ");
        s.append("visibility: ");
        s.append(visibility + ", ");
        s.append("thickness: ");
        s.append(thickness);
        s.append("]");
        return s.toString();
    }

    @Override
    public DXFEntity clone() {
        return new DXFCircle(this);
    }
}
