/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */

package org.dspace.app.cris.model.listener;

//Code written by Florian Gantner
import it.cilea.osd.common.listener.NativePostUpdateEventListener;

import javax.persistence.Transient;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.rdf.RDFUtil;
import org.dspace.core.Context;

import it.cilea.osd.common.model.Identifiable;

public class RDFListener implements NativePostUpdateEventListener, PostDeleteEventListener {
	
	public static final long serialVersionUID = 0;
	//listens to Events on ACRISObjects (Projects/RP/OU/ResearchObjects)
	//Automatically updates the Triple Store, if configured
	//similiar to RDFConsumer
	//Values of Items as Authority Values etc.. 
	//...are automatically updated by org.dspace.app.cris.integration.authority.CrisConsumer
	
	//Add/Modify Metadata -> Regenerate Objects
	//Delete Metadatafield -> Regenerate Objects
	//Delete Object -> Remove Object

	@Transient
	private static Logger log = Logger.getLogger(RDFListener.class);

	@Override
	public <T extends Identifiable> void onPostUpdate(T entity) {
		
		Object object = entity;
		if (!(object instanceof ACrisObject)) {
			// nothing to do
			return;
		}

		log.debug("Call onPostUpdate " + RDFListener.class);
		
		ACrisObject crisObj = (ACrisObject) object;
		
		try {
			log.debug("Update RDF Test for entity " + crisObj.getTypeText() + "/" + crisObj.getCrisID());
			//add RDF-Generating for CRIS-Objects here
			//something has been updated
			//not more available? (public/privacy) -> Delete
			if(RDFUtil.isPublicBoolean(null, crisObj)) {
				//convert and Store again
				RDFUtil.convertAndStore(null, crisObj);
				log.debug("Updating cris-object ");
			}else {
				//delete, not more public (or not public before)
				//if not in triple store, error will be thrown.
				String uri = RDFUtil.generateIdentifier(null, crisObj);
				RDFUtil.delete(uri);
				log.debug("Trying to delete object from " + uri);
			}
		} catch (Exception e) {
			log.error("Failed to Generate and Store RDF for entity " + crisObj.getTypeText() + "/" + crisObj.getCrisID() + " |" + e.toString());
		}
		
		log.debug("End onPostUpdate " + RDFListener.class);
	}
	
	  public void onPostDelete(PostDeleteEvent event)
	    {
	        Object object = event.getEntity();
	        if (!(object instanceof ACrisObject))
	        {
	            // nothing to do
	            return;
	        }
	        
	        log.debug("Call onPostDelete " + RDFListener.class);
			
			ACrisObject crisObj = (ACrisObject) object;
			
			try {
				log.debug("Delete RDF Test for entity " + crisObj.getTypeText() + "/" + crisObj.getCrisID());
				//add RDF-Generating for CRIS-Objects here
				//something has been updated
				String uri = RDFUtil.generateIdentifier(null, crisObj);
				RDFUtil.delete(uri);
				
			} catch (Exception e) {
				log.error("Failed to Delete RDF for entity " + crisObj.getTypeText() + "/" + crisObj.getCrisID());
			}
			
			log.debug("End onPostDelete " + RDFListener.class);
	    }

	}