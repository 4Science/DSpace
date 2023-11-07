/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.science4.webcache;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;

/**
 * Service to handle web server cache related work.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 */
public interface WebServerCache {

    /**
     * Returns list of subject urls to be cached.
     *
     * @param ctx     context
     * @param subject dspace object
     * @return collection of urls.
     */
    public Collection<? extends String> getURLsToCache(Context ctx, DSpaceObject subject);

    /**
     * Returns list of subject urls not to be cached.
     *
     * @param ctx     context
     * @param subject dspace object
     * @return collection of urls.
     */
    public Collection<? extends String> getURLsToDontCache(Context ctx, DSpaceObject subject);

    /**
     * Returns all urls of the subject.
     *
     * @param ctx     context
     * @param subject subject
     * @return list of urls.
     */
    public List<String> getAllURLs(Context ctx, DSpaceObject subject);

    /**
     * Returns list of eventually cached urls for deleted object.
     *
     * @param ctx            context
     * @param subjectType    subject type
     * @param subjectID      subject id
     * @param handle         subject handle
     * @param identifiers    subject identifiers
     * @param metadataValues metadata values
     * @return collection of urls.
     */
    public Collection<? extends String> getURLsEventuallyInCacheForDeletedObject(Context ctx, int subjectType,
            UUID subjectID, String handle, List<String> identifiers, List<MetadataValueDTO> metadataValues);

    /**
     * Invalidates and renews passed urls.
     *
     * @param ctx          context
     * @param urlsToUpdate set of urls to be updated
     * @param urlsToRemove set of urls to be invalidated.
     */
    public void invalidateAndRenew(Context ctx, Set<String> urlsToUpdate, Set<String> urlsToRemove);

}
