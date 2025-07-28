/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.unpaywall.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.unpaywall.dto.UnpaywallItemVersionDto;
import org.dspace.unpaywall.model.Unpaywall;

/**
 * Service that handle the work with Unpaywall api.
 */
public interface UnpaywallService {

    /**
     * Initiates an unpaywall API call if it has not yet been initiated.
     *
     * @param context the relevant DSpace Context
     * @param doi     object doi
     * @param itemId  item id
     */
    void initUnpaywallCallIfNeeded(Context context, String doi, UUID itemId);

    /**
     * Initiates an unpaywall API call.
     *
     * @param context the relevant DSpace Context
     * @param doi     object doi
     * @param itemId  item id
     */
    void initUnpaywallCall(Context context, String doi, UUID itemId);


    /**
     * Retrieves the Unpaywall record by the given doi and itemId values.
     *
     * @param context the DSpace context
     * @param doi     entity item doi
     * @param itemId  item id
     * @return record
     */
    Optional<Unpaywall> findUnpaywall(Context context, String doi, UUID itemId);

    /**
     * Creates the Unpaywall record.
     *
     * @param context   the DSpace context
     * @param unpaywall unpaywall record
     * @return created record
     */
    Unpaywall create(Context context, Unpaywall unpaywall);

    /**
     * Deletes Unpaywall record.
     *
     * @param context   the DSpace context
     * @param unpaywall unpaywall record
     */
    void delete(Context context, Unpaywall unpaywall);

    /**
     * Find all Unpaywall records.
     *
     * @param context the DSpace context
     * @return all records
     */
    List<Unpaywall> findAll(Context context);

    /**
     * Retrives item versions.
     *
     * @param context the DSpace context
     * @param item    item
     * @return list of item versions
     */
    List<UnpaywallItemVersionDto> getItemVersions(Context context, Item item);

    /**
     * Retrives item versions.
     *
     * @param context the DSpace context
     * @param itemId  uuid of the item
     * @return list of item versions
     */
    List<UnpaywallItemVersionDto> getItemVersions(Context context, UUID itemId);

    Unpaywall downloadResource(Context context, Unpaywall unpaywall, Item item);
}
