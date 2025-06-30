/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Site;
import org.dspace.core.Context;

/**
 * Service interface class for the Site object.
 * The implementation of this class is responsible for all business logic calls for the Site object and is autowired
 * by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SiteService extends DSpaceObjectService<Site> {

    public Site createSite(Context context) throws SQLException;

    public Site findSite(Context context) throws SQLException;

    /**
     * Retrieve a list of all sites in the system.
     *
     * @param context The relevant DSpace Context.
     * @return A List of all Site objects in the system.
     * @throws SQLException If a database access error occurs.
     */
    List<Site> findAll(Context context) throws SQLException;
}
