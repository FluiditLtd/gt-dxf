package org.geotools.data.dxf.parser;

import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

public class DXFLineNumberReader extends LineNumberReader {
    private static final int MARK_SIZE = 2 * 2049;

    public DXFLineNumberReader(Reader r) {
        super(r);
    }

    @Override
    public String readLine() throws IOException {
        String value;

        value = super.readLine();

        if (value == null) {
            throw new EOFException();
        }
        return value.trim();
    }

    public void mark() throws IOException {
        this.mark(MARK_SIZE);
    }
}

