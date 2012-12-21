/*
 * [ 1719398 ] First shot at LWPOLYLINE
 * Peter Hopfgartner - hopfgartner
 *  
 */
package org.geotools.data.dxf.entities;

import java.awt.geom.Point2D;

public class DXFLwVertex extends DXFPoint {
    /*
     * The bulge is the tangent of 1/4 of the included angle for the arc 
     * between the selected vertex and the next vertex in the polyline's 
     * vertex list. A negative bulge value indicates that the arc goes 
     * clockwise from the selected vertex to the next vertex. A bulge of 
     * 0 indicates a straight segment, and a bulge of 1 is a semicircle.
     * 
     * http://www.afralisp.net/lisp/Bulges1.htm
     */
    double _bulge;

    public DXFLwVertex(Point p, double bulge) {
        super(p);
        this._bulge = bulge;
    }

    public DXFLwVertex(double x, double y, double z, double bulge) {
        super(new Point(x, y, z));
        this._bulge = bulge;
    }

    public DXFLwVertex(DXFLwVertex orig, boolean bis) {
        super(orig._point.x, orig._point.y, orig._point.z, orig.getColor(), orig.getRefLayer(), 0, 1);
        _bulge = orig._bulge;
    }

    public double getBulge() {
        return _bulge;
    }
    
    public String toString(double b, double x, double y) {
        StringBuilder s = new StringBuilder();
        s.append("DXFLwVertex [");
        s.append("bulge: ");
        s.append(b + ", ");
        s.append("x: ");
        s.append(x + ", ");
        s.append("y: ");
        s.append(y);
         s.append("]");
        return s.toString();
    }
    
    /*
     * Calculates the <see cref="Bulge"/> from the arc segment's 
     * center and end points.
     * <param name="center">The arc center.</param>
     * <param name="arcStart">The arc start point.</param>
     * <param name="arcEnd">The arc end point.</param>
     */
    public static double GetBulgeFromEndPoints(
            Point2D center,
            Point2D arcStart,
            Point2D arcEnd) {
        // Take v1 as the x-axis.
        Point2D v1 = new Point2D.Double(arcStart.getX() - center.getX(),arcStart.getY() - center.getY());
        Point2D yaxis = new Point2D.Double(-v1.getY(), v1.getX());

        Point2D v2 = new Point2D.Double(arcEnd.getX() - center.getX(),arcEnd.getY() - center.getY());
        // Project on v1.
        double x = v1.getX()*v2.getX()+v1.getY()*v2.getY();
        double y = yaxis.getX()*v2.getX()+yaxis.getY()*v2.getY();
 
        double angle = Math.atan2(y, x);

        return Math.tan(angle / 4d);
    }

    @Override
    public DXFEntity clone(){
        return new DXFLwVertex(this, true);
    }
}
