package org.geotools.database;

import org.geotools.data.DataSourceException;
import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLockException;
import org.geotools.data.FeatureLocking;
import org.geotools.data.LockingManager;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

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
        return this.lockFeatures((Filter)Filter.INCLUDE);
    }

    public int lockFeatures(Filter filter) throws IOException {
        return this.lockFeatures((Query)(new DefaultQuery(((SimpleFeatureType)this.getSchema()).getTypeName(), filter)));
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
                        SimpleFeature feature = (SimpleFeature)reader.next();
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
        this.unLockFeatures((Filter)Filter.INCLUDE);
    }

    public void unLockFeatures(Filter filter) throws IOException {
        this.unLockFeatures((Query)(new DefaultQuery(((SimpleFeatureType)this.getSchema()).getTypeName(), filter)));
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
                        SimpleFeature feature = (SimpleFeature)reader.next();
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
