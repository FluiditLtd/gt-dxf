package org.geotools.database;

import org.geotools.data.DataSourceException;
import org.geotools.data.Diff;
import org.geotools.data.DiffFeatureReader;
import org.geotools.data.DiffFeatureWriter;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TransactionStateDiff implements Transaction.State {
    AbstractDataStore store;
    Transaction transaction;
    Map<String, Diff> typeNameDiff = new HashMap();
    public static final SimpleFeature NULL = new SimpleFeature() {
        public Object getAttribute(String path) {
            return null;
        }

        public Object getAttribute(int index) {
            return null;
        }

        public ReferencedEnvelope getBounds() {
            return null;
        }

        public Geometry getDefaultGeometry() {
            return null;
        }

        public SimpleFeatureType getFeatureType() {
            return null;
        }

        public String getID() {
            return null;
        }

        public FeatureId getIdentifier() {
            return null;
        }

        public void setAttribute(int position, Object val) {
        }

        public void setAttribute(String path, Object attribute) throws IllegalAttributeException {
        }

        public Object getAttribute(Name name) {
            return null;
        }

        public int getAttributeCount() {
            return 0;
        }

        public List<Object> getAttributes() {
            return null;
        }

        public SimpleFeatureType getType() {
            return null;
        }

        public void setAttribute(Name name, Object value) {
        }

        public void setAttributes(List<Object> values) {
        }

        public void setAttributes(Object[] values) {
        }

        public void setDefaultGeometry(Object geometry) {
        }

        public GeometryAttribute getDefaultGeometryProperty() {
            return null;
        }

        public void setDefaultGeometryProperty(GeometryAttribute geometryAttribute) {
        }

        public Collection<Property> getProperties(Name name) {
            return null;
        }

        public Collection<Property> getProperties() {
            return null;
        }

        public Collection<Property> getProperties(String name) {
            return null;
        }

        public Property getProperty(Name name) {
            return null;
        }

        public Property getProperty(String name) {
            return null;
        }

        public Collection<? extends Property> getValue() {
            return null;
        }

        public void setValue(Collection<Property> values) {
        }

        public AttributeDescriptor getDescriptor() {
            return null;
        }

        public Name getName() {
            return null;
        }

        public Map<Object, Object> getUserData() {
            return null;
        }

        public boolean isNillable() {
            return false;
        }

        public void setValue(Object newValue) {
        }

        public String toString() {
            return "<NullFeature>";
        }

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object arg0) {
            return arg0 == this;
        }

        public void validate() {
        }
    };

    public TransactionStateDiff(AbstractDataStore dataStore) {
        this.store = dataStore;
    }

    public synchronized void setTransaction(Transaction transaction) {
        if (transaction != null) {
            this.transaction = transaction;
        } else {
            this.transaction = null;
            if (this.typeNameDiff != null) {
                Iterator i = this.typeNameDiff.values().iterator();

                while(i.hasNext()) {
                    Diff diff = (Diff)i.next();
                    diff.clear();
                }

                this.typeNameDiff.clear();
            }

            this.store = null;
        }

    }

    public synchronized Diff diff(String typeName) throws IOException {
        if (!this.exists(typeName)) {
            throw new IOException(typeName + " not defined");
        } else if (this.typeNameDiff.containsKey(typeName)) {
            return (Diff)this.typeNameDiff.get(typeName);
        } else {
            Diff diff = new Diff();
            this.typeNameDiff.put(typeName, diff);
            return diff;
        }
    }

    boolean exists(String typeName) {
        String[] types;
        try {
            types = this.store.getTypeNames();
        } catch (IOException var4) {
            return false;
        }

        Arrays.sort(types);
        return Arrays.binarySearch(types, typeName) != -1;
    }

    public synchronized void addAuthorization(String AuthID) throws IOException {
    }

    public synchronized void commit() throws IOException {
        Iterator i = this.typeNameDiff.entrySet().iterator();

        while(i.hasNext()) {
            Map.Entry<String, Diff> entry = (Map.Entry)i.next();
            String typeName = (String)entry.getKey();
            Diff diff = (Diff)entry.getValue();
            this.applyDiff(typeName, diff);
        }

    }

    void applyDiff(String typeName, Diff diff) throws IOException {
        if (!diff.isEmpty()) {
            FeatureWriter writer;
            try {
                writer = this.store.createFeatureWriter(typeName, this.transaction);
            } catch (UnsupportedOperationException var29) {
                throw var29;
            }

            Object cause = null;

            try {
                while(writer.hasNext()) {
                    SimpleFeature feature = (SimpleFeature)writer.next();
                    String fid = feature.getID();
                    if (diff.getModified().containsKey(fid)) {
                        SimpleFeature update = (SimpleFeature)diff.getModified().get(fid);
                        if (update == Diff.NULL) {
                            writer.remove();
                            this.store.listenerManager.fireFeaturesRemoved(typeName, this.transaction, ReferencedEnvelope.reference(feature.getBounds()), true);
                        } else {
                            try {
                                feature.setAttributes(update.getAttributes());
                                writer.write();
                                ReferencedEnvelope bounds = new ReferencedEnvelope((CoordinateReferenceSystem)null);
                                bounds.include(feature.getBounds());
                                bounds.include(update.getBounds());
                                this.store.listenerManager.fireFeaturesChanged(typeName, this.transaction, bounds, true);
                            } catch (IllegalAttributeException var28) {
                                throw new DataSourceException("Could update " + fid, var28);
                            }
                        }
                    }
                }

                synchronized(diff) {
                    Iterator var10 = diff.getAddedOrder().iterator();

                    while(var10.hasNext()) {
                        String fid = (String)var10.next();
                        SimpleFeature addedFeature = (SimpleFeature)diff.getAdded().get(fid);
                        SimpleFeature nextFeature = (SimpleFeature)writer.next();
                        if (nextFeature == null) {
                            throw new DataSourceException("Could not add " + fid);
                        }

                        try {
                            nextFeature.setAttributes(addedFeature.getAttributes());
                            nextFeature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                            if (addedFeature.getUserData().containsKey(Hints.PROVIDED_FID)) {
                                String providedFid = (String)addedFeature.getUserData().get(Hints.PROVIDED_FID);
                                nextFeature.getUserData().put(Hints.PROVIDED_FID, providedFid);
                            } else {
                                nextFeature.getUserData().put(Hints.PROVIDED_FID, addedFeature.getID());
                            }

                            writer.write();
                            this.store.listenerManager.fireFeaturesAdded(typeName, this.transaction, ReferencedEnvelope.reference(nextFeature.getBounds()), true);
                        } catch (IllegalAttributeException var27) {
                            throw new DataSourceException("Could update " + fid, var27);
                        }
                    }

                }
            } catch (IOException var33) {
                cause = var33;
                throw var33;
            } catch (RuntimeException var34) {
                cause = var34;
                throw var34;
            } finally {
                try {
                    writer.close();
                    this.store.listenerManager.fireChanged(typeName, this.transaction, true);
                    diff.clear();
                } catch (IOException var30) {
                    if (cause != null) {
                        var30.initCause((Throwable)cause);
                    }

                    throw var30;
                } catch (RuntimeException var31) {
                    if (cause != null) {
                        var31.initCause((Throwable)cause);
                    }

                    throw var31;
                }
            }
        }
    }

    public synchronized void rollback() throws IOException {
        Iterator i = this.typeNameDiff.entrySet().iterator();

        while(i.hasNext()) {
            Map.Entry<String, Diff> entry = (Map.Entry)i.next();
            String typeName = (String)entry.getKey();
            Diff diff = (Diff)entry.getValue();
            diff.clear();
            this.store.listenerManager.fireChanged(typeName, this.transaction, false);
        }

    }

    public synchronized FeatureReader<SimpleFeatureType, SimpleFeature> reader(String typeName) throws IOException {
        Diff diff = this.diff(typeName);
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = this.store.getFeatureReader(typeName);
        return new DiffFeatureReader(reader, diff);
    }

    public synchronized FeatureWriter<SimpleFeatureType, SimpleFeature> writer(final String typeName, Filter filter) throws IOException {
        Diff diff = this.diff(typeName);
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = new FilteringFeatureReader(this.store.getFeatureReader(typeName, new Query(typeName, filter)), filter);
        return new DiffFeatureWriter(reader, diff, filter) {
            public void fireNotification(int eventType, ReferencedEnvelope bounds) {
                switch(eventType) {
                    case -1:
                        TransactionStateDiff.this.store.listenerManager.fireFeaturesRemoved(typeName, TransactionStateDiff.this.transaction, bounds, false);
                        break;
                    case 0:
                        TransactionStateDiff.this.store.listenerManager.fireFeaturesChanged(typeName, TransactionStateDiff.this.transaction, bounds, false);
                        break;
                    case 1:
                        TransactionStateDiff.this.store.listenerManager.fireFeaturesAdded(typeName, TransactionStateDiff.this.transaction, bounds, false);
                }

            }

            public String toString() {
                return "<DiffFeatureWriter>(" + this.reader.toString() + ")";
            }
        };
    }
}
