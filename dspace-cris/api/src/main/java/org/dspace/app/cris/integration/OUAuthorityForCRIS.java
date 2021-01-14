/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;

public class OUAuthorityForCRIS extends CRISAuthorityForCRIS<OrganizationUnit>
{

    @Override
    public int getCRISTargetTypeID()
    {
        return CrisConstants.OU_TYPE_ID;
    }

    @Override
    public Class<OrganizationUnit> getCRISTargetClass()
    {
        return OrganizationUnit.class;
    }

    
    @Override
    public String getPublicPath() {
    	return "ou";
    }

	@Override
	public OrganizationUnit getNewCrisObject() {
		return new OrganizationUnit();
	}

	@Override
	public Choices getMatches(Context context, String field, String text, int collection, int start, int limit,
			String locale, boolean extra) {
		// TODO Auto-generated method stub
		return null;
	}
    

}
