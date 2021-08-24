/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery.tree;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.discovery.CrisServiceIndexPlugin;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.ConfigurationManager;
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
public class TreeViewIndexPlugin
        implements CrisServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(TreeViewIndexPlugin.class);

    private ApplicationService applicationService;

    private TreeViewConfigurator configurator;

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
        String result = "";
        //multiple parent metadata (but retrieve only the first value)
        List<String> metadataKeyParents = configurator.getParents().get(crisObject.getAuthorityPrefix());
        
        String metadataKeyParent = metadataKeyParents.get(0);
        
        for(String metadataKeyParentTmp : metadataKeyParents) {
            List<P> ppParent = crisObject.getAnagrafica4view().get(metadataKeyParentTmp); 
            if(ppParent != null && !ppParent.isEmpty()) {
                metadataKeyParent = metadataKeyParentTmp;
                break;
            }
        }

        result = getRootInfo(crisObject, metadataKeyParent);
        document.addField("treeroot_s", result);
        document.addField("treecontext_s", crisObject.getTypeText());
        
        Map<String, String> metadataNodeClosedProp = configurator.getClosed().get(crisObject.getAuthorityPrefix());
        
        boolean found = false;
        if (metadataNodeClosedProp != null)
        {
        	external : for(String metadataNodeClosed : metadataNodeClosedProp.keySet()) {
        		if(StringUtils.isNotBlank(metadataNodeClosed)) {
        			String nodeIsClosed = crisObject.getMetadata(metadataNodeClosed);
        			if(StringUtils.isNotBlank(nodeIsClosed)) {
        				if(nodeIsClosed.equals(metadataNodeClosedProp.get(metadataNodeClosed))) {
        					found = true;
        					break external;
        				}
        			}
        		}
        		
        	}
		}

        //it's a boolean value
        document.addField("treenodeclosed_b", found);
        
        //multiple leaf metadata (but retrieve only the first value)
        List<String> metadataKeyLeafs = configurator.getLeafs().get(crisObject.getAuthorityPrefix());
        if(metadataKeyLeafs!=null) {
            String metadataKeyLeaf = metadataKeyLeafs.get(0);
    
            for(String metadataKeyLeafTmp : metadataKeyLeafs) {
                List<P> ppLeaf = crisObject.getAnagrafica4view().get(metadataKeyLeafTmp); 
                if(ppLeaf != null && !ppLeaf.isEmpty()) {
                    metadataKeyLeaf = metadataKeyLeafTmp;
                    break;
                }
            }
        
            List<P> ppLeaf = crisObject.getAnagrafica4view().get(metadataKeyLeaf);
            for (P metadata : ppLeaf) {
                //it's a boolean value
                document.addField("treeleaf_b", metadata.getValue().getObject());
                break;
            }
        }

        List<P> ppParent = crisObject.getAnagrafica4view().get(metadataKeyParent);
        for (P metadata : ppParent)
        {
            PointerValue val = ((PointerValue) metadata.getValue());
            ACrisObject aCrisObject = (ACrisObject) val.getObject();
            document.addField("treeparent_s", aCrisObject.getCrisID());
            break;
        }
        
        addRootInfo(crisObject, metadataKeyParent, document);
    }

    private <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> String getRootInfo(
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
        return crisObject.getCrisID();
    }

    private <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void addRootInfo(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            String metadataKey, SolrInputDocument document)
    {
        List<P> pp = crisObject.getAnagrafica4view().get(metadataKey);
        for (P metadata : pp)
        {
            PointerValue val = ((PointerValue) metadata.getValue());
            ACrisObject aCrisObject = (ACrisObject) val.getObject();
            document.addField("treeparents_mvuntokenized", getRootInfo(aCrisObject, metadataKey));
        }
        document.addField("treeparents_mvuntokenized", crisObject.getCrisID());
    }
    
    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACNO dso, SolrInputDocument sorlDoc, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        // TODO NOT SUPPORTED OPERATION
    }

    public void setConfigurator(TreeViewConfigurator configurator)
    {
        this.configurator = configurator;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

}
