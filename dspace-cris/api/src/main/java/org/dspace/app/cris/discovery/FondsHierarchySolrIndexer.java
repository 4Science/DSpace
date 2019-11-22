package org.dspace.app.cris.discovery;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.Metadatum;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public class FondsHierarchySolrIndexer implements CrisServiceIndexPlugin
{

        private static final Logger log = Logger
                .getLogger(FondsHierarchySolrIndexer.class);

        @Override
        public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
                ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
                SolrInputDocument sorlDoc,
                Map<String, List<DiscoverySearchFilter>> searchFilters)
        {
            String fondshierarchy=null;
            if(crisObject instanceof ResearchObject) {
                
                ApplicationService appService = new DSpace().getServiceManager()
                        .getServiceByName("applicationService",
                                ApplicationService.class);
                
                Metadatum[] fondsparent = crisObject.getMetadata("crisfonds" , "fondsparent", null, null);
                fondshierarchy = crisObject.getMetadata("fondsname");
                
                    while(fondsparent.length != 0) {
                    ResearchObject ro = appService.getEntityByCrisId(fondsparent[0].authority);
                    fondshierarchy = ro.getMetadata("fondsparent") + " > " + fondshierarchy;
                    fondsparent = crisObject.getMetadata("crisfonds" , "fondsparent", null, null);
                        }
                }
            sorlDoc.addField(crisObject.getTypeText() + ".fondshierarchy", fondshierarchy);
        }
        
        @Override
        public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
                ACNO dso, SolrInputDocument sorlDoc,
                Map<String, List<DiscoverySearchFilter>> searchFilters)
        {
            // TODO Auto-generated method stub
            
        }
       
}