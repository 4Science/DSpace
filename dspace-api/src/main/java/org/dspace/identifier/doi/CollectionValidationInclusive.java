/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Permit DOI minting ONLY to one collection.
 *  
 */
public class CollectionValidationInclusive extends ACollectionValidation {

    private Logger log = Logger.getLogger(CollectionValidationInclusive.class);
    
	private String collectionHandle;

	@Override
	public boolean canRegister(Context context, DSpaceObject dso) {
		if (dso.getType() == Constants.ITEM) {
			Item item = (Item) dso;
			String collHandle = getCollectionHandle(context, item);

			if (collectionHandle.equalsIgnoreCase(collHandle)) {
			    log.debug("Checking DOI could mint (ITEM Identifier):" + item.getID());
				return isToRegister(item);
			}
		}		
		return false;
	}

	public String getCollectionHandle() {
		return collectionHandle;
	}

	public void setCollectionHandle(String collectionHandle) {
		this.collectionHandle = collectionHandle;
	}

}
