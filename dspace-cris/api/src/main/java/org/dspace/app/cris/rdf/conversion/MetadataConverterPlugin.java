/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */

package org.dspace.app.cris.rdf.conversion;
import org.dspace.utils.DSpace;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.rdf.RDFUtil;
import org.dspace.app.cris.rdf.RDFizer;
import org.dspace.app.util.MetadataExposure;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.app.cris.rdf.conversion.ConverterPlugin;
import org.dspace.app.cris.rdf.conversion.DMRM;
import org.dspace.app.cris.rdf.conversion.MetadataConverterPlugin;
import org.dspace.app.cris.rdf.conversion.MetadataRDFMapping;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.services.ConfigurationService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.OrganizationUnit;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class MetadataConverterPlugin implements ConverterPlugin
{
    public final static String METADATA_MAPPING_PATH_KEY = "rdf.metadata.mappings";
    public final static String METADATA_SCHEMA_URL_KEY = "rdf.metadata.schema";
    public final static String METADATA_PREFIXES_KEY = "rdf.metadata.prefixes";
    
    private final static Logger log = Logger.getLogger(MetadataConverterPlugin.class);
    protected ConfigurationService configurationService;
    
    @Override
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public Model convert(Context context, DSpaceObject dso)
            throws SQLException, AuthorizeException {
        String uri = RDFUtil.generateIdentifier(context, dso);
        if (uri == null)
        {
            log.error("Cannot create URI for " + dso.getTypeText() + " " 
                    + dso.getID() + " stopping conversion.");
            return null;
        }

        Model convertedData = ModelFactory.createDefaultModel();
        String prefixesPath = configurationService.getProperty(METADATA_PREFIXES_KEY);
        if (!StringUtils.isEmpty(prefixesPath))
        {	
            InputStream is = FileManager.get().open(prefixesPath);
            if (is == null)
            {
                log.warn("Cannot find file '" + prefixesPath + "', ignoring...");
            } else {
                convertedData.read(is, null, FileUtils.guessLang(prefixesPath));
                try {
                    is.close();
                }
                catch (IOException ex)
                {
                    // nothing to do here.
                }
            }
        }
        
        Model config = loadConfiguration();
        if (config == null)
        {
            log.error("Cannot load MetadataConverterPlugin configuration, "
                    + "skipping this plugin.");
            return null;
        }
        /*
        if (log.isDebugEnabled())
        {
            StringWriter sw = new StringWriter();
            sw.append("Inferenced the following model:\n");
            config.write(sw, "TURTLE");
            sw.append("\n");
            log.debug(sw.toString());
            try {
                sw.close();
            } catch (IOException ex) {
                // nothing to do here
            }
        }
        */

        ResIterator mappingIter = 
                config.listSubjectsWithProperty(RDF.type, DMRM.DSpaceMetadataRDFMapping);
        if (!mappingIter.hasNext())
        {
            log.warn("No metadata mappings found, returning null.");
            return null;
        }
        
        List<MetadataRDFMapping> mappings = new ArrayList<>();
        while (mappingIter.hasNext())
        {
            MetadataRDFMapping mapping = MetadataRDFMapping.getMetadataRDFMapping(
                    mappingIter.nextResource(), uri);
            if (mapping != null) mappings.add(mapping);
        }
        
        // should be changed, if Communities and Collections have metadata as well.
        if (!((dso instanceof Item) 
        		|| (dso instanceof Community) 
        		|| (dso instanceof Collection) 
        		|| (dso instanceof ResearcherPage) 
        		|| (dso instanceof Project) 
        		|| (dso instanceof OrganizationUnit) 
        		|| (dso instanceof ResearchObject))) //RO for dynamic types
        {
            log.error("This DspaceObject (" + dso.getTypeText() + " " 
                    + dso.getID() + ") should not have bin submitted to this "
                    + "plugin, as it supports Items only!");
            return null;
        }
        //Items
        //TODO: easier Way to Check Metadata, redundant code here
        //Check for ACrisObject
        //use collections to collect metadata-output
        log.debug("Collecting Metadata for Object " + dso.getHandle());
        
        ArrayList<Metadatum> metadata_values = new ArrayList<>();
        
        if(dso instanceof Item) {
            Item item = (Item) dso;
        	Collections.addAll(metadata_values, item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY));
        }
        if(dso instanceof Collection) {
        	Collection item = (Collection) dso;
        	Collections.addAll(metadata_values, item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY));
        }
        if(dso instanceof Community) {
        	Community item = (Community) dso;
        	Collections.addAll(metadata_values, item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY));
        }
        if(dso instanceof ResearcherPage) {
        	ResearcherPage item = (ResearcherPage) dso;
        	//no item.ANY-modifier for cris-entities, element has to be specified
        	//have a look at the CRIS-administration for the fields-name in the boxes
        	Collections.addAll(metadata_values, item.getMetadata("crisrp", "*", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisrp", "fullName", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisrp", "email", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisrp", "orcid", "*", "*"));
            Collections.addAll(metadata_values, item.getMetadata("crisrp", "personalsite", "*", "*"));
            Collections.addAll(metadata_values, item.getMetadata("crisrp", "persongender", "*", "*"));
            Collections.addAll(metadata_values, item.getMetadata("crisrp", "variants", "*", "*"));
            Collections.addAll(metadata_values, item.getMetadata("crisrp", "dept", "*", "*")); //link to OU
            Collections.addAll(metadata_values, item.getMetadata("crisrp", "dept", "name", "*")); //link to OU
            Collections.addAll(metadata_values, item.getMetadata("crisrp", "workgroups", "*", "*"));
            Collections.addAll(metadata_values, item.getMetadata("crisrp", "affiliation", "*", "*"));
            //metadata linking to other entities
            //Collections.addAll(metadata_values, item.getMetadata("crisrp", "mainproj", "title", "*"));
            //Collections.addAll(metadata_values, item.getMetadata("crisrp", "mainproj", "code", "*"));
            //Collections.addAll(metadata_values, item.getMetadata("crisrp", "mainproj", "*", "*"));
            //Collections.addAll(metadata_values, item.getMetadata("crisrp", "bsp_link", "*", "*"));
            //Collections.addAll(metadata_values, item.getMetadata("crisrp", "bsp_link", "journalsname", "*"));
            log.debug(metadata_values.size() + " metadata for " + item.getCrisID() + "found.");
        }
        if(dso instanceof Project) {
        	Project item = (Project) dso;
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "*", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "code", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "title", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "organization", "director", "*")); //link to OU
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "organization", "*", "*")); //link to OU
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "status", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "abstract", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisproject", "description", "*", "*"));
        	log.debug(metadata_values.size() + " metadata for " + item.getCrisID() + "found.");
        }
        if(dso instanceof OrganizationUnit) {
        	OrganizationUnit item = (OrganizationUnit) dso;
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "city", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "iso-3166-country", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "description", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "name", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "director", "*", "*")); //link to OU
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "director", "fullName", "*")); //link to RP
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "parentorgunit", "*", "*")); //link to OU
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "parentorgunit", "name", "*")); //link to OU
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "established", "*", "*")); //link to OU
        	Collections.addAll(metadata_values, item.getMetadata("crisou", "city", "*", "*"));
        	log.debug(metadata_values.size() + " metadata for " + item.getCrisID() + "found.");
        }
        if(dso instanceof ResearchObject) {
        	//for further control of types, the authorityPrefix could be checked.
        	ResearchObject item = (ResearchObject) dso;
        	//e.g. event
        	Collections.addAll(metadata_values, item.getMetadata("crisevents", "eventsname", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisevents", "eventslocation", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisevents", "eventsstartdate", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisevents", "eventsenddate", "*", "*"));
        	//e.g. journal
        	Collections.addAll(metadata_values, item.getMetadata("crisjournals", "journalsname", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisjournals", "journalsdescription", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisjournals", "journalsissn", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisjournals", "journalskeywords", "*", "*"));
        	//own defined entity with shortname exmp
        	Collections.addAll(metadata_values, item.getMetadata("crisexmp", "journalsname", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisexmp", "eventssname", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisexmp", "journalsdescription", "*", "*"));
        	Collections.addAll(metadata_values, item.getMetadata("crisexmp", "eventslocation", "*", "*"));
        	log.debug(metadata_values.size() + " metadata for " + item.getCrisID() + "found.");	
        	
        }
        log.debug(metadata_values.size() + " metadata values found");
        for (Metadatum value : metadata_values)
        {
        	
            String fieldname = value.schema + "." + value.element;
            if (value.qualifier != null) 
            {
                fieldname = fieldname + "." + value.qualifier;
            }
            if (MetadataExposure.isHidden(context, value.schema, value.element,
                    value.qualifier))
            {
                log.debug(fieldname + " is a hidden metadata field, won't "
                        + "convert it.");
                continue;
            }
            boolean converted = false;
            if (value.qualifier != null)
            {
                Iterator<MetadataRDFMapping> iter = mappings.iterator();
                while (iter.hasNext())
                {
                	String value_string = value.value;
                	Map<String,String> value_map = new HashMap<String,String>();
                	value_map.put("DSpaceValue", value_string);
                    MetadataRDFMapping mapping = iter.next();
                      	//TODO: authority-check
                    	//cris Entities pointing to other entities use the qualifier as the Pointing Value
                    	//e.g. crisrp.proj.*
                    	//e.g. crisrp.mainproj.title
                    	//resolve crisID saved in authority
                    	//resolveHandle and generate identifier
                    	//Assumption: DspaceObjects link to ACrisObjects as Authority values
                    	//no external authority values are used.
                    String authorityURIvalue = ""; 	
                    	if(value.authority != null && value.authority != "") {
                    		//lookup CrisObject from applicationService by ID
                    		ApplicationService as = new DSpace().getServiceManager().getServiceByName(
                                    "applicationService", ApplicationService.class);
                    		as.init();
                    		DSpaceObject aco = as.getEntityByCrisId(value.authority);
                    	    if(aco != null) {
                    	    	authorityURIvalue = RDFUtil.generateIdentifier(context, aco); 	
                        		if( authorityURIvalue != null && !authorityURIvalue.equals("")) {
                        			value_map.put("DSpaceAuthority", authorityURIvalue);
                            	}	
                    	    }
                    	}
                    	
                        if (mapping.matchesName(fieldname) &&
                        	(mapping.fulfills(value_string) ||
                        			mapping.fulfills(authorityURIvalue)))
                        {
                        	mapping.convert(value_map, value.language, uri, convertedData);
                        	converted = true;
                        }
                }
            }
            if (!converted)
            {
            	String value_string = value.value;
            	Map<String,String> value_map = new HashMap<String,String>();
            	value_map.put("DSpaceValue", value_string);
                String name = value.schema + "." + value.element;
                Iterator<MetadataRDFMapping> iter = mappings.iterator();
                while (iter.hasNext() && !converted)
                {
                    	MetadataRDFMapping mapping = iter.next();
                    	if(value.authority != null && value.authority != "") {
                    		//lookup CrisObject from applicationService by ID
                    		ApplicationService as = new DSpace().getServiceManager().getServiceByName(
                                    "applicationService", ApplicationService.class);
                    		as.init();
                    		DSpaceObject aco = as.getEntityByCrisId(value.authority);
                    	    if(aco != null) {
                    	    	String authorityURIvalue = RDFUtil.generateIdentifier(context, aco); 	
                        		if( authorityURIvalue != null && !authorityURIvalue.equals("")) {
                        			value_map.put("DSpaceAuthority", authorityURIvalue);
                            	}	
                    	    }
                    	}
                    	
                        if (mapping.matchesName(name) && mapping.fulfills(value_string))
                        {
                        	mapping.convert(value_map, value.language, uri, convertedData);
                        	converted = true;
                        }
                }
            }
            if (!converted)
            {
                log.debug("Did not convert " + fieldname + ". Found no "
                        + "corresponding mapping.");
            }
        }
        
        
        config.close();
        if (convertedData.isEmpty())
        {
            convertedData.close();
            return null;
        }
        return convertedData;
    }

    @Override
    public boolean supports(int type) {
        // should be changed, if Communities and Collections have metadata as well.
        return (type == Constants.ITEM || type == Constants.COLLECTION || type == Constants.COMMUNITY || type == CrisConstants.RP_TYPE_ID || type == CrisConstants.PROJECT_TYPE_ID || type == CrisConstants.OU_TYPE_ID || type >= CrisConstants.CRIS_DYNAMIC_TYPE_ID_START  ); //support for all dynamic objects
    }
    
    protected Model loadConfiguration()
    {
        String mappingPathes = configurationService.getProperty(METADATA_MAPPING_PATH_KEY);
        if (StringUtils.isEmpty(mappingPathes))
        {
            return null;
        }
        String[] mappings = mappingPathes.split(",\\s*");        
        if (mappings == null || mappings.length == 0)
        {
            log.error("Cannot find metadata mappings (looking for "
                    + "property " + METADATA_MAPPING_PATH_KEY + ")!");
            return null;
        }
        
        InputStream is = null;
        Model config = ModelFactory.createDefaultModel();
        for (String mappingPath : mappings)
        {
            is = FileManager.get().open(mappingPath);
            if (is == null)
            {
                log.warn("Cannot find file '" + mappingPath + "', ignoring...");
            }
            config.read(is, "file://" + mappingPath, FileUtils.guessLang(mappingPath));
            try {
                is.close();
            }
            catch (IOException ex)
            {
                // nothing to do here.
            }
        }
        if (config.isEmpty())
        {
            config.close();
            log.warn("Metadata RDF Mapping did not contain any triples!");
            return null;
        }
        
        String schemaURL = configurationService.getProperty(METADATA_SCHEMA_URL_KEY);
        if (schemaURL == null)
        {
            log.error("Cannot find metadata rdf mapping schema (looking for "
                    + "property " + METADATA_SCHEMA_URL_KEY + ")!");
        }
        if (!StringUtils.isEmpty(schemaURL))
        {
            log.debug("Going to inference over the rdf metadata mapping.");
            // Inferencing over the configuration data let us detect some rdf:type
            // properties out of rdfs:domain and rdfs:range properties
            // A simple rdfs reasoner is enough for this task.
            Model schema = ModelFactory.createDefaultModel();
            schema.read(schemaURL);
            Reasoner reasoner = ReasonerRegistry.getRDFSSimpleReasoner().bindSchema(schema);
            InfModel inf = ModelFactory.createInfModel(reasoner, config);

            // If we do inferencing, we can easily check for consistency.
            ValidityReport reports = inf.validate();
            if (!reports.isValid())
            {
                StringBuilder sb = new StringBuilder();
                sb.append("The configuration of the MetadataConverterPlugin is ");
                sb.append("not valid regarding the schema (");
                sb.append(DMRM.getURI());
                sb.append(").\nThe following problems were encountered:\n");
                for (Iterator<ValidityReport.Report> iter = reports.getReports();
                        iter.hasNext() ; )
                {
                    ValidityReport.Report report = iter.next();
                    if (report.isError)
                    {
                        sb.append(" - " + iter.next() + "\n");
                    }
                }
                log.error(sb.toString());
                return null;
            }
            return inf;
        }
        return config;
    }
    
}
