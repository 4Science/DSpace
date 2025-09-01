/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;

public interface Signature {
    public List<String> getSignature(/* BrowsableDSpaceObject */DSpaceObject item, Context context);

    public List<String> getPlainSignature(DSpaceObject item, Context context);

    public List<String> getSignature(ExternalDataObject object);

    public int getResourceTypeID();

    public String getSignatureType();

    public String getMetadata();
}
