/*0
 * $Id: DXFDataStoreFactory.java Matthijs $
 */

package org.geotools.data.dxf;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public class DXFDataStoreFactory implements FileDataStoreFactorySpi {
    public static final DataStoreFactorySpi.Param PARAM_INPUT_STREAM = new DataStoreFactorySpi.Param("stream", InputStream.class, "input stream of a .dxf file");
    public static final DataStoreFactorySpi.Param PARAM_URL = new DataStoreFactorySpi.Param("url", URL.class, "url to a .dxf file");
    public static final DataStoreFactorySpi.Param PARAM_SRS = new DataStoreFactorySpi.Param("srs", String.class, "srs for the .dxf file");
    public static final DataStoreFactorySpi.Param PARAM_TARGET_SRS = new DataStoreFactorySpi.Param("target srs", String.class, "target srs; optional; used for converting text rotation angles");
    public static final DataStoreFactorySpi.Param PARAM_AFFINE_TRANSFORM = new DataStoreFactorySpi.Param("transform", AffineTransform.class, "affine transform performed to the geometries");

    public DXFDataStoreFactory() {

    }

    public String getDisplayName() {
        return "DXF File";
    }

    public String getDescription() {
        return "Autodesk DXF format";
    }

    public String[] getFileExtensions() {
        return new String[] {".dxf", ".dxf.gz", ".dxf.zip"};
    }

    /**
     * @return true if stream is not empty
     */
    public boolean canProcess(InputStream stream) throws IOException {
       return stream.available() > 0;
    }

    /**
     * @return true if the file of the f parameter exists
     */
    public boolean canProcess(URL f) {
        String name = f.getFile().toLowerCase();
        return name.endsWith(".dxf") || name.endsWith(".dxf.zip") || name.endsWith(".dxf.gz");
    }

    /**
     * @return true if srs can be resolved
     */
    public boolean canProcess(String srs) throws NoSuchAuthorityCodeException, FactoryException {
        return CRS.decode(srs, true) != null;
    }

    /**
     * @return true if the file in the url param exists
     */
    public boolean canProcess(Map params) {
        boolean result = false;
        if (params.containsKey(PARAM_INPUT_STREAM.key)) {
            try {
                InputStream stream = (InputStream)PARAM_INPUT_STREAM.lookUp(params);
                result = canProcess(stream);
            } catch (IOException ioe) {
                result = false;
            }
        }
        if (params.containsKey(PARAM_URL.key)) {
            try {
                URL url = (URL)PARAM_URL.lookUp(params);
                result = canProcess(url);
            } catch (IOException ioe) {
                result = false;
            }
        }
        if (result && params.containsKey(PARAM_SRS.key)) {
            try {
                String srs = (String) PARAM_SRS.lookUp(params);
                result = canProcess(srs);
            } catch (NoSuchAuthorityCodeException ex) {
                result = false;
            } catch (FactoryException ex) {
                result = false;
            } catch (IOException ioe) {
                result = false;
            }
        }
        else
            result = false;

        if (result && params.containsKey(PARAM_TARGET_SRS.key)) {
            try {
                String srs = (String) PARAM_TARGET_SRS.lookUp(params);
                result = canProcess(srs);
            } catch (NoSuchAuthorityCodeException ex) {
                result = false;
            } catch (FactoryException ex) {
                result = false;
            } catch (IOException ioe) {
                result = false;
            }
        }
        return result;
    }

    /*
     * Always returns true, no additional libraries needed
     */
    public boolean isAvailable() {
        return true;
    }

    public DataStoreFactorySpi.Param[] getParametersInfo() {
        return new DataStoreFactorySpi.Param[] {PARAM_URL, PARAM_SRS, PARAM_TARGET_SRS, PARAM_AFFINE_TRANSFORM};
    }

    public Map getImplementationHints() {
        /* XXX do we need to put something in this map? */
        return Collections.EMPTY_MAP;
    }

    public String getTypeName(URL url) throws IOException {
        return DXFDataStore.getURLTypeName(url);
    }

    public FileDataStore createDataStore(URL url) throws IOException {
        Map params = new HashMap();
        params.put(PARAM_URL.key, url);

        boolean isLocal = url.getProtocol().equalsIgnoreCase("file");
        if(isLocal && !(new File(url.getFile()).exists())){
            throw new UnsupportedOperationException("Specified DXF file \"" + url + "\" does not exist, this plugin is read-only so no new file will be created");
        } else {
            return createDataStore(params);
        }
    }

    public FileDataStore createDataStore(InputStream stream) throws IOException {
        Map params = new HashMap();
        params.put(PARAM_INPUT_STREAM.key, stream);

        return createDataStore(params);
    }

    public FileDataStore createDataStore(Map params) throws IOException {
        if(!canProcess(params)) {
            throw new FileNotFoundException( "DXF file not found: " + params);
        }
        if (params.containsKey(PARAM_INPUT_STREAM.key)) {
            return new DXFDataStore(null, (InputStream)params.get(PARAM_INPUT_STREAM.key), (String)params.get(PARAM_SRS.key), (String)params.get(PARAM_TARGET_SRS.key), (AffineTransform)params.get(PARAM_AFFINE_TRANSFORM.key));
        } else {
            return new DXFDataStore((URL)params.get(PARAM_URL.key), null, (String)params.get(PARAM_SRS.key), (String)params.get(PARAM_TARGET_SRS.key), (AffineTransform)params.get(PARAM_AFFINE_TRANSFORM.key));
        }
    }

    public DataStore createNewDataStore(Map params) throws IOException {
        throw new UnsupportedOperationException("This plugin is read-only");
    }
}
