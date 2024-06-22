package org.geotools.database;

import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.FeatureLock;
import org.geotools.api.data.FeatureLockException;
import org.geotools.api.data.FeatureLocking;
import org.geotools.api.data.LockingManager;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureLocking;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.simple.SimpleFeatureIterator;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Set;

public abstract class AbstractFeatureLocking extends AbstractFeatureStore implements FeatureLocking<SimpleFeatureType, SimpleFeature>, SimpleFeatureLocking {
    FeatureLock featureLock;

    public AbstractFeatureLocking() {
        this.featureLock = FeatureLock.TRANSACTION;
    }

    public AbstractFeatureLocking(Set hints) {
        super(hints);
        this.featureLock = FeatureLock.TRANSACTION;
    }

    public void setFeatureLock(FeatureLock lock) {
        if (lock == null) {
            throw new NullPointerException("A FeatureLock is required - did you mean FeatureLock.TRANSACTION?");
        } else {
            this.featureLock = lock;
        }
    }

    public int lockFeatures() throws IOException {
        return this.lockFeatures(Filter.INCLUDE);
    }

    public int lockFeatures(Filter filter) throws IOException {
        return this.lockFeatures(new Query(this.getSchema().getTypeName(), filter));
    }

    public int lockFeatures(Query query) throws IOException {
        LockingManager lockingManager = this.getDataStore().getLockingManager();
        if (lockingManager == null) {
            throw new UnsupportedOperationException("DataStore not using lockingManager, must provide alternate implementation");
        } else {
            SimpleFeatureIterator reader = this.getFeatures(query).features();
            String typeName = query.getTypeName();
            int count = 0;

            try {
                while(reader.hasNext()) {
                    try {
                        SimpleFeature feature = reader.next();
                        lockingManager.lockFeatureID(typeName, feature.getID(), this.getTransaction(), this.featureLock);
                        ++count;
                    } catch (FeatureLockException var12) {
                        ;
                    } catch (NoSuchElementException var13) {
                        throw new DataSourceException("Problem with " + query.getHandle() + " while locking", var13);
                    }
                }
            } finally {
                reader.close();
            }

            return count;
        }
    }

    public void unLockFeatures() throws IOException {
        this.unLockFeatures(Filter.INCLUDE);
    }

    public void unLockFeatures(Filter filter) throws IOException {
        this.unLockFeatures(new Query(this.getSchema().getTypeName(), filter));
    }

    public void unLockFeatures(Query query) throws IOException {
        LockingManager lockingManager = this.getDataStore().getLockingManager();
        if (lockingManager == null) {
            throw new UnsupportedOperationException("DataStore not using lockingManager, must provide alternate implementation");
        } else {
            SimpleFeatureIterator reader = this.getFeatures(query).features();
            String typeName = query.getTypeName();

            try {
                while(reader.hasNext()) {
                    try {
                        SimpleFeature feature = reader.next();
                        lockingManager.unLockFeatureID(typeName, feature.getID(), this.getTransaction(), this.featureLock);
                    } catch (NoSuchElementException var10) {
                        throw new DataSourceException("Problem with " + query.getHandle() + " while locking", var10);
                    }
                }
            } finally {
                reader.close();
            }

        }
    }
}
