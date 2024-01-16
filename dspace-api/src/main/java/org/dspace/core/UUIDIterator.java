/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.AbstractIterator;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.springframework.beans.factory.annotation.Autowired;



/**
 * Iterator implementation which allows to iterate over items and commit while
 * iterating. Using a list of UUID the iterator doesn't get invalidated after a
 * commit
 *
 * @author stefano.maffei at 4science.com
 * @param  <T> class type
 */
public class UUIDIterator<T> extends AbstractIterator<T> {

    private Class<?> entityTypeClass;

    private List<UUID> cachedUUIDs = new LinkedList<>();

    private Iterator<UUID> uuidIterator;

    private Iterator<T> iterator;

    @SuppressWarnings("rawtypes")
    @Autowired
    private DSpaceObjectService dsoService;

    private Context ctx;

    private boolean isSupportedUUIDIterator;

    public UUIDIterator(Context ctx, Iterator<T> i, Class<?> entityTypeClass) throws SQLException {
        this.ctx = ctx;

        this.entityTypeClass = entityTypeClass;
        isSupportedUUIDIterator = DSpaceObject.class.isAssignableFrom(this.entityTypeClass);

        if (isSupportedUUIDIterator) {
            while (i.hasNext()) {
                DSpaceObject dso = (DSpaceObject) i.next();
                if (dsoService == null) {
                    dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
                }
                cachedUUIDs.add(dso.getID());
            }
            uuidIterator = cachedUUIDs.iterator();
        } else {
            iterator = i;
        }

    }

    public UUIDIterator(Context ctx, Collection<T> collection, Class<?> entityTypeClass) throws SQLException {
        this.ctx = ctx;

        this.entityTypeClass = entityTypeClass;
        isSupportedUUIDIterator = DSpaceObject.class.isAssignableFrom(this.entityTypeClass);

        if (isSupportedUUIDIterator) {
            for (T obj : collection) {
                DSpaceObject dso = (DSpaceObject) obj;
                if (dsoService == null) {
                    dsoService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
                }
                cachedUUIDs.add(dso.getID());
            }
            uuidIterator = cachedUUIDs.iterator();
        } else {
            throw new UnsupportedOperationException("Cannot generate iterator for this collection");
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    protected T computeNext() {
        try {
            if (isSupportedUUIDIterator) {
                return uuidIterator.hasNext() ? (T) dsoService.find(ctx, uuidIterator.next()) : endOfData();
            } else {
                return iterator.hasNext() ? (T) iterator.next() : endOfData();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
