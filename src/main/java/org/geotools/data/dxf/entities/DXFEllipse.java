package org.geotools.data.dxf.entities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import java.io.EOFException;
import java.io.IOException;


import java.util.ArrayList;
import java.util.List;
import org.geotools.data.GeometryType;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.header.DXFTables;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFParseException;

public class DXFEllipse extends DXFEntity {
    public DXFPoint _point1 = new DXFPoint();
    public DXFPoint _point2 = new DXFPoint();
    public DXFPoint _centre = new DXFPoint();
    public double _r = 0;
    public double _rotation = 0;
    public double _ratio = 0;
    public double _start = 0;
    public double _end = 0;

    public DXFEllipse(DXFEllipse newEllipse) {
        this(new DXFPoint(newEllipse._point1._point.x, newEllipse._point1._point.y, newEllipse.getColor(), null, 0, (double) newEllipse.getThickness()),
                new DXFPoint(newEllipse._point2._point.x, newEllipse._point2._point.y, newEllipse.getColor(), null, 0, (double) newEllipse.getThickness()),
                newEllipse._ratio, newEllipse._start, newEllipse._end, newEllipse.getColor(), newEllipse.getRefLayer(), newEllipse.visibility, newEllipse.getLineType());

        setType(newEllipse.getType());
        setUnivers(newEllipse.getUnivers());
    }

    public DXFEllipse(DXFPoint point1, DXFPoint point2, double r, double s, double e, int c, DXFLayer l, int visibility, DXFLineType typeLine) {
        super(c, l, visibility, typeLine, DXFTables.defaultThickness);
        _centre = point1;
        _r = Math.sqrt(Math.pow(point2._point.x, 2) + Math.pow(point2._point.y, 2));
        _rotation = Math.atan2(point2._point.y, point2._point.x);
        _ratio = r;
        _end = e;
        _start = s;
    }

    public static DXFEllipse read(DXFLineNumberReader br, DXFUnivers univers) throws NumberFormatException, IOException {
        int visibility = 0, c = 0;
        double x = 0, y = 0, x1 = 0, y1 = 0, r = 0, s = 0, e = 0;
        DXFLayer l = null;
        DXFLineType lineType = null;

        DXFCodeValuePair cvp = null;
        DXFGroupCode gc = null;

        boolean doLoop = true;
        while (doLoop) {
            cvp = new DXFCodeValuePair();
            try {
                gc = cvp.read(br);
            } catch (DXFParseException ex) {
                throw new IOException("DXF parse error" + ex.getLocalizedMessage());
            } catch (EOFException eofe) {
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
                case LINETYPE_NAME: //"6"
                    lineType = univers.findLType(cvp.getStringValue());
                    break;
                case VISIBILITY: //"60"
                    visibility = cvp.getShortValue();
                    break;
                case COLOR: //"62"
                    c = cvp.getShortValue();
                    break;
                case DOUBLE_1: //"40"
                    r = cvp.getDoubleValue();
                    break;
                case DOUBLE_2: //"41"
                    s = cvp.getDoubleValue();
                    break;
                case DOUBLE_3: //"42"
                    e = cvp.getDoubleValue();
                    break;
                case X_1: //"10"
                    x = cvp.getDoubleValue();
                    break;
                case Y_1: //"20"
                    y = cvp.getDoubleValue();
                    break;
                case X_2: //"11"
                    x1 = cvp.getDoubleValue();
                    break;
                case Y_2: //"21"
                    y1 = cvp.getDoubleValue();
                    break;
                default:
                    break;
            }

        }
        
        DXFEllipse m = new DXFEllipse(
                new DXFPoint(x, y, c, l, visibility, 1),
                new DXFPoint(x1, y1, c, l, visibility, 1),
                r, s, e, c, l, visibility, lineType);
        m.setType(GeometryType.POLYGON);
        m.setUnivers(univers);
        return m;
    }

    public Coordinate[] toCoordinateArray() {

        /*
         * This functions returns an array containing 36 points to draw an
         * ellipse.
         *
         * @param x {double} X coordinate
         * @param y {double} Y coordinate
         * @param a {double} Semimajor axis
         * @param b {double} Semiminor axis
         * @param angle {double} Angle of the ellipse
        
        function calculateEllipse(x, y, a, b, angle, steps) 
        {
        if (steps == null)
        steps = 36;
        var points = [];
        
        // Angle is given by Degree Value
        var beta = -angle * (Math.PI / 180); //(Math.PI/180) converts Degree Value into Radiance
        var sinbeta = Math.sin(beta);
        var cosbeta = Math.cos(beta);
        
        for (var i = 0; i < 360; i += 360 / steps) 
        {
        var alpha = i * (Math.PI / 180) ;
        var sinalpha = Math.sin(alpha);
        var cosalpha = Math.cos(alpha);
        
        var X = x + (a * cosalpha * cosbeta - b * sinalpha * sinbeta);
        var Y = y + (a * cosalpha * sinbeta + b * sinalpha * cosbeta);
        
        points.push(new OpenLayers.Geometry.Point(X, Y));
        }
        
        return points;
        }
         */
        List<Coordinate> lc = new ArrayList<Coordinate>();
        
        if (Math.abs(_start - _end) < 0.5 || _start >= _end)
            _start -= Math.PI * 2;

        double sinth = Math.sin(_rotation);
        double costh = Math.cos(_rotation);
        double a = _r;
        double b = _r * _ratio;
        
        //_start = 0;
        //_end = Math.PI * 2;
        
        for (double angle = _start; angle < _end; angle += (_end - _start) / 18) {
            double sina = Math.sin(angle);
            double cosa = Math.cos(angle);
            Coordinate c = new Coordinate(
                    _centre.X() + (a * cosa * costh - b * sina * sinth),
                    _centre.Y() + (a * cosa * sinth + b * sina * costh));
            lc.add(c);
        }

        // Fix to avoid error while creating LinearRing
        // If first coord is not equal to last coord
        if (_end - _start == Math.PI * 2 && !lc.get(0).equals(lc.get(lc.size() - 1))) {
            // Set last coordinate == first
            lc.set(lc.size() - 1, lc.get(0));
        }
        
        if (_end - _start == Math.PI * 2) {
            setType(GeometryType.POLYGON);
        } else {
            setType(GeometryType.LINE);
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
        if (ca != null && ca.length > 2) {
            if (getType() == GeometryType.POLYGON) {
                LinearRing lr = getUnivers().getGeometryFactory().createLinearRing(ca);
                geometry = getUnivers().getGeometryFactory().createPolygon(lr, null);
            }
            else {
                geometry = getUnivers().getGeometryFactory().createLineString(ca);                
            }
        } else {

        }
    }

    public String toString(int visibility, int c, double r, double t, double e, double x, double y, double x1, double y1) {
        StringBuffer s = new StringBuffer();
        s.append("DXFEllipse [");
        s.append("visibility: ");
        s.append(visibility + ", ");
        s.append("color: ");
        s.append(c + ", ");
        s.append("r: ");
        s.append(r + ", ");
        s.append("s: ");
        s.append(t + ", ");
        s.append("e: ");
        s.append(e + ", ");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y + ", ");
        s.append("x1: ");
        s.append(x1 + ", ");
        s.append("y1: ");
        s.append(y1);
        s.append("]");
        return s.toString();
    }

    @Override
    public DXFEntity translate(double x, double y) {
        // Is Translation of centre necessary?
        _centre._point.x += x;
        _centre._point.y += y;
        return this;
    }

    @Override
    public DXFEntity clone() {
        return new DXFEllipse(this);
    }
}
