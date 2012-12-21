package org.geotools.data.dxf.entities;

import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.io.EOFException;
import java.io.IOException;

import org.geotools.data.GeometryType;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.header.DXFTables;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFParseException;

public class DXFAttrib extends DXFText {
    public DXFAttrib(DXFAttrib newAttrib) {
        this(newAttrib._point.X(), newAttrib._point.Y(), newAttrib._point.Z(), newAttrib._value, newAttrib._rotation, newAttrib.getThickness(), newAttrib._height,
                newAttrib._align, newAttrib._align2, newAttrib._style, newAttrib.getColor(), newAttrib.getRefLayer(), newAttrib._angle, newAttrib._zoomfactor, newAttrib.visibility, newAttrib.getLineType());

        setType(newAttrib.getType());
        setUnivers(newAttrib.getUnivers());
    }

    public DXFAttrib(double x, double y, double z, String value, double rotation, double thickness, double height, float align, float align2, String style, int color, DXFLayer layer, double angle, double zoomFactor, int visibility, DXFLineType lineType) {
        super(x, y, z, value, rotation, thickness, height, align, align2, style, color, layer, angle, zoomFactor, visibility, lineType);
    }

    public static DXFAttrib readAttrib(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        DXFLayer l = null;
        String value = "", style = "STANDARD";
        int visibility = 0, c = -1;
        int flag = 0;
        float align = 0, align2 = 0;
        DXFLineType lineType = null;
        double x = 0,
                y = 0,
                z = 0,
                angle = 0,
                rotation = 0,
                zoomfactor = 1,
                thickness = DXFTables.defaultThickness,
                height = 0;

        int sln = br.getLineNumber();

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
                    br.reset();
                    doLoop = false;
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
                case TEXT: //"1"
                    value = cvp.getStringValue();
                    if (value.startsWith("{") && value.endsWith("}") && value.contains("|")) {
                        value = value.substring(1, value.length() - 1);
                        value.substring(value.lastIndexOf('|'));
                    }
                    value = value.replace("\\P", "\n");
                    value = processText(value);
                    value = processText2(value);
                    break;
                case ANGLE_1: //"50"
                    rotation = cvp.getDoubleValue();
                    break;
                case THICKNESS: //"39"
                    thickness = cvp.getDoubleValue();
                    break;
                case DOUBLE_1: //"40"
                    height = cvp.getDoubleValue() + 1;
                    break;
                case ANGLE_2: //"51"
                    angle = cvp.getDoubleValue();
                    break;
                case DOUBLE_2: //"41"
                    zoomfactor = cvp.getDoubleValue();
                    break;
                case INT_1: // "70"
                    flag = cvp.getShortValue();
                    break;
                case INT_3: //"72"
                    align = cvp.getShortValue();
                    break;
                case INT_4: //"73"
                    align2 = cvp.getShortValue();
                    break;
                case LAYER_NAME: //"8"
                    l = univers.findLayer(cvp.getStringValue());
                    break;
                case COLOR: //"62"
                    c = cvp.getShortValue();
                    break;
                case TEXT_STYLE_NAME: //"7"
                    style = cvp.getStringValue();
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
        
        // Aligned and fit are the same thing as left
        if (align == 3 || align == 5)
            align = 0;
        // Treat middle as center
        else if (align == 4)
            align = 1;
        
        align2--;
        if (align2 < 0)
            align2 = 0;
        align /= 2f;
        align2 /= 2f;
        
        if ((flag & 1) == 1)
            visibility = 1;
        //System.out.println(String.format("%d %d %s", visibility, flag, value));
        
        DXFAttrib e = new DXFAttrib(x, y, z, value.trim(), rotation, thickness, height, align, align2, style, c, l, angle, zoomfactor, visibility, lineType);
        e.setType(GeometryType.POINT);
        e.setUnivers(univers);
        return e;
    }

    @Override
    public DXFEntity clone() {
        return new DXFAttrib(this);
    }
}
