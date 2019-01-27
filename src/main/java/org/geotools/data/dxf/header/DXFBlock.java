package org.geotools.data.dxf.header;

import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;
import java.util.Iterator;


import org.geotools.data.dxf.parser.DXFColor;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.dxf.entities.DXFEntity;
import org.geotools.data.dxf.entities.DXFPoint;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFConstants;
import org.geotools.data.dxf.parser.DXFGroupCode;

public class DXFBlock extends DXFEntity implements DXFConstants {
    public Vector<DXFEntity> theEntities = new Vector<DXFEntity>();
    public DXFPoint _point = new DXFPoint();
    public String _name;
    public int _flag;
    public double _xs, _ys, _zs;

    public DXFBlock(DXFBlock newBlock) {
        this(newBlock._point.X(), newBlock._point.Y(), newBlock._point.Z(), newBlock._flag, newBlock._name, null, newBlock.getColor(), newBlock.getRefLayer());
        _xs = newBlock._xs;
        _ys = newBlock._ys;
        _zs = newBlock._zs;
        
        // Copy entities
        Iterator iter = newBlock.theEntities.iterator();
        while (iter.hasNext()) {
            theEntities.add(((DXFEntity) iter).clone());
        }
    }

    public DXFBlock(double x, double y, double z, int flag, String name, Vector<DXFEntity> ent, int c, DXFLayer l) {
        super(c, l, 0, null, DXFTables.defaultThickness);
        _point = new DXFPoint(x, y, z, c, l, 0, 1);
        _name = name;
        _flag = flag;

        if (ent == null) {
            ent = new Vector<DXFEntity>();
        }
        theEntities = ent;
    }

    public static DXFBlock read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        Vector<DXFEntity> sEnt = new Vector<DXFEntity>();
        String name = "";
        double x = 0, y = 0, z = 0;
        double xscale = 1, yscale = 1, zscale = 1;
        int flag = 0;
        DXFLayer l = null;

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
                    if (type.equals(ENDBLK)) {
                        doLoop = false;
                    } else if (type.equals(ENDSEC)) {
                        // hack voor als ENDBLK ontbreekt
                        doLoop = false;
                        br.reset();
                    } else if (type.equals(BLOCK)) {
                        doLoop = false;
                        br.reset();
                    //} else if (type.equals(INSERT)) {
                    //    DXFInsert.read(br, univers);
                    } else {
                        // check of dit entities zijn
                        br.reset();
                        sEnt.addAll(DXFEntities.readEntities(br, univers).theEntities);
                    }
                    break;
                case LAYER_NAME:
                    l = univers.findLayer(cvp.getStringValue());
                    break;
                case NAME:
                    name = cvp.getStringValue();
                    break;
                case INT_1:
                    flag = cvp.getShortValue();
                    break;
                case X_1:
                    x = cvp.getDoubleValue();
                    break;
                case Y_1:
                    y = cvp.getDoubleValue();
                    break;
                case Z_1:
                    z = cvp.getDoubleValue();
                    break;
                case DOUBLE_2:
                    xscale = cvp.getDoubleValue();
                    break;
                case DOUBLE_3:
                    yscale = cvp.getDoubleValue();
                    break;
                case DOUBLE_4:
                    zscale = cvp.getDoubleValue();
                    break;
                default:
                    break;
            }
        }
        
        DXFBlock e = new DXFBlock(x, y, z, flag, name, sEnt, DXFColor.getDefaultColorIndex(), l);
        e._xs = xscale;
        e._ys = yscale;
        e._zs = zscale;
        return e;
    }

    public String toString(double x, double y, int flag, String name, int numEntities, int c) {
        StringBuilder s = new StringBuilder();
        s.append("DXFBlock [");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y + ", ");
        s.append("flag: ");
        s.append(flag + ", ");
        s.append("name: ");
        s.append(name + ", ");
        s.append("color: ");
        s.append(c + ", ");
        s.append("numEntities: ");
        s.append(numEntities);
        s.append("]");
        return s.toString();
    }

    public DXFEntity clone() {
        return new DXFBlock(this);
    }
}
