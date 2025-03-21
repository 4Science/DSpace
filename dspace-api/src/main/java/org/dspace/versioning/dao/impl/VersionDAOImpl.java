/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.Version_;
import org.dspace.versioning.dao.VersionDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the Version object.
 * This class is responsible for all database calls for the Version object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author kevinvandevelde at atmire.com
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 */
public class VersionDAOImpl extends AbstractHibernateDAO<Version> implements VersionDAO {
    protected VersionDAOImpl() {
        super();
    }

    @Override
    public Version findByItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Version.class);
        Root<Version> versionRoot = criteriaQuery.from(Version.class);
        criteriaQuery.select(versionRoot);
        criteriaQuery.where(criteriaBuilder.equal(versionRoot.get(Version_.item), item));
        return singleResult(context, criteriaQuery);
    }

    @Override
    public int getNextVersionNumber(Context c, VersionHistory vh) throws SQLException {
        Query q = this.createQuery(c,
                                   "SELECT (COALESCE(MAX(versionNumber), 0) + 1) "
                                       + "FROM Version WHERE versionHistory.id = :historyId");
        q.setParameter("historyId", vh.getID());

        int next = (Integer) q.getSingleResult();
        return next;
    }

    @Override
    public List<Version> findVersionsWithItems(Context context, VersionHistory versionHistory, int offset, int limit)
        throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Version.class);
        Root<Version> versionRoot = criteriaQuery.from(Version.class);
        criteriaQuery.select(versionRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(versionRoot.get(Version_.versionHistory), versionHistory),
                                       criteriaBuilder.isNotNull(versionRoot.get(Version_.item))
                   )
        );

        List<jakarta.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(versionRoot.get(Version_.versionNumber)));
        criteriaQuery.orderBy(orderList);

        return list(context, criteriaQuery, false, Version.class, limit, offset);
    }

    @Override
    public int countVersionsByHistoryWithItem(Context context, VersionHistory versionHistory) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM " + Version.class.getSimpleName()
                + " WHERE versionHistory = :versionhistory"
                + " AND  item IS NOT NULL");
        query.setParameter("versionhistory", versionHistory);
        return count(query);
    }

    @Override
    public boolean areDifferentVersionsOfSameItem(Context context, UUID firstItemUuid, UUID secondItemUuid)
        throws SQLException {

        Query query = createQuery(context, ""
            + " SELECT 1 "
            + "   FROM Version v1, Version v2 "
            + "  WHERE v1.item.id = :firstItemUuid "
            + "    AND v2.item.id = :secondItemUuid "
            + "    AND v1.versionHistory.id = v2.versionHistory.id");

        query.setParameter("firstItemUuid", firstItemUuid);
        query.setParameter("secondItemUuid", secondItemUuid);

        return singleResult(query) != null;
    }

}
