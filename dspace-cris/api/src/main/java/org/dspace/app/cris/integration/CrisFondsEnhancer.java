/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.model.AValue;
import it.cilea.osd.jdyna.model.AnagraficaObject;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.value.DateValue;
import it.cilea.osd.jdyna.value.TextValue;
import it.cilea.osd.jdyna.widget.WidgetTesto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.symmetric.AES.OFB;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ICrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.openaireproject.OpenAIREProjectService;
import org.dspace.content.DCDate;

public class CrisFondsEnhancer extends CrisEnhancer
{
	private static final Logger log = Logger.getLogger(CrisFondsEnhancer.class);
	

    public <P extends Property<TP>, TP extends PropertiesDefinition> List<P> getProperties(
            ICrisObject<P, TP> cris, String qualifier)
    {
    	List<P> results = new ArrayList<P>();
    	
        // TODO: formattare la data (trasformarla in stringa)
        // 2019-09-13T10:30:37Z
        // TODO: passare per TextValue
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		
        List<P> p = cris.getAnagrafica4view().get("fondscreationtime");
        if (p == null || p.isEmpty() || StringUtils.isBlank(p.get(0).toString())) {
        	ResearcherPageUtils.buildGenericValue((ACrisObject)cris, f.format(cris.getTimeStampInfo().getTimestampCreated().getTimestamp()), "fondscreationtime", 1);
        }
		return cris.getAnagrafica4view().get("fondscreationtime");
    }

}
