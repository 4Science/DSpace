/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.value.PointerValue;

/**
 * @author Luigi Andrea Pascarelli
 *
 */
public class CrisPathIndexPlugin
        implements CrisServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(CrisPathIndexPlugin.class);

    private ApplicationService applicationService;

    private ConfiguratorResource configurator;

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        if (crisObject != null)
        {
            if (configurator.getEnabled()
                    .containsKey(crisObject.getAuthorityPrefix()))
            {
                buildTree(crisObject, document);
            }
        }
    }

    private <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void buildTree(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            SolrInputDocument document)
    {
        String metadataKeyParent = crisObject.getAuthorityPrefix() + "parent";
        String metadataKeyLeaf = crisObject.getAuthorityPrefix() + "leaf";
        ACrisObject root = getRootInfo(crisObject, metadataKeyParent);
        document.addField("treeroot_s", root.getCrisID());
        document.addField("treerootname_s", root.getName());
        document.addField("treecontext_s", crisObject.getTypeText());
        List<P> ppLeaf = crisObject.getAnagrafica4view().get(metadataKeyLeaf);
        for (P metadata : ppLeaf) {
            document.addField("treeleaf_b", metadata.getValue().getObject());
        }

        List<P> ppParent = crisObject.getAnagrafica4view().get(metadataKeyParent);
        for (P metadata : ppParent)
        {
            PointerValue val = ((PointerValue) metadata.getValue());
            ACrisObject aCrisObject = (ACrisObject) val.getObject();
            document.addField("treeparent_s", aCrisObject.getCrisID());
            break;
        }
    }

    private <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> ACrisObject getRootInfo(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            String metadataKey)
    {
        List<P> pp = crisObject.getAnagrafica4view().get(metadataKey);
        for (P metadata : pp)
        {
            PointerValue val = ((PointerValue) metadata.getValue());
            ACrisObject aCrisObject = (ACrisObject) val.getObject();
            return getRootInfo(aCrisObject, metadataKey);
        }
        return crisObject;
    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACNO dso, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        // TODO NOT SUPPORTED OPERATION
    }

    public void setConfigurator(ConfiguratorResource configurator)
    {
        this.configurator = configurator;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

}
