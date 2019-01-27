package org.geotools.data.dxf.entities;

import java.awt.geom.AffineTransform;
import java.io.EOFException;

import org.geotools.data.dxf.header.DXFBlock;
import org.geotools.data.dxf.header.DXFBlockReference;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.dxf.parser.DXFUnivers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geotools.database.GeometryType;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.referencing.operation.transform.AffineTransform2D;

public class DXFInsert extends DXFBlockReference {
    public DXFPoint _point = new DXFPoint();
    public double _angle = 0.0;
    public double _xs = 1, _ys = 1, _zs = 1;

    public DXFInsert(DXFInsert newInsert) {
        this(newInsert._point.X(), newInsert._point.Y(), newInsert._point.Z(), newInsert._blockName, newInsert._refBlock, newInsert.getRefLayer(), newInsert.visibility, newInsert.getColor(), newInsert.getLineType(), newInsert._angle);

        setType(newInsert.getType());
        setUnivers(newInsert.getUnivers());
    }

    public DXFInsert(double x, double y, double z, String nomBlock, DXFBlock refBlock, DXFLayer layer, int visibility, int color, DXFLineType lineType, double angle) {
        super(color, layer, visibility, lineType, nomBlock, refBlock);
        _point = new DXFPoint(x, y, z, color, null, visibility, 1);
        _angle = angle;
    }

    public static DXFInsert read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        String nomBlock = "";
        DXFInsert m = null;
        DXFLayer layer = null;
        double x = 0, y = 0, z = 0, xscale = 1, yscale = 1, zscale = 1;
        int visibility = 0, color = -1;
        DXFBlock refBlock = null;
        DXFLineType lineType = null;
        double angle = 0.0;

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
                case Z_1: //"20"
                    z = cvp.getDoubleValue();
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
                case DOUBLE_4: // 43
                    zscale = cvp.getDoubleValue();
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
                case XDATA_APPLICATION_NAME:
                    xdata = readXdata(cvp.getStringValue(), br, univers, xdata);
                    break;
                default:
                    break;
            }
        }

        m = new DXFInsert(x, y, z, nomBlock, refBlock, layer, visibility, color, lineType, angle);
        m.setXData(xdata);
        m.setType(GeometryType.POINT);
        m.setUnivers(univers);
        m._xs = xscale;
        m._ys = yscale;
        m._zs = zscale;

        return m;
    }

    public AffineTransform2D getTransform(AffineTransform2D at) {
        AffineTransform copy = new AffineTransform();
        copy.translate(_point.X(), _point.Y());
        copy.scale(_xs, _ys);
        copy.rotate(Math.toRadians(_angle));
        
        DXFBlock block = univers.findBlock(_blockName);
        copy.translate(block._point.X(), block._point.Y());
        copy.scale(block._xs, block._ys);
        
        copy.preConcatenate(at);
        
        return new AffineTransform2D(copy);
    }
    
    public List<DXFEntity> getChildren() {
        return univers.findBlock(_blockName).theEntities;
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
    public DXFEntity clone() {
        return new DXFInsert(this);
    }
}
