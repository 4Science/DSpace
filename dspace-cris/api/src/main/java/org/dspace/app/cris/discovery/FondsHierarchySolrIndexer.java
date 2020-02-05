package org.dspace.app.cris.discovery;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public class FondsHierarchySolrIndexer implements CrisServiceIndexPlugin
{
    
    DSpace dspace = new DSpace();
    ApplicationService appService = dspace.getServiceManager().getServiceByName("ApplicationService", ApplicationService.class);
    


    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACNO dso, SolrInputDocument sorlDoc,
            Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            SolrInputDocument sorlDoc,
            Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        sorlDoc.addField(crisObject.getTypeText()+"."+ResearcherPageUtils.FONDS+ResearcherPageUtils.HIERARCHY,  ResearcherPageUtils.getDisplayEntry(crisObject));
    }

    
}