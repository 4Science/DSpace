/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * CollectionValidationExclusive permits doi register to all collection NOT in collectionList; 
 *
 */
public class CollectionValidationExclusive extends ACollectionValidation {

    private Logger log = Logger.getLogger(CollectionValidationExclusive.class);
    
	private ArrayList<String> collectionList;

	@Override
	public boolean canRegister(Context context, DSpaceObject dso) {
		if (dso.getType() == Constants.ITEM) {
			Item item = (Item) dso;
			String collHandle = getCollectionHandle(context, item);

			if (!collectionList.contains(collHandle)) {
			    log.debug("Checking DOI could mint (ITEM Identifier):" + item.getID());
				return isToRegister(item);
			}
		}
		return false;
	}

	public ArrayList<String> getCollectionList() {
		return collectionList;
	}

	public void setCollectionList(ArrayList<String> collectionList) {
		this.collectionList = collectionList;
	}


}
