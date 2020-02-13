/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ICrisObject;
import org.dspace.util.MultiFormatDateParser;

public class CrisDateEnhancer extends CrisEnhancer
{
    private static final Logger log = Logger.getLogger(CrisDateEnhancer.class);

    public <P extends Property<TP>, TP extends PropertiesDefinition> List<P> getProperties(
            ICrisObject<P, TP> cris, String qualifier)
    {
        List<P> props = cris.getAnagrafica4view().get(alias);
        List<P> validProps = new ArrayList<>();
        for (P prop : props) {
            String svalue = prop.toString();
            Date date = MultiFormatDateParser.parse(svalue);
            if(date != null) {
                String formatValue = DateFormatUtils.formatUTC(date, "yyyy-MM-dd");
                if (date.getYear() < 3000) {
                    validProps.add(prop);
                }
            }
        }

        return validProps;
    }

}
