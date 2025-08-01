/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.dspace.content.Bitstream;
import org.dspace.content.Bitstream_;
import org.dspace.content.Bundle;
import org.dspace.content.Bundle_;
import org.dspace.content.Collection;
import org.dspace.content.Collection_;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Item_;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.UUIDIterator;

/**
 * Hibernate implementation of the Database Access Object interface class for the Bitstream object.
 * This class is responsible for all database calls for the Bitstream object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BitstreamDAOImpl extends AbstractHibernateDSODAO<Bitstream> implements BitstreamDAO {

    protected BitstreamDAOImpl() {
        super();
    }

    @Override
    public List<Bitstream> findDeletedBitstreams(Context context, int limit, int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Bitstream.class);
        Root<Bitstream> bitstreamRoot = criteriaQuery.from(Bitstream.class);
        criteriaQuery.select(bitstreamRoot);
        criteriaQuery.orderBy(criteriaBuilder.desc(bitstreamRoot.get(Bitstream_.ID)));
        criteriaQuery.where(criteriaBuilder.equal(bitstreamRoot.get(Bitstream_.deleted), true));
        return list(context, criteriaQuery, false, Bitstream.class, limit, offset);

    }

    @Override
    public List<Bitstream> findDuplicateInternalIdentifier(Context context, Bitstream bitstream) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Bitstream.class);
        Root<Bitstream> bitstreamRoot = criteriaQuery.from(Bitstream.class);
        criteriaQuery.select(bitstreamRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(bitstreamRoot.get(Bitstream_.internalId), bitstream.getInternalId()),
            criteriaBuilder.notEqual(bitstreamRoot.get(Bitstream_.id), bitstream.getID())
                            )
        );
        return list(context, criteriaQuery, false, Bitstream.class, -1, -1);
    }

    @Override
    public List<Bitstream> findBitstreamsWithNoRecentChecksum(Context context) throws SQLException {
        Query query = createQuery(context, "SELECT b FROM MostRecentChecksum c RIGHT JOIN Bitstream b " +
            "ON c.bitstream = b WHERE c IS NULL" );

        return query.getResultList();
    }

    @Override
    public Iterator<Bitstream> findByCommunity(Context context, Community community) throws SQLException {
        // Select UUID of all bitstreams, joining from Bitstream -> Bundle -> Item -> Collection -> Community
        // to find all that exist under the given community.
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UUID> criteriaQuery = criteriaBuilder.createQuery(UUID.class);
        Root<Bitstream> bitstreamRoot = criteriaQuery.from(Bitstream.class);
        criteriaQuery.select(bitstreamRoot.get(Bitstream_.id));
        // Joins from Bitstream -> Bundle -> Item -> Collection
        Join<Bitstream, Bundle> joinBundle = bitstreamRoot.join(Bitstream_.bundles);
        Join<Bundle, Item> joinItem = joinBundle.join(Bundle_.items);
        Join<Item, Collection> joinCollection = joinItem.join(Item_.collections);
        // Where "community" is a member of the list of Communities linked by the collection(s)
        criteriaQuery.where(criteriaBuilder.isMember(community, joinCollection.get(Collection_.COMMUNITIES)));

        // Transform into a query object to execute
        Query query = createQuery(context, criteriaQuery);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Bitstream>(context, uuids, Bitstream.class, this);
    }

    @Override
    public Iterator<Bitstream> findByCollection(Context context, Collection collection) throws SQLException {
        // Select UUID of all bitstreams, joining from Bitstream -> Bundle -> Item -> Collection
        // to find all that exist under the given collection.
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UUID> criteriaQuery = criteriaBuilder.createQuery(UUID.class);
        Root<Bitstream> bitstreamRoot = criteriaQuery.from(Bitstream.class);
        criteriaQuery.select(bitstreamRoot.get(Bitstream_.id));
        // Joins from Bitstream -> Bundle -> Item
        Join<Bitstream, Bundle> joinBundle = bitstreamRoot.join(Bitstream_.bundles);
        Join<Bundle, Item> joinItem = joinBundle.join(Bundle_.items);
        // Where "collection" is a member of the list of Collections linked by the item(s)
        criteriaQuery.where(criteriaBuilder.isMember(collection, joinItem.get(Item_.collections)));

        // Transform into a query object to execute
        Query query = createQuery(context, criteriaQuery);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Bitstream>(context, uuids, Bitstream.class, this);
    }

    @Override
    public Iterator<Bitstream> findByItem(Context context, Item item) throws SQLException {
        // Select UUID of all bitstreams, joining from Bitstream -> Bundle -> Item
        // to find all that exist under the given item.
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UUID> criteriaQuery = criteriaBuilder.createQuery(UUID.class);
        Root<Bitstream> bitstreamRoot = criteriaQuery.from(Bitstream.class);
        criteriaQuery.select(bitstreamRoot.get(Bitstream_.id));
        // Join from Bitstream -> Bundle
        Join<Bitstream, Bundle> joinBundle = bitstreamRoot.join(Bitstream_.bundles);
        // Where "item" is a member of the list of Items linked by the bundle(s)
        criteriaQuery.where(criteriaBuilder.isMember(item, joinBundle.get(Bundle_.items)));

        // Transform into a query object to execute
        Query query = createQuery(context, criteriaQuery);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Bitstream>(context, uuids, Bitstream.class, this);
    }

    @Override
    public Iterator<Bitstream> findShowableByItem(Context context, UUID itemId, String bundleName) throws SQLException {
        Query query = createQuery(
            context,
            "select b.id from Bitstream b " +
            "join b.bundles bitBundle " +
            "join bitBundle.items item " +
            "WHERE item.id = :itemId " +
            "and NOT EXISTS( " +
            "  select 1 from MetadataValue mv " +
            "  join mv.metadataField mf " +
            "  join mf.metadataSchema ms " +
            "  where mv.dSpaceObject = b and " +
            "  ms.name = 'bitstream' and " +
            "  mf.element = 'hide' and " +
            "  mf.qualifier is null and " +
            "  (mv.value = 'true' or mv.value = 'yes') " +
            ")" +
            " AND (" +
            "  :bundleName is null OR " +
            "  EXISTS ( " +
            "    select 1 " +
            "    from MetadataValue mvB " +
            "    join mvB.metadataField mfB " +
            "    join mfB.metadataSchema msB " +
            "    where mvB.dSpaceObject = bitBundle and " +
            "    msB.name = 'dc' and " +
            "    mfB.element = 'title' and " +
            "    mfB.qualifier is null and " +
            "    mvB.value = :bundleName " +
            "  )" +
            ")"
        );

        query.setParameter("itemId", itemId);
        query.setParameter("bundleName", bundleName);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Bitstream>(context, uuids, Bitstream.class, this);
    }

    @Override
    public Iterator<Bitstream> findByStoreNumber(Context context, Integer storeNumber) throws SQLException {
        Query query = createQuery(context, "select b.id from Bitstream b where b.storeNumber = :storeNumber");
        query.setParameter("storeNumber", storeNumber);
        @SuppressWarnings("unchecked")
        List<UUID> uuids = query.getResultList();
        return new UUIDIterator<Bitstream>(context, uuids, Bitstream.class, this);
    }

    @Override
    public Long countByStoreNumber(Context context, Integer storeNumber) throws SQLException {


        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<Bitstream> bitstreamRoot = criteriaQuery.from(Bitstream.class);
        criteriaQuery.where(criteriaBuilder.equal(bitstreamRoot.get(Bitstream_.storeNumber), storeNumber));
        return countLong(context, criteriaQuery, criteriaBuilder, bitstreamRoot);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from Bitstream"));
    }

    @Override
    public int countDeleted(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Bitstream b WHERE b.deleted=true"));
    }

    @Override
    public int countWithNoPolicy(Context context) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT count(bit.id) from Bitstream bit where bit.deleted<>true and bit.id not in" +
                                      " (select res.dSpaceObject from ResourcePolicy res where res.resourceTypeId = " +
                                      ":typeId )");
        query.setParameter("typeId", Constants.BITSTREAM);
        return count(query);
    }

    @Override
    public List<Bitstream> getNotReferencedBitstreams(Context context) throws SQLException {
        return list(createQuery(context, "select bit from Bitstream bit where bit.deleted != true" +
            " and bit.id not in (select bit2.id from Bundle bun join bun.bitstreams bit2)" +
            " and bit.id not in (select com.logo.id from Community com)" +
            " and bit.id not in (select col.logo.id from Collection col)" +
            " and bit.id not in (select bun.primaryBitstream.id from Bundle bun)"));
    }

    @Override
    public Iterator<Bitstream> findAll(Context context, int limit, int offset) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        return findByX(context, Bitstream.class, map, true, limit, offset).iterator();

    }
}
