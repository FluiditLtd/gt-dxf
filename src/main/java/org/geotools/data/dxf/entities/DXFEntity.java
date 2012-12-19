package org.geotools.data.dxf.entities;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.algorithm.Angle;
import org.geotools.data.GeometryType;
import org.geotools.data.dxf.header.DXFBlockReference;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.header.DXFLineType;
import org.geotools.data.dxf.parser.DXFColor;
import org.geotools.data.dxf.parser.DXFConstants;
import org.geotools.data.dxf.parser.DXFUnivers;


public abstract class DXFEntity implements DXFConstants {
    protected GeometryType geometryType;
    protected Geometry geometry = null;
    /* dxf read */
    protected DXFUnivers univers;
    protected DXFLineType _lineType;
    protected int _color = -1;
    protected DXFLayer _refLayer;
    protected double _thickness;
    protected int visibility = 0;
    private double _entRotationAngle = 0.0;
    protected double _xScale = 1, _yScale = 1, _zScale = 1;
    protected Coordinate _entBase = new Coordinate(0.0, 0.0, 0.0);

    /**
     * Copy constructor.
     * 
     * @param newEntity 
     */
    public DXFEntity(DXFEntity newEntity) {
        this(newEntity.getColor(), newEntity.getRefLayer(), newEntity.visibility, newEntity.getLineType(), newEntity.getThickness());
        _xScale = newEntity._xScale;
        _yScale = newEntity._yScale;
        _zScale = newEntity._zScale;
    }

    /**
     * Creates new entity.
     * 
     * @param color color
     * @param layer layer of the entity
     * @param visibility 0 if entity is visible, 1 otherwise
     * @param lineType line type
     * @param thickness line thickness
     */
    public DXFEntity(int color, DXFLayer layer, int visibility, DXFLineType lineType, double thickness) {

        _refLayer = layer;

        if (lineType != null && lineType._name.equalsIgnoreCase("BYLAYER") && _refLayer != null) {
            //lineType = _refLayer.getLineType();
        }
        _lineType = lineType;

        if (!(this instanceof DXFBlockReference) && !(this instanceof DXFLayer)) {
            if ((color < 0) || (color == 256 && _refLayer != null)) {
                if (_refLayer == null) {
                    color = DXFColor.getDefaultColorIndex();
                } else {
                    color = _refLayer._color;
                }
            }
        }
        _color = color;
        _thickness = thickness;
        this.visibility = visibility;
    }

    public void setBase(Coordinate coord) {
        this._entBase = coord;
    }

    public Coordinate getBase() {
        return _entBase;        
    }
    
    public void setAngle(double angle) {
        this._entRotationAngle = angle;
    }
    
    public double getAngle() {
        return _entRotationAngle;
    }

    public void setScale(double x, double y, double z) {
        _xScale = x;
        _yScale = y;
        _zScale = z;
    }
    
    public double getXScale() {
        return _xScale;
    }
    
    public double getYScale() {
        return _yScale;
    }
    
    public double getZScale() {
        return _zScale;
    }
    
    abstract public DXFEntity translate(double x, double y, double z);

    protected Coordinate rotateAndPlace(Coordinate coord) {
        Coordinate[] array = new Coordinate[1];
        array[0] = coord;

        return rotateAndPlace(array)[0];
    }

    protected Coordinate[] rotateAndPlace(Coordinate[] coordarray) {
        for (int i = 0; i < coordarray.length; i++) {
            coordarray[i] = rotateCoordDegrees(coordarray[i], _entRotationAngle);
            coordarray[i].x = _xScale * coordarray[i].x + _entBase.x;
            coordarray[i].y = _yScale * coordarray[i].y + _entBase.y;
            coordarray[i].z = _zScale * coordarray[i].z + _entBase.z;
        }
        return coordarray;
    }

    private Coordinate rotateCoordDegrees(Coordinate coord, double angle) {
        angle = Angle.toRadians(angle);
        angle = Angle.angle(coord) + angle;

        Coordinate newCoord = new Coordinate(coord);
        double radius = Math.sqrt(Math.pow(coord.x, 2) + Math.pow(coord.y, 2));

        newCoord.x = radius * Math.cos(angle);
        newCoord.y = radius * Math.sin(angle);

        return newCoord;
    }

    @Override
    abstract public DXFEntity clone();

    public Geometry getGeometry() {
        if (geometry == null) {
            updateGeometry();
        }
        return geometry;
    }

    public void updateGeometry() {
        geometry = getUnivers().getErrorGeometry();
    }

    public GeometryType getType() {
        return geometryType;
    }

    public void setType(GeometryType geometryType) {
        this.geometryType = geometryType;
    }

    public void setVisible(boolean visible) {
        if (visible)
            this.visibility = 0;
        else
            this.visibility = 1;
    }

    public boolean isVisible() {
        return (visibility & 1) == 0;
    }

    public DXFLineType getLineType() {
        return _lineType;
    }

    public String getLineTypeName() {
        if (_lineType == null) {
            return DXFLineType.DEFAULT_NAME;
        }
        return _lineType._name;
    }

    public void setLineType(DXFLineType lineType) {
        this._lineType = lineType;
    }

    public int getColor() {
        return _color;
    }

    public String getColorRGB() {
        return DXFColor.getColorRGB(_color);
    }

    public void setColor(int color) {
        this._color = color;
    }

    public DXFLayer getRefLayer() {
        return _refLayer;
    }

    public String getRefLayerName() {
        if (_refLayer == null) {
            return DXFLayer.DEFAULT_NAME;
        }
        return _refLayer.getName();
    }

    public void setRefLayer(DXFLayer refLayer) {
        this._refLayer = refLayer;
    }

    public double getThickness() {
        return _thickness;
    }

    public void setThickness(double thickness) {
        this._thickness = thickness;
    }

    public DXFUnivers getUnivers() {
        return univers;
    }

    public void setUnivers(DXFUnivers univers) {
        this.univers = univers;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
