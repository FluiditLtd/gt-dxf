package org.geotools.database;

import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.util.factory.Hints;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class AbstractFeatureStore extends AbstractFeatureSource implements SimpleFeatureStore {
    protected Transaction transaction;

    public AbstractFeatureStore() {
        this.transaction = Transaction.AUTO_COMMIT;
    }

    public AbstractFeatureStore(Set hints) {
        super(hints);
        this.transaction = Transaction.AUTO_COMMIT;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    public final void modifyFeatures(AttributeDescriptor type, Object value, Filter filter) throws IOException {
        Name attributeName = type.getName();
        this.modifyFeatures(attributeName, value, filter);
    }

    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter) throws IOException {
        this.modifyFeatures(new Name[]{attributeName}, new Object[]{attributeValue}, filter);
    }

    public void modifyFeatures(String name, Object attributeValue, Filter filter) throws IOException {
        this.modifyFeatures(new Name[]{new NameImpl(name)}, new Object[]{attributeValue}, filter);
    }

    public void modifyFeatures(String[] names, Object[] values, Filter filter) throws IOException {
        Name[] attributeNames = new Name[names.length];

        for(int i = 0; i < names.length; ++i) {
            attributeNames[i] = new NameImpl(names[i]);
        }

        this.modifyFeatures(attributeNames, values, filter);
    }

    public final void modifyFeatures(AttributeDescriptor[] type, Object[] value, Filter filter) throws IOException {
        Name[] attributeNames = new Name[type.length];

        for(int i = 0; i < type.length; ++i) {
            attributeNames[i] = type[i].getName();
        }

        this.modifyFeatures(attributeNames, value, filter);
    }

    public void modifyFeatures(Name[] attributeNames, Object[] attributeValues, Filter filter) throws IOException {
        String typeName = ((SimpleFeatureType)this.getSchema()).getTypeName();
        if (filter == null) {
            String msg = "Must specify a filter, must not be null.";
            throw new IllegalArgumentException(msg);
        } else {
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = this.getDataStore().getFeatureWriter(typeName, filter, this.getTransaction());
            Name[] var7 = attributeNames;
            int var8 = attributeNames.length;

            for(int var9 = 0; var9 < var8; ++var9) {
                Name attributeName = var7[var9];
                if (((SimpleFeatureType)this.getSchema()).getDescriptor(attributeName) == null) {
                    throw new DataSourceException("Cannot modify " + attributeName + " as it is not an attribute of " + ((SimpleFeatureType)this.getSchema()).getName());
                }
            }

            try {
                while(writer.hasNext()) {
                    SimpleFeature feature = (SimpleFeature)writer.next();

                    for(int i = 0; i < attributeNames.length; ++i) {
                        try {
                            feature.setAttribute(attributeNames[i], attributeValues[i]);
                        } catch (Exception var14) {
                            throw new DataSourceException("Could not update feature " + feature.getID() + " with " + attributeNames[i] + "=" + attributeValues[i], var14);
                        }
                    }

                    writer.write();
                }
            } finally {
                writer.close();
            }

        }
    }

    public Set<String> addFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader) throws IOException {
        Set<String> addedFids = new HashSet();
        String typeName = ((SimpleFeatureType)this.getSchema()).getTypeName();
        SimpleFeature feature = null;
        FeatureWriter writer = this.getDataStore().getFeatureWriterAppend(typeName, this.getTransaction());

        try {
            while(reader.hasNext()) {
                try {
                    feature = (SimpleFeature)reader.next();
                } catch (Exception var13) {
                    throw new DataSourceException("Could not add Features, problem with provided reader", var13);
                }

                SimpleFeature newFeature = (SimpleFeature)writer.next();

                try {
                    newFeature.setAttributes(feature.getAttributes());
                } catch (Exception var12) {
                    throw new DataSourceException("Could not create " + typeName + " out of provided feature: " + feature.getID(), var12);
                }

                boolean useExisting = Boolean.TRUE.equals(feature.getUserData().get(Hints.USE_PROVIDED_FID));
                if (this.getQueryCapabilities().isUseProvidedFIDSupported() && useExisting) {
                    ((FeatureIdImpl)newFeature.getIdentifier()).setID(feature.getID());
                }

                writer.write();
                addedFids.add(newFeature.getID());
            }
        } finally {
            reader.close();
            writer.close();
        }

        return addedFids;
    }

    public List<FeatureId> addFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> collection) throws IOException {
        List<FeatureId> addedFids = new LinkedList();
        String typeName = ((SimpleFeatureType)this.getSchema()).getTypeName();
        SimpleFeature feature = null;
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = this.getDataStore().getFeatureWriterAppend(typeName, this.getTransaction());
        FeatureIterator iterator = collection.features();

        try {
            while(iterator.hasNext()) {
                feature = (SimpleFeature)iterator.next();
                SimpleFeature newFeature = (SimpleFeature)writer.next();

                try {
                    newFeature.setAttributes(feature.getAttributes());
                } catch (Exception var12) {
                    throw new DataSourceException("Could not create " + typeName + " out of provided feature: " + feature.getID(), var12);
                }

                boolean useExisting = Boolean.TRUE.equals(feature.getUserData().get(Hints.USE_PROVIDED_FID));
                if (this.getQueryCapabilities().isUseProvidedFIDSupported() && useExisting) {
                    ((FeatureIdImpl)newFeature.getIdentifier()).setID(feature.getID());
                }

                writer.write();
                addedFids.add(newFeature.getIdentifier());
            }
        } finally {
            iterator.close();
            writer.close();
        }

        return addedFids;
    }

    public void removeFeatures(Filter filter) throws IOException {
        String typeName = ((SimpleFeatureType)this.getSchema()).getTypeName();
        FeatureWriter writer = this.getDataStore().getFeatureWriter(typeName, filter, this.getTransaction());

        try {
            while(writer.hasNext()) {
                writer.next();
                writer.remove();
            }
        } finally {
            writer.close();
        }

    }

    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader) throws IOException {
        String typeName = ((SimpleFeatureType)this.getSchema()).getTypeName();
        FeatureWriter writer = this.getDataStore().getFeatureWriter(typeName, this.getTransaction());

        try {
            SimpleFeature feature;
            while(writer.hasNext()) {
                feature = (SimpleFeature)writer.next();
                writer.remove();
            }

            for(; reader.hasNext(); writer.write()) {
                try {
                    feature = (SimpleFeature)reader.next();
                } catch (Exception var12) {
                    throw new DataSourceException("Could not add Features, problem with provided reader", var12);
                }

                SimpleFeature newFeature = (SimpleFeature)writer.next();

                try {
                    newFeature.setAttributes(feature.getAttributes());
                } catch (org.opengis.feature.IllegalAttributeException var11) {
                    throw new DataSourceException("Could not create " + typeName + " out of provided feature: " + feature.getID(), var11);
                }
            }
        } finally {
            reader.close();
            writer.close();
        }

    }

    public void setTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null, did you mean Transaction.AUTO_COMMIT?");
        } else {
            this.transaction = transaction;
        }
    }
}