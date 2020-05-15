/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ICrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;

public class CrisFondsEnhancer extends CrisEnhancer
{
	private static final Logger log = Logger.getLogger(CrisFondsEnhancer.class);

    public <P extends Property<TP>, TP extends PropertiesDefinition> List<P> getProperties(
            ICrisObject<P, TP> cris, String qualifier)
    {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        List<P> p = cris.getAnagrafica4view().get("fondscreationtime");
        if (p == null || p.isEmpty() || StringUtils.isBlank(p.get(0).toString())) {
        	ResearcherPageUtils.buildGenericValue((ACrisObject)cris, f.format(cris.getTimeStampInfo().getTimestampCreated().getTimestamp()), "fondscreationtime", 1);
        }
		return cris.getAnagrafica4view().get("fondscreationtime");
    }

}
