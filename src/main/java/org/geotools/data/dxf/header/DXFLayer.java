package org.geotools.data.dxf.header;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;


import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.dxf.entities.DXFEntity;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFConstants;
import org.geotools.data.dxf.parser.DXFGroupCode;

public class DXFLayer extends DXFEntity implements DXFConstants {
    public static final String DEFAULT_NAME = "default";
    public int _flag = 0;
    public String _name;
    public ArrayList<DXFEntity> theEnt = new ArrayList<DXFEntity>();

    public DXFLayer(String nom, int c) {
        super(c, null, 0, null, DXFTables.defaultThickness);
        setName(nom);
    }

    public DXFLayer(String nom, int c, int flag) {
        super(c, null, 0, null, DXFTables.defaultThickness);
        setName(nom);
        _flag = flag;
        setVisible((flag & 1) == 0);
    }

    public void setName(String name) {
        this._name = name;
    }
    
    public String getName() {
        return this._name;
    }
    
    public static DXFLayer read(DXFLineNumberReader br) throws NumberFormatException, IOException {
        String name = "";
        int f = 0, color = 0;

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
                case VARIABLE_NAME:
                    br.reset();
                    doLoop = false;
                    break;
                case NAME:
                    name = cvp.getStringValue();
                    break;
                case COLOR:
                    color = cvp.getShortValue();
                    break;
                case INT_1:
                    f = cvp.getShortValue();
                    break;
                default:
            }
        }

        if (color < 0)
            f = f | 1;        
        DXFLayer l = new DXFLayer(name, color, f);
        l.setVisible((f & 1) == 0);
        return l;
    }

    public String toString(String name, int color, int f) {
        StringBuilder s = new StringBuilder();
        s.append("DXFLayer [");
        s.append("name: ");
        s.append(name + ", ");
        s.append("color: ");
        s.append(color + ", ");
        s.append("f: ");
        s.append(f);
        s.append("]");
        return s.toString();
    }

    public DXFEntity clone(){
        return this;
    }
}

