/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.dao;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.unpaywall.model.Unpaywall;

/**
 * Database Access Object interface class for the Unpaywall object. The
 * implementation of this class is responsible for all database calls for the
 * Unpaywall object and is autowired by spring. This class should only be
 * accessed from a single service and should never be exposed outside of the API
 */
public interface UnpaywallDAO extends GenericDAO<Unpaywall> {

    /**
     * Find the Unpaywall record by the given item id.
     *
     * @param context the DSpace context
     * @param itemId  entity item id
     * @return record
     * @throws SQLException if an SQL error occurs
     */
    Unpaywall uniqueByItemId(Context context, UUID itemId) throws SQLException;

    /**
     * Find the Unpaywall record by the given doi and itemId values.
     *
     * @param context the DSpace context
     * @param doi     entity item doi
     * @param itemId  item id
     * @return record
     * @throws SQLException if an SQL error occurs
     */
    Unpaywall uniqueByDOIAndItemID(Context context, String doi, UUID itemId) throws SQLException;

}
