/*
 * $Id: DXFParseException.java Matthijs $
 */

package org.geotools.data.dxf.parser;

import org.geotools.data.dxf.entities.DXFEntity;

/**
 * Exception thrown while parsing a SDL file, adds line number in front of
 * specified message.
 *
 * @author Matthijs Laan, B3Partners
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/build/maven/javadoc/../../../modules/unsupported/dxf/src/main/java/org/geotools/data/dxf/parser/DXFParseException.java $
 */
public class DXFParseException extends Exception {
    private String message;

    public DXFParseException(DXFLineNumberReader reader, String message) {
        super();
        this.message = "line " + reader.getLineNumber() + ": " + message;
    }

    public DXFParseException(DXFEntity entry, String message) {
        super();
        this.message = "error: " + message;
    }

    public DXFParseException(DXFEntity entry, String message, Exception cause) {
        super(cause);
        this.message = "error: " + message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
