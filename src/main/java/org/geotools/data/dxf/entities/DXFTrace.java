package org.geotools.data.dxf.entities;

import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.io.IOException;

import org.geotools.data.GeometryType;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;

public class DXFTrace extends DXFSolid {
    public DXFTrace(DXFTrace newTrace) {
        this(new DXFPoint(newTrace._p1.X(), newTrace._p1.Y(), newTrace._p1.Z(), newTrace.getColor(), null, 0, newTrace.getThickness()),
                new DXFPoint(newTrace._p2.X(), newTrace._p2.Y(), newTrace._p2.Z(), newTrace.getColor(), null, 0, newTrace.getThickness()),
                new DXFPoint(newTrace._p3.X(), newTrace._p3.Y(), newTrace._p3.Y(), newTrace.getColor(), null, 0, newTrace.getThickness()),
                new DXFPoint(newTrace._p4.X(), newTrace._p4.Y(), newTrace._p4.Y(), newTrace.getColor(), null, 0, newTrace.getThickness()),
                newTrace.getThickness(), newTrace.getColor(), newTrace.getRefLayer(),
                newTrace.visibility, newTrace.getLineType());

        setType(newTrace.getType());
        setUnivers(newTrace.getUnivers());
    }

    public DXFTrace(DXFPoint p1, DXFPoint p2, DXFPoint p3, DXFPoint p4, double thickness, int c, DXFLayer l, int visibility, DXFLineType lineType) {
        super(p1, p2, p3, p4, thickness, c, l, visibility, lineType);
    }

    public static DXFEntity read(DXFLineNumberReader br, DXFUnivers univers) throws IOException {
        int visibility = 0;
        DXFSolid s = (DXFSolid) DXFSolid.read(br, univers);
        if (!s.isVisible()) {
            visibility = 1;
        }
        DXFTrace e = new DXFTrace(s._p1, s._p2, s._p3, s._p4, s.getThickness(), s.getColor(), s.getRefLayer(), visibility, s.getLineType());
        e.setType(GeometryType.UNSUPPORTED);
        e.setUnivers(univers);
        return e;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("DXFTrace [");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append(": ");
        s.append(", ");
        s.append("]");
        return s.toString();
    }

}
