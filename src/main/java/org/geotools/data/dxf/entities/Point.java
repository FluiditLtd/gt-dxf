/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotools.data.dxf.entities;

/**
 *
 * @author SUNELMA
 */
public class Point {
    public double x;
    public double y;
    public double z;
    
    public Point() {
        
    }
    
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
