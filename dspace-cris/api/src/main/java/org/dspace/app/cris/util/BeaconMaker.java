/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;


import it.cilea.osd.jdyna.model.Property;

/**
 * Util for generating Beacon Files, used for simple sharing of links on standardized identifiers
 * like the GND
 * 
 * 
 * #FORMAT Beacon 
 * #Header 
 * Lines 
 * GND|Message|Link Text
 *  @author: Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class BeaconMaker {

	/** log4j logger */
	private static Logger log = Logger.getLogger(BeaconMaker.class);

	/** Configuration Settings*/
	/*These are general Settings for providing the file*/
	private boolean module_active = true;
	private String filepath;
	
	/*These are used when generating the file*/
	private String identifier;
	private String identifier_replacement;
	private boolean identifier_expand;
	private boolean entry_shortform; 
	
	private String link_type; 
	private boolean link_expand;
	private String link_url; 
	private String link_baseurl;
	private String feed_url;
	private String crisproperty;
	private boolean ignore_private;
	private String message_fix;
	
	BeaconMaker() {
		readGeneralConfig();
		readGeneratorConfig();
	}

	private static BeaconMaker instance = null;

	public static synchronized BeaconMaker getInstance() {
		if (instance == null) {
			instance = new BeaconMaker();
		}
		return instance;
	}

	private void readGeneralConfig() {
		//Enable / Disable Module
		this.module_active = ConfigurationManager.getBooleanProperty("beacon", "module.active", false);
		//Replacement Pattern in Identifier above
		this.filepath = ConfigurationManager.getProperty("beacon", "filepath");						
	}
	
	private void readGeneratorConfig() {
		this.link_baseurl = ConfigurationManager.getProperty("dspace.url");
		//Enable / Disable Module
		this.module_active = ConfigurationManager.getBooleanProperty("beacon", "module.active", false);
		//Show expanded Form (URI on line)
		this.link_expand = ConfigurationManager.getBooleanProperty("beacon", "link.expand", false);
		//Type of identifier (gnd/crisid/uuid)
		this.link_type = ConfigurationManager.getProperty("beacon", "link.type");
		//Url path to type id in dspace, e.g. /cris/ or /uuid/
		this.link_url = ConfigurationManager.getProperty("beacon", "link.url");
		//Shortform (without Name being displayed)
		this.entry_shortform = ConfigurationManager.getBooleanProperty("beacon", "entry.shortform", false);
		//Url to Feed
		this.feed_url = ConfigurationManager.getProperty("beacon", "feed.url");
		//Fixed Message
		this.message_fix = ConfigurationManager.getProperty("beacon", "message.fix");
		//Pattern of Identifier, e.g. 
		this.identifier = ConfigurationManager.getProperty("beacon", "identifier");
		//Expand Identifier (show on line)
		this.identifier_expand = ConfigurationManager.getBooleanProperty("beacon", "identifier.expand", false);
		//Replacement Pattern in Identifier above
		this.identifier_replacement = ConfigurationManager.getProperty("beacon", "identifier.replacement");
		//ignore private status of the properties
		this.ignore_private = ConfigurationManager.getBooleanProperty("beacon", "property.ignoreprivate", false);
		//comma-seperated list of Cris-Properties being checked
		this.crisproperty = ConfigurationManager.getProperty("beacon", "crisproperty");	
	}
	
	public String getData(boolean force_recreate, boolean dry_run) {
		if(checkFileExist() && force_recreate == false) {
		return getBeaconFile();
		}else {
		return generateFile(dry_run);
		}
	}
	
	private boolean checkFileExist() {
		File f = new File(filepath);
		return f.exists() && !f.isDirectory(); 
	}
	
	private int ageOfFileInAboutDays() {
		try {
		File f = new File(filepath);
		long date = f.lastModified();
		Date old = new Date(date);
		Date now = new Date();

	    long diffInMillies = Math.abs(now.getTime() - old.getTime());
	    long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
	    diff = TimeUnit.DAYS.convert(diff, TimeUnit.DAYS);
	    return (int) diff;
		}catch (Exception e) {

		}
		return -1;
		
	}
	/**
	 * Print Beacon File as String output
	 */
	private String generateFile(boolean dry_run) {
		if(!module_active) return "";
		
		readGeneratorConfig();
		StringBuilder sb = new StringBuilder();
		sb.append(createFixHeader());
		sb.append(readHeaderFile());
		sb.append(createMessageHeader());
		sb.append(createIdentifierHeader());
		sb.append(createLinkHeader());
		sb.append(createDynamicHeader());
		ArrayList<String> props = new ArrayList<String>();
		if(crisproperty.contains(",")) {
			props.addAll(Arrays.asList(crisproperty.split(",")));
		}else {
			props.add(crisproperty);
		}
		for(String prop : props) {
		try {
			//Loop for ResearchObjects -> person 
	    	DSpace dspace = new DSpace();
			SearchService searchService = dspace.getSingletonService(SearchService.class);
	        
			ApplicationService applicationService = dspace.getServiceManager().getServiceByName("applicationService",
					ApplicationService.class);
			applicationService.init();
			//Resourcetype has to be adapted to target resource
	        SolrQuery query = new SolrQuery("{!field f=search.resourcetype}" + "1001");
	        query.addFilterQuery(prop+":*");
	        query.addFilterQuery("withdrawn:false");
	        query.setRows(3000); //Set some limit here
	        //query.addSort(prop, ORDER.asc); Cannot sort on multivalued field
	        SolrDocumentList resultList;
	        try {
	            QueryResponse response = searchService.search(query);
	            resultList = response.getResults();
		    	for(SolrDocument doc : resultList) {
		    		if(doc.getFieldValue("cris-id") != null) {
		    		ACrisObject aco = applicationService.getEntityByCrisId((String)doc.getFieldValue("cris-id"));
		    		if(aco != null) sb.append(createLine(aco, props));
		    		}   
		    	}
	        } catch (SearchServiceException e) {
	            log.error(e);
	        }
			
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();

		}
		}
		String res = sb.toString();
		if(dry_run){
			return res;
		}
		//Save File
		try {
		      FileWriter myWriter = new FileWriter(filepath);
		      myWriter.write(res);
		      myWriter.close();
		      System.out.println("Successfully wrote Beacon file to " + filepath);
		    } catch (IOException e) {
		      System.out.println(e.getMessage());
		      log.error(e.getMessage());
		      e.printStackTrace();
		    }
		return res;
	}

	private String createLine(ACrisObject aco, List<String> props) {
		// we assume TextValue as Property type
		
		for(String prop : props) {
			prop = (((String[])prop.split("\\."))[1]).trim();
		try {
			List<Property> list = (List<Property>) aco.getAnagrafica4view().get(prop);
			if (list == null || list.isEmpty()) continue;
			Property value = list.get(0);
				if(ignore_private == false && value.getVisibility() == 0) continue;
				String val = normIdentifier(value.getValue().toString());
				StringBuilder sbline = new StringBuilder();
				if (createLineIdentifier(val) == null || createLineLink(aco, val) == null || createLineMessage(aco) == null) {
					throw new Exception("invalid line");
				}
				sbline.append(createLineIdentifier(val));
				//Shortform: just print identifier, since we have TARGET in the Header
				if(!entry_shortform) {
					sbline.append("|");
					sbline.append(createLineMessage(aco));
					sbline.append("|");
					sbline.append(createLineLink(aco, val));
				}
				sbline.append(System.lineSeparator());
				return sbline.toString();
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		}
		return "";
	}

	private String normIdentifier(String input) {
		//modified saved value. 
		//value might contains only the token or also the full url
		String identifier_base = identifier.replace(identifier_replacement, "");
		if (input.startsWith(identifier_base)) {
			input = input.replace(identifier_base, "");
		}
		input = identifier.replace(identifier_replacement, input );
		return input;
	}
	
	private String createLineMessage(ACrisObject aco) {
		if(message_fix.isEmpty()) {
		return aco.getName();
		}else {
			return "";
		}
	}

	private String createLineIdentifier(String idvalue) throws Exception {
		if(!identifier_expand) {
			String identifiershort = identifier.replace(identifier_replacement, "");
			idvalue = idvalue.replace(identifiershort, "");
			if(!idvalue.matches("[0-9X-]{6,11}")) throw new Exception ("invalid gnd Identifier");

		}
		return idvalue;
	}

	private String createLineLink(ACrisObject aco, String val) {
		// shorten link or long form
		StringBuilder sblink = new StringBuilder();
		if(!link_expand) {
		sblink.append(link_baseurl);
		}
		if (link_type.contentEquals("crisid")) {
			sblink.append(link_url).append(aco.getAuthorityPrefix()).append("/").append(aco.getCrisID());
		} else if (link_type.contentEquals("uuid")) {
			sblink.append(link_url).append("uuid/").append(aco.getUuid());
		} else if(link_type.contentEquals("gnd")){
			//create Link depending on gnd forwarding servlet
			String identifiershort = identifier.replace(identifier_replacement, "");
			val = val.replace(identifiershort, "");
			sblink.append(link_url).append(val);
		}
		return sblink.toString();
	}

	private String createIdentifierHeader() {
		if (!identifier_expand) {
			return "#PREFIX: " + identifier + System.lineSeparator();
		}
		return "";
	}

	private String createMessageHeader() {
		if(!message_fix.isEmpty()) {
		return "#MESSAGE: " + message_fix + System.lineSeparator(); 
		}else {
			return "";
		}
	}

	private String createLinkHeader() {
		if(entry_shortform) {
			return "#TARGET: " + link_baseurl + link_url + identifier_replacement+ System.lineSeparator();
		}
		if (!link_expand) {
			return "";
		} else {
			return "#TARGET: " + link_baseurl + link_url + identifier_replacement+ System.lineSeparator();
		}
	}

	private String createFixHeader() {
		return "#FORMAT: Beacon" + System.lineSeparator();
	}

	private String createDynamicHeader() {
		StringBuilder sbheader = new StringBuilder();
		//add current Timestamp:
		TimeZone tz = TimeZone.getTimeZone("Europe/Berlin");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ"); // Qu0oted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		sbheader.append("#FEED: " + feed_url).append(System.lineSeparator());
		sbheader.append("#TIMESTAMP: " + nowAsISO).append(System.lineSeparator());	
		return sbheader.toString();
	}
	
    protected String readHeaderFile()
    {
    	String path = ConfigurationManager.getProperty("beacon", "header.fix.path");
        if (path == null)
        {
            log.error("Cannot find beacon header file");
            return "";
        }
        
        log.debug("Going to read static data from file '" + path + "'.");
        StringBuilder sb = new StringBuilder();
        try {
  	      File file = new File(path);
  	      BufferedReader br = new BufferedReader(new FileReader(file));
  	    for(String line; (line = br.readLine()) != null; ) {
  	        	if (!line.startsWith("#")) {
  	        		break;
  	        	}else {
  	        		if(!line.endsWith(System.lineSeparator())) {
  	        			line += System.lineSeparator();
  	        		}
  	        		sb.append(line);
  	        	}
  	      }
  	      br.close();
  	      return sb.toString();
  	    } catch (IOException e) {
  	      System.out.println(e.getMessage());
  	      log.error(e.getMessage());
  	      return sb.toString();
	            }
    }
	
    private String getBeaconFile(){
    	StringBuilder sb = new StringBuilder();
    	try {
    	      File file = new File(filepath);
    	      
    	      BufferedReader br = new BufferedReader(new FileReader(file));
    	  	    for(String line; (line = br.readLine()) != null; ) {
    	  	      sb.append(line);
    	  	      if(!line.endsWith(System.lineSeparator())) {
	        			sb.append(System.lineSeparator());
	        		}
    	  	    }
    	  	    br.close();
    	  	    return sb.toString();
    	    } catch (FileNotFoundException e) {
    	      System.out.println(e.getMessage());
    	      log.error(e.getMessage());
    	      return sb.toString();
    	    } catch (IOException e) {
    	    	System.out.println(e.getMessage());
      	      log.error(e.getMessage());
      	       return sb.toString();
			}
    }
	
	/**
	 * Command Line Beacon Generator for beacon launcher
	 */
	public static void main(String[] args) {
		// update / recreate file
		// dryrun without updating
		// print out actual file
		
		BeaconMaker bm = BeaconMaker.getInstance();
		
	      // prepare CLI and parse arguments
        Options options = new Options();

        options.addOption("h", "help", false, "Print usage information and exit.");
        options.addOption("n", "dry-run", false, "Generate, but do not save");
        options.addOption("f", "force-recreate", false, "Force Recreation");
        options.addOption("t", "time", true, "Recreate only, if the existent file is older than about <opionvalue> days");
		
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        try 
        {
            line = parser.parse(options, args);
        } 
        catch (org.apache.commons.cli.ParseException ex)
        {
            System.err.println(ex.getMessage());
            log.fatal(ex);
            System.exit(1);
        } 

        if (line.hasOption("help"))
        {
        	String cliSyntax = "[dspace-bin]/bin/dspace beacon [OPTIONS...]";
            String header = "";
            String footer = "You cannot use dry-run and force-recreate together.";
        	  PrintWriter err = new PrintWriter(System.err);
              HelpFormatter helpformater = new HelpFormatter();
              helpformater.printHelp(err, 79, cliSyntax, header, options, 2, 2, footer);
              err.flush();
              System.exit(1);
        }
        boolean dryrun = false;
        if (line.hasOption("dry-run"))
        {
        	dryrun = true;
        }
        boolean forcerecreate = false;
        if (line.hasOption("force-recreate"))
        {
        	forcerecreate = true;
        }
        if (line.hasOption("time"))
        { try {
        	int period = Integer.parseInt(line.getOptionValue("time"));
        	int agefile = bm.ageOfFileInAboutDays();
        	if(agefile < period) {
        		System.out.println("Beacon File is " + agefile + " days old. Nothing to update yet, the Limit is " + period + " days");
        		System.exit(1);
        	}
        }catch (Exception e) {
        	
		}
        }
        if(dryrun && forcerecreate) {
        	System.err.println("Unallowed Argument combinations: dryrun+force recreate");
            System.exit(1);
        }

		System.out.println(bm.getData(forcerecreate, dryrun));
	}
	
	  	
}
