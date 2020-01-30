/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deduplication.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.deduplication.Deduplication;

/**
 * Database Access Object interface class for the Deduplication object. The
 * implementation of this class is responsible for all database calls for the
 * Deduplication object and is autowired by spring This class should only be
 * accessed from a single service and should never be exposed outside of the API
 *
 * @author fcadili
 */
public interface DeduplicationDAO extends GenericDAO<Deduplication> {
    public List<Deduplication> findByFirstAndSecond(Context context, String firstId, String secondId)
            throws SQLException;

    public Deduplication uniqueByFirstAndSecond(Context context, String firstId, String secondId) throws SQLException;

    public List<Deduplication> findAll(Context context) throws SQLException;

    public Integer getNextDeduplicationId(Context context) throws SQLException;

    public int countRows(Context context) throws SQLException;
}
