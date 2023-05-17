/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.http.HttpException;
import org.dspace.core.Context;
import org.dspace.unpaywall.model.Unpaywall;

/**
 * Service that handle the work with Unpaywall api.
 */
public interface UnpaywallService {

    /**
     * Returns unpaywall api record, creates if not exists.
     *
     * @param context the relevant DSpace Context
     * @param doi     object doi
     * @param itemId  item id
     * @return unpaywall api record.
     */
    Unpaywall getUnpaywallCall(Context context, String doi, UUID itemId)
            throws HttpException, SQLException;
}
