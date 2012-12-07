package org.geotools.data.dxf.entities;

import java.io.EOFException;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.io.IOException;
import org.geotools.data.GeometryType;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFBlock;
import org.geotools.data.dxf.header.DXFBlockReference;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFParseException;

public class DXFInsert extends DXFBlockReference {
    public DXFPoint _point = new DXFPoint();
    public double _angle = 0.0;
    public double _xs = 1, _ys = 1;

    public DXFInsert(DXFInsert newInsert) {
        this(newInsert._point._point.x, newInsert._point._point.y, newInsert._blockName, newInsert._refBlock, newInsert.getRefLayer(), newInsert.visibility, newInsert.getColor(), newInsert.getLineType(), newInsert._angle);

        setType(newInsert.getType());
        setUnivers(newInsert.getUnivers());
    }

    public DXFInsert(double x, double y, String nomBlock, DXFBlock refBlock, DXFLayer layer, int visibility, int color, DXFLineType lineType, double angle) {
        super(color, layer, visibility, lineType, nomBlock, refBlock);
        _point = new DXFPoint(x, y, color, null, visibility, 1);
        _angle = angle;
    }

    public static DXFInsert read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        String nomBlock = "";
        DXFInsert m = null;
        DXFLayer layer = null;
        double x = 0, y = 0, xscale = 1, yscale = 1;
        int visibility = 0, color = -1;
        DXFBlock refBlock = null;
        DXFLineType lineType = null;
        double angle = 0.0;

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

                    br.reset();
                    doLoop = false;
                    break;
                case LAYER_NAME: //"8"
                    layer = univers.findLayer(cvp.getStringValue());
                    break;
                case NAME: //"2"
                    nomBlock = cvp.getStringValue();
                    break;
                case X_1: //"10"
                    x = cvp.getDoubleValue();
                    break;
                case Y_1: //"20"
                    y = cvp.getDoubleValue();
                    break;
                case ANGLE_1: //"20"
                    //angle = 360d - cvp.getDoubleValue();
                    angle = cvp.getDoubleValue();
                    break;
                case DOUBLE_2: // 41
                    xscale = cvp.getDoubleValue();
                    break;
                case DOUBLE_3: // 42
                    yscale = cvp.getDoubleValue();
                    break;
                case COLOR: //"62"
                    color = cvp.getShortValue();
                    break;
                case VISIBILITY: //"60"
                    visibility = cvp.getShortValue();
                    break;
                case LINETYPE_NAME: //"6"
                    lineType = univers.findLType(cvp.getStringValue());
                    break;
                default:
                    break;
            }
        }

        m = new DXFInsert(x, y, nomBlock, refBlock, layer, visibility, color, lineType, angle);
        m.setType(GeometryType.POINT);
        m.setUnivers(univers);
        m._xs = xscale;
        m._ys = yscale;

        univers.addRefBlockForUpdate(m);

        return m;
    }

    public String toString(double x, double y, int visibility, int c, DXFLineType lineType) {
        StringBuilder s = new StringBuilder();
        s.append("DXFInsert [");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y + ", ");
        s.append("visibility: ");
        s.append(visibility + ", ");
        s.append("color: ");
        s.append(c + ", ");
        s.append("line type: ");
        if (lineType != null) {
            s.append(lineType._name);
        }
        s.append("]");
        return s.toString();
    }

    @Override
    public DXFEntity translate(double x, double y) {
        _point._point.x += x;
        _point._point.y += y;

        return this;
    }

    @Override
    public DXFEntity clone() {
        return new DXFInsert(this);
    }
}
