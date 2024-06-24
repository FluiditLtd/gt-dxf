package org.geotools.database;


import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureListener;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.LockingManager;
import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultServiceInfo;
import org.geotools.data.Diff;
import org.geotools.data.DiffFeatureReader;
import org.geotools.data.EmptyFeatureReader;
import org.geotools.data.EmptyFeatureWriter;
import org.geotools.data.FeatureListenerManager;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.FilteringFeatureWriter;
import org.geotools.data.InProcessLockingManager;
import org.geotools.data.MaxFeatureReader;
import org.geotools.data.ReTypeFeatureReader;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractDataStore implements DataStore {
    protected static final Logger LOGGER = Logging.getLogger("org.geotools.data");
    public FeatureListenerManager listenerManager;
    protected final boolean isWriteable;
    private InProcessLockingManager lockingManager;

    public AbstractDataStore() {
        this(true);
    }

    public AbstractDataStore(boolean isWriteable) {
        this.listenerManager = new FeatureListenerManager();
        this.isWriteable = isWriteable;
        this.lockingManager = this.createLockingManager();
    }

    protected InProcessLockingManager createLockingManager() {
        return new InProcessLockingManager();
    }

    protected Map createMetadata(String typeName) {
        return Collections.EMPTY_MAP;
    }

    public abstract String[] getTypeNames() throws IOException;

    public ServiceInfo getInfo() {
        DefaultServiceInfo info = new DefaultServiceInfo();
        info.setDescription("Features from " + this.getClass().getSimpleName());
        info.setSchema(FeatureTypes.DEFAULT_NAMESPACE);
        return info;
    }

    public abstract SimpleFeatureType getSchema(String var1) throws IOException;

    protected abstract FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String var1) throws IOException;

    protected FeatureWriter<SimpleFeatureType, SimpleFeature> createFeatureWriter(String typeName, Transaction transaction) throws IOException {
        throw new UnsupportedOperationException("FeatureWriter not supported");
    }

    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Schema creation not supported");
    }

    public void updateSchema(String typeName, SimpleFeatureType featureType) {
        throw new UnsupportedOperationException("Schema modification not supported");
    }

    public SimpleFeatureSource getFeatureSource(final String typeName) throws IOException {
        final SimpleFeatureType featureType = this.getSchema(typeName);
        if (this.isWriteable) {
            return (SimpleFeatureSource)(this.lockingManager != null ? new AbstractFeatureLocking(this.getSupportedHints()) {
                public DataStore getDataStore() {
                    return AbstractDataStore.this;
                }

                public String toString() {
                    return "AbstractDataStore.AbstractFeatureLocking(" + typeName + ")";
                }

                public void addFeatureListener(FeatureListener listener) {
                    AbstractDataStore.this.listenerManager.addFeatureListener(this, listener);
                }

                public void removeFeatureListener(FeatureListener listener) {
                    AbstractDataStore.this.listenerManager.removeFeatureListener(this, listener);
                }

                public SimpleFeatureType getSchema() {
                    return featureType;
                }
            } : new AbstractFeatureStore(this.getSupportedHints()) {
                public DataStore getDataStore() {
                    return AbstractDataStore.this;
                }

                public String toString() {
                    return "AbstractDataStore.AbstractFeatureStore(" + typeName + ")";
                }

                public void addFeatureListener(FeatureListener listener) {
                    AbstractDataStore.this.listenerManager.addFeatureListener(this, listener);
                }

                public void removeFeatureListener(FeatureListener listener) {
                    AbstractDataStore.this.listenerManager.removeFeatureListener(this, listener);
                }

                public SimpleFeatureType getSchema() {
                    return featureType;
                }
            });
        } else {
            return new AbstractFeatureSource(this.getSupportedHints()) {
                public DataStore getDataStore() {
                    return AbstractDataStore.this;
                }

                public String toString() {
                    return "AbstractDataStore.AbstractFeatureSource(" + typeName + ")";
                }

                public void addFeatureListener(FeatureListener listener) {
                    AbstractDataStore.this.listenerManager.addFeatureListener(this, listener);
                }

                public void removeFeatureListener(FeatureListener listener) {
                    AbstractDataStore.this.listenerManager.removeFeatureListener(this, listener);
                }

                public SimpleFeatureType getSchema() {
                    return featureType;
                }
            };
        }
    }

    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction) throws IOException {
        Filter filter = query.getFilter();
        String typeName = query.getTypeName();
        String[] propertyNames = query.getPropertyNames();
        if (filter == null) {
            throw new NullPointerException("getFeatureReader requires Filter: did you mean Filter.INCLUDE?");
        } else if (typeName == null) {
            throw new NullPointerException("getFeatureReader requires typeName: use getTypeNames() for a list of available types");
        } else if (transaction == null) {
            throw new NullPointerException("getFeatureReader requires Transaction: did you mean to use Transaction.AUTO_COMMIT?");
        } else {
            SimpleFeatureType featureType = this.getSchema(query.getTypeName());
            if (propertyNames != null || query.getCoordinateSystem() != null) {
                try {
                    featureType = DataUtilities.createSubType(featureType, propertyNames, query.getCoordinateSystem());
                } catch (SchemaException var9) {
                    LOGGER.log(Level.FINEST, var9.getMessage(), var9);
                    throw new DataSourceException("Could not create Feature Type for query", var9);
                }
            }

            if (filter != Filter.EXCLUDE && !filter.equals(Filter.EXCLUDE)) {
                filter = this.getUnsupportedFilter(typeName, filter);
                if (filter == null) {
                    throw new NullPointerException("getUnsupportedFilter shouldn't return null. Do you mean Filter.INCLUDE?");
                } else {
                    Diff diff = null;
                    if (transaction != Transaction.AUTO_COMMIT) {
                        TransactionStateDiff state = this.state(transaction);
                        if (state != null) {
                            diff = state.diff(typeName);
                        }
                    }

                    FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.getFeatureReader(typeName, query);
                    if (diff != null) {
                        reader = new DiffFeatureReader((FeatureReader)reader, diff, query.getFilter());
                    }

                    if (!filter.equals(Filter.INCLUDE)) {
                        reader = new FilteringFeatureReader((FeatureReader)reader, filter);
                    }

                    if (!featureType.equals(((FeatureReader)reader).getFeatureType())) {
                        LOGGER.fine("Recasting feature type to subtype by using a ReTypeFeatureReader");
                        reader = new ReTypeFeatureReader((FeatureReader)reader, featureType, false);
                    }

                    if (query.getMaxFeatures() != 2147483647) {
                        reader = new MaxFeatureReader((FeatureReader)reader, query.getMaxFeatures());
                    }

                    return (FeatureReader)reader;
                }
            } else {
                return new EmptyFeatureReader(featureType);
            }
        }
    }

    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query) throws IOException {
        return this.getFeatureReader(typeName);
    }

    protected Filter getUnsupportedFilter(String typeName, Filter filter) {
        return filter;
    }

    protected TransactionStateDiff state(Transaction transaction) {
        synchronized(transaction) {
            TransactionStateDiff state = (TransactionStateDiff)transaction.getState(this);
            if (state == null) {
                state = new TransactionStateDiff(this);
                transaction.putState(this, state);
            }

            return state;
        }
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Filter filter, Transaction transaction) throws IOException {
        if (filter == null) {
            throw new NullPointerException("getFeatureReader requires Filter: did you mean Filter.INCLUDE?");
        } else if (filter == Filter.EXCLUDE) {
            SimpleFeatureType featureType = this.getSchema(typeName);
            return new EmptyFeatureWriter(featureType);
        } else if (transaction == null) {
            throw new NullPointerException("getFeatureWriter requires Transaction: did you mean to use Transaction.AUTO_COMMIT?");
        } else {
            Object writer;
            if (transaction == Transaction.AUTO_COMMIT) {
                try {
                    writer = this.createFeatureWriter(typeName, transaction);
                } catch (UnsupportedOperationException var6) {
                    throw var6;
                }
            } else {
                TransactionStateDiff state = this.state(transaction);
                if (state == null) {
                    throw new UnsupportedOperationException("Subclass sould implement");
                }

                writer = state.writer(typeName, filter);
            }

            if (this.lockingManager != null) {
                writer = this.lockingManager.checkedWriter((FeatureWriter)writer, transaction);
            }

            if (filter != Filter.INCLUDE) {
                writer = new FilteringFeatureWriter((FeatureWriter)writer, filter);
            }

            return (FeatureWriter)writer;
        }
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Transaction transaction) throws IOException {
        return this.getFeatureWriter(typeName, Filter.INCLUDE, transaction);
    }

    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName, Transaction transaction) throws IOException {
        FeatureWriter writer = this.getFeatureWriter(typeName, transaction);

        while(writer.hasNext()) {
            writer.next();
        }

        return writer;
    }

    public LockingManager getLockingManager() {
        return this.lockingManager;
    }

    protected ReferencedEnvelope getBounds(Query query) throws IOException {
        return null;
    }

    protected int getCount(Query query) throws IOException {
        return -1;
    }

    protected Set getSupportedHints() {
        return Collections.EMPTY_SET;
    }

    public void dispose() {
    }

    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        return this.getFeatureSource(typeName.getLocalPart());
    }

    public List<Name> getNames() throws IOException {
        String[] typeNames = this.getTypeNames();
        List<Name> names = new ArrayList(typeNames.length);
        String[] var3 = typeNames;
        int var4 = typeNames.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String typeName = var3[var5];
            names.add(new NameImpl(typeName));
        }

        return names;
    }

    public SimpleFeatureType getSchema(Name name) throws IOException {
        return this.getSchema(name.getLocalPart());
    }

    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        this.updateSchema(typeName.getLocalPart(), featureType);
    }

    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException("Schema removal not supported");
    }

    public void removeSchema(String typeName) throws IOException {
        throw new UnsupportedOperationException("Schema removal not supported");
    }
}
