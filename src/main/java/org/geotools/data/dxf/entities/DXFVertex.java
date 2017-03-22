package org.geotools.data.dxf.entities;

import java.io.EOFException;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.geotools.data.GeometryType;
import org.geotools.data.dxf.header.DXFBlock;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFParseException;

public class DXFVertex extends DXFPoint {
    protected double _bulge = 0;

    public DXFVertex(DXFVertex newVertex) {
        this(newVertex._point.x, newVertex._point.y, newVertex._point.z, newVertex._bulge, newVertex.getColor(), newVertex.getRefLayer(), newVertex.visibility);

        setType(newVertex.getType());
        setUnivers(newVertex.getUnivers());
    }

    public DXFVertex(double x, double y, double z, double b, int c, DXFLayer l, int visibility) {
        super(x, y, z, c, l, visibility, 1);
        _bulge = b;
    }

    public static DXFVertex read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        DXFLayer l = null;
        int visibility = 0, c = -1;
        double x = 0, y = 0, z = 0, b = 0;

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
                case LAYER_NAME: //"8"
                    l = univers.findLayer(cvp.getStringValue());
                    break;
                case DOUBLE_3: //"42"
                    b = cvp.getDoubleValue();
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
                case COLOR: //"62"
                    c = cvp.getShortValue();
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

        DXFVertex e = new DXFVertex(x, y, z, b, c, l, visibility);
        e.setType(GeometryType.POINT);
        e.setXData(xdata);
        e.setUnivers(univers);
        return e;
    }

    public String toString(double b, double x, double y, int c, int visibility) {
        StringBuilder s = new StringBuilder();
        s.append("DXFVertex [");
        s.append("bulge: ");
        s.append(b + ", ");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y + ", ");
        s.append("c: ");
        s.append(c + ", ");
        s.append("visibility: ");
        s.append(visibility);
        s.append("]");
        return s.toString();
    }  
}
