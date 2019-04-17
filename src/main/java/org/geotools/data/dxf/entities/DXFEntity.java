package org.geotools.data.dxf.entities;

import org.geotools.data.dxf.header.DXFBlockReference;
import org.geotools.data.dxf.header.DXFLayer;
import org.geotools.data.dxf.parser.DXFCodeValuePair;
import org.geotools.data.dxf.parser.DXFColor;
import org.geotools.data.dxf.parser.DXFConstants;
import org.geotools.data.dxf.parser.DXFGroupCode;
import org.geotools.data.dxf.parser.DXFLineNumberReader;
import org.geotools.data.dxf.parser.DXFParseException;
import org.geotools.data.dxf.parser.DXFUnivers;
import org.geotools.database.GeometryType;
import org.geotools.data.dxf.header.DXFLineType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.algorithm.Angle;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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
    protected Map<String, List<String>> xdata;
    

    /**
     * Copy constructor.
     * 
     * @param newEntity 
     */
    public DXFEntity(DXFEntity newEntity) {
        this(newEntity.getColor(), newEntity.getRefLayer(), newEntity.visibility, newEntity.getLineType(), newEntity.getThickness());
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
    
    protected Coordinate rotateAndPlace(Coordinate coord) {
        Coordinate[] array = new Coordinate[1];
        array[0] = coord;

        return rotateAndPlace(array)[0];
    }

    protected Coordinate[] rotateAndPlace(Coordinate[] coordarray) {
        /*for (int i = 0; i < coordarray.length; i++) {
            coordarray[i] = rotateCoordDegrees(coordarray[i], _entRotationAngle);
            coordarray[i].x = _xScale * coordarray[i].x + _entBase.x;
            coordarray[i].y = _yScale * coordarray[i].y + _entBase.y;
            coordarray[i].z = _zScale * coordarray[i].z + _entBase.z;
        }*/
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
        updateGeometry();
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
    
    public int getActualColor(int insertColor) {
        if (_color == 0 && insertColor != -1)
            return insertColor;
        else if (_color == -1 || _color == 256) {
            if (_refLayer != null)
                return Math.max(1, _refLayer._color);
            else
                return 1;
        }
        else
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
    
    public Map<String, List<String>> getXData() {
        return xdata;
    }
    
    public void setXData(Map<String, List<String>> xdata) {
        this.xdata = xdata;
    }
    
    protected static Map<String, List<String>> addXdata(String application, String data, Map<String, List<String>> xdata) {
        if (xdata == null)
            xdata = new HashMap<String, List<String>>();
        
        if (!xdata.containsKey(application)) {
            List<String> list = new LinkedList<String>();
            xdata.put(application, list);
            list.add(data);
        }
        else
            xdata.get(application).add(data);
        
        return xdata;
    }
    
    public static Map<String, List<String>> readXdata(String application, DXFLineNumberReader br, DXFUnivers univers, Map<String, List<String>> xdata) throws IOException {
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
                case XDATA_CONTROL_STRING:
                    if (cvp.getStringValue().trim().equals("}"))
                        doLoop = false;
                    break;
                case XDATA_ASCII_STRING:
                case XDATA_CHUNK_OF_BYTES:
                case XDATA_LAYER_NAME:
                    xdata = addXdata(application, cvp.getStringValue(), xdata);
                    break;
                case XDATA_INT16:
                    xdata = addXdata(application, Short.toString(cvp.getShortValue()), xdata);
                    break;
                case XDATA_INT32:
                    xdata = addXdata(application, Integer.toString(cvp.getIntValue()), xdata);
                    break;
                case XDATA_DOUBLE:
                case XDATA_X_1:
                case XDATA_X_2:
                case XDATA_X_3:
                case XDATA_X_4:
                case XDATA_Y_1:
                case XDATA_Y_2:
                case XDATA_Y_3:
                case XDATA_Y_4:
                case XDATA_Z_1:
                case XDATA_Z_2:
                case XDATA_Z_3:
                case XDATA_Z_4:
                case XDATA_SCALE_FACTOR:
                    xdata = addXdata(application, Double.toString(cvp.getDoubleValue()), xdata);
                    break;
                default:
                    br.reset();
                    doLoop = false;
                    break;
            }
        }
        return xdata;
    }
}
