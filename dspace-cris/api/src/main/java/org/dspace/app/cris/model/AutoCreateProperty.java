package org.dspace.app.cris.model;

public interface AutoCreateProperty {

	/***
	 * This method is usually called by the
	 * context Bean.
	 * 
	 * E.g. @see org.dspace.app.webui.cris.controller.OUDetailsControl
	 */
	public void enableAutoCreate();
	
	/***
	 * This method is usually implemented inside
	 * bean to check for a property like 'crisrp.dept.create'.
	 */
	public void discoverAutoCreate();
	
	/***
	 * @return The method return true if autoCreate is enabled
	 * and discovered auto-created preference exists.
	 */
	public boolean forceAutoCreate();
}
