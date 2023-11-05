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
import org.dspace.core.Context;

public interface WebServerCache {

    public Collection<? extends String> getURLsToCache(Context ctx, DSpaceObject subject);

    public Collection<? extends String> getURLsToDontCache(Context ctx, DSpaceObject subject);

    public Collection<? extends String> getURLsEventuallyInCacheForDeletedObject(Context ctx, int subjectType,
            UUID subjectID, String handle, List<String> identifiers);

    public void invalidateAndRenew(Context ctx, Set<String> urlsToUpdate, Set<String> urlsToRemove);

}
