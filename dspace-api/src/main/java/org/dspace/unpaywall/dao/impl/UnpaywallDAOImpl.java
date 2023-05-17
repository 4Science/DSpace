/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.dao.impl;

import java.sql.SQLException;
import java.util.UUID;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.unpaywall.dao.UnpaywallDAO;
import org.dspace.unpaywall.model.Unpaywall;

/**
 * Implementation of {@link UnpaywallDAO}.
 */
@SuppressWarnings("unchecked")
public class UnpaywallDAOImpl extends AbstractHibernateDAO<Unpaywall> implements UnpaywallDAO {

    @Override
    public Unpaywall uniqueByItemId(Context context, UUID itemId) throws SQLException {
        Query query = createQuery(context, "FROM Unpaywall WHERE itemId = :itemId");
        query.setParameter("itemId", itemId);
        return getSungleResult(query);
    }

    @Override
    public Unpaywall uniqueByDOIAndItemID(Context context, String doi, UUID itemId) throws SQLException {
        Query query = createQuery(context, "FROM Unpaywall WHERE doi = :doi AND itemId = :itemId");
        query.setParameter("doi", doi);
        query.setParameter("itemId", itemId);
        return getSungleResult(query);
    }

    private static Unpaywall getSungleResult(Query query) {
        try {
            return (Unpaywall) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
