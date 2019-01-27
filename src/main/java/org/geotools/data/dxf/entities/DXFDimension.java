package org.geotools.data.dxf.entities;

import java.io.EOFException;

import org.geotools.data.dxf.header.DXFBlock;
import org.geotools.data.dxf.header.DXFBlockReference;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.data.dxf.parser.DXFUnivers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.geotools.database.GeometryType;
import org.geotools.data.dxf.header.DXFLineType;

public class DXFDimension extends DXFBlockReference {
    public double _angle = 0;//50
    public String _dimension = "<>";//1
    public DXFPoint _point_WCS = new DXFPoint();//10,20

    public DXFDimension(DXFDimension newDimension) {
        this(newDimension._angle, newDimension._dimension, newDimension._point_WCS._point.x, newDimension._point_WCS._point.y, newDimension._point_WCS._point.z,
                newDimension._refBlock, newDimension._blockName, newDimension.getRefLayer(), newDimension.visibility, newDimension.getColor(), newDimension.getLineType());

        setType(newDimension.getType());
        setUnivers(newDimension.getUnivers());
    }

    public DXFDimension(double a, String dim, double x, double y, double z, DXFBlock refBlock, String nomBlock, DXFLayer l, int visibility, int c, DXFLineType lineType) {
        super(c, l, visibility, null, nomBlock, refBlock);
        _angle = a;
        _dimension = dim;
        _point_WCS = new DXFPoint(x, y, z, c, null, visibility, 1);
    }

    public static DXFDimension read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        String dimension = "", nomBlock = "";
        DXFDimension d = null;
        DXFLayer l = null;
        DXFBlock refBlock = null;
        double angle = 0, x = 0, y = 0, z = 0;
        int visibility = 0, c = -1;
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
                case LAYER_NAME: //"8"
                    l = univers.findLayer(cvp.getStringValue());
                    break;
                case TEXT: //"1"
                    dimension = cvp.getStringValue();
                    break;
                case ANGLE_1: //"50"
                    angle = cvp.getDoubleValue();
                    break;
                case NAME: //"2"
                    nomBlock = cvp.getStringValue();
                    break;
                case LINETYPE_NAME: //"6"
                    lineType = univers.findLType(cvp.getStringValue());
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
                case VISIBILITY: //"60"
                    visibility = cvp.getShortValue();
                    break;
                case COLOR: //"62"
                    c = cvp.getShortValue();
                    break;
                case XDATA_APPLICATION_NAME:
                    xdata = DXFEntity.readXdata(cvp.getStringValue(), br, univers, xdata);
                    break;
                default:
                    break;
            }
        }

        d = new DXFDimension(angle, dimension, x, y, z, refBlock, nomBlock, l, visibility, c, lineType);
        d.setType(GeometryType.UNSUPPORTED);
        d.setXData(xdata);
        d.setUnivers(univers);

        return d;
    }

    @Override
    public void updateGeometry(){
        // not supported
    }


    public String toString(String dimension, double angle, String nomBlock, double x, double y, int visibility, int c) {
        StringBuilder s = new StringBuilder();
        s.append("DXFDimension [");
        s.append("dimension: ");
        s.append(dimension + ", ");
        s.append("angle: ");
        s.append(angle + ", ");
        s.append("nameBlock: ");
        s.append(nomBlock + ", ");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y + ", ");
        s.append("visibility: ");
        s.append(visibility + ", ");
        s.append("color: ");
        s.append(c);
        s.append("]");
        return s.toString();
    }

    @Override
    public DXFEntity clone() {
        return new DXFDimension(this);
    }
}
