/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.utils.DSpace;

public class AssignDOIConsumer implements Consumer {
	
	// items to be updated with DOI
	private Map<Integer, Item> itemIDs = null;
    
	@Override
	public void initialize() throws Exception {
	}

	@Override
	public void consume(Context ctx, Event event) throws Exception {
		int subjectType = event.getSubjectType();
        int eventType = event.getEventType();
        
        if (itemIDs == null)
        {
        	itemIDs = new HashMap<Integer, Item>();
        }
        
        if (subjectType == Constants.ITEM && eventType == Event.MODIFY)
        {
        	// put Item ID and Item into the created map
        	Item item = (Item)event.getSubject(ctx);
        	int itemID = item.getID();
        	if (item.isArchived() && !itemIDs.containsKey(itemID))
        	{
        		itemIDs.put(itemID, item);
        	}
        }
	}

	@Override
	public void end(Context ctx) throws Exception {
		if (itemIDs != null && itemIDs.size() > 0)
		{
			DOIIdentifierProvider doiIdentifierService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
			
			for (Map.Entry<Integer, Item> itemEntry : itemIDs.entrySet())
			{
				try
				{
					doiIdentifierService.register(ctx, itemEntry.getValue());
				} catch (IdentifierException e)
				{
					throw new RuntimeException("Can't create an Identifier!", e);
				}
			}
			
			// browse updates wrote to the DB, so we have to commit.
			ctx.getDBConnection().commit();
		}
		itemIDs = null;
	}

	@Override
	public void finish(Context ctx) throws Exception {
	}

}