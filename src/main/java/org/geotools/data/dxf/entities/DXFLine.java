package org.geotools.data.dxf.entities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import java.io.EOFException;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.io.IOException;

import org.geotools.data.GeometryType;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFParseException;

public class DXFLine extends DXFEntity {
    public DXFPoint _a = new DXFPoint();
    public DXFPoint _b = new DXFPoint();

    public DXFLine(DXFLine newLine) {
        this(new DXFPoint(newLine._a._point.x, newLine._a._point.y, newLine.getColor(), null, 0, newLine.getThickness()),
                new DXFPoint(newLine._b._point.x, newLine._b._point.y, newLine.getColor(), null, 0, newLine.getThickness()),
                newLine.getColor(), newLine.getRefLayer(), newLine.getLineType(), newLine.getThickness(), newLine.visibility);

        setType(newLine.getType());
        setUnivers(newLine.getUnivers());
    }

    public DXFLine(DXFPoint a, DXFPoint b, int color, DXFLayer layer, DXFLineType lineType, double thickness, int visibility) {
        super(color, layer, visibility, lineType, thickness);
        _a = a;
        _b = b;
    }

    public static DXFLine read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        DXFLayer layer = null;
        double x1 = 0, y1 = 0, x2 = 0, y2 = 0, thickness = 0;
        DXFLineType lineType = null;
        int visibility = 0, color = -1;

        DXFCodeValuePair cvp = null;
        DXFGroupCode gc = null;

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
                    y1 = cvp.getDoubleValue();
                    break;
                case Y_1: //"20"
                    x1 = cvp.getDoubleValue();
                    break;
                case X_2: //"11"
                    y2 = cvp.getDoubleValue();
                    break;
                case Y_2: //"21"
                    x2 = cvp.getDoubleValue();
                    break;
                case LAYER_NAME: //"8"
                    layer = univers.findLayer(cvp.getStringValue());
                    break;
                case COLOR: //"62"
                    color = cvp.getShortValue();
                    break;
                case LINETYPE_NAME: //"6"
                    lineType = univers.findLType(cvp.getStringValue());
                    break;
                case THICKNESS: //"39"
                    thickness = cvp.getDoubleValue();
                    break;
                case VISIBILITY: //"60"
                    visibility = cvp.getShortValue();
                    break;
                default:
                    break;
            }

        }
        DXFLine e = new DXFLine(new DXFPoint(x1, y1, color, layer, visibility, 1),
                new DXFPoint(x2, y2, color, layer, visibility, 1),
                color,
                layer,
                lineType,
                thickness,
                visibility);
        e.setType(GeometryType.LINE);
        e.setUnivers(univers);
        return e;
    }

    public Coordinate[] toCoordinateArray() {
        if (_a == null || _b == null) {
            return null;
        }

        return rotateAndPlace(new Coordinate[]{_a.toCoordinate(), _b.toCoordinate()});
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
            geometry = getUnivers().getGeometryFactory().createLineString(ca);
        } else {
        }
    }

    public String toString(double x1, double y1, double x2, double y2, int c, int visibility, double thickness) {
        StringBuilder s = new StringBuilder();
        s.append("DXFLine [");
        s.append("x1: ");
        s.append(x1 + ", ");
        s.append("y1: ");
        s.append(y1 + ", ");
        s.append("x2: ");
        s.append(x2 + ", ");
        s.append("y2: ");
        s.append(y2 + ", ");
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
    public DXFEntity translate(double x, double y) {
        _a._point.x += x;
        _a._point.y += y;

        _b._point.x += x;
        _b._point.y += y;
        return this;
    }

    @Override
    public DXFEntity clone() {
        return new DXFLine(this);
    }
}
