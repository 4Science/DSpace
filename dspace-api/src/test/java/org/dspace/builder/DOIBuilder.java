/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;


import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.service.DOIService;


/**
* Builder to construct DOI entities for tests
*
* @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
**/
public class DOIBuilder extends AbstractBuilder<DOI, DOIService> {

    private DOI doi;

    protected DOIBuilder(Context context) {
        super(context);
    }

    public static DOIBuilder createDOI(Context context, DSpaceObject dso) {
        return new DOIBuilder(context).create(dso);
    }

    private DOIBuilder create(DSpaceObject dso) {
        try {
            doi = doiService.findDOIByDSpaceObject(context, dso);
            if (doi == null) {
                doi = doiService.create(context);
                doi.setDSpaceObject(dso);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return this;
    }

    public DOIBuilder withDoiString(String doiString) {
        doi.setDoi(doiString);
        return this;
    }

    public DOIBuilder withStatus(int status) {
        doi.setStatus(status);
        return this;
    }

    @Override
    public DOI build() {
        try {
            doiService.update(context, doi);
            context.dispatchEvents();
            indexingService.commit();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return doi;
    }

    @Override
    public void delete(Context c, DOI dso) throws Exception {
        if (dso != null) {
            doiDao.delete(c, doi);
            doi = null;
        }
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            doi = c.reloadEntity(doi);
            if (doi != null) {
                doiDao.delete(c, doi);
                c.complete();
            }
        }
    }

    @Override
    protected DOIService getService() {
        return doiService;
    }
}
