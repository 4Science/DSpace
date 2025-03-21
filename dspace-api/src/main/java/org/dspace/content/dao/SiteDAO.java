/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Site;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the Site object.
 * The implementation of this class is responsible for all database calls for the Site object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SiteDAO extends DSpaceObjectDAO<Site> {

    public Site findSite(Context context) throws SQLException;

    /**
     * Fetches all Site objects from the database.
     *
     * @param context The relevant DSpace Context.
     * @return A List of all Site objects in the database.
     * @throws SQLException If a database access error occurs.
     */
    List<Site> findAll(Context context) throws SQLException;
}
