package org.geotools.database;


import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;

import java.io.IOException;

public abstract class AbstractFileDataStore extends AbstractDataStore implements FileDataStore {
    public AbstractFileDataStore() {
    }

    public SimpleFeatureType getSchema() throws IOException {
        return this.getSchema(this.getTypeNames()[0]);
    }

    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader() throws IOException {
        return this.getFeatureReader(this.getTypeNames()[0]);
    }

    public void updateSchema(SimpleFeatureType featureType) throws IOException {
        this.updateSchema(this.getSchema().getTypeName(), featureType);
    }

    public SimpleFeatureSource getFeatureSource() throws IOException {
        return this.getFeatureSource(this.getSchema().getTypeName());
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Filter filter, Transaction transaction) throws IOException {
        return this.getFeatureWriter(this.getSchema().getTypeName(), filter, transaction);
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(Transaction transaction) throws IOException {
        return this.getFeatureWriter(this.getSchema().getTypeName(), transaction);
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(Transaction transaction) throws IOException {
        return this.getFeatureWriterAppend(this.getSchema().getTypeName(), transaction);
    }
}
