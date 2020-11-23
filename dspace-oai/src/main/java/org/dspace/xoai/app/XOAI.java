/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import static com.lyncode.xoai.dataprovider.core.Granularity.Second;
import static org.dspace.xoai.util.ItemUtils.retrieveMetadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.util.MetadatumAuthorityDecorator;
import org.dspace.app.cris.util.UtilsCrisMetadata;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.utils.DSpace;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.database.CollectionsService;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.DSpaceSolrIndexerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.lyncode.xoai.dataprovider.exceptions.ConfigurationException;
import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

/**
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class XOAI {
    public static final String ITEMTYPE_DEFAULT = "item";

    public static final String ITEMTYPE_SPECIAL = "cfitem";

    private static Logger log = LogManager.getLogger(XOAI.class);

    private Context context;
    private boolean optimize;
    private boolean verbose;
    private boolean clean;

    @Autowired
    private SolrServerResolver solrServerResolver;
    @Autowired
    private CollectionsService collectionsService;


    private static List<String> getFileFormats(Item item) {
        List<String> formats = new ArrayList<String>();
        try {
            for (Bundle b : item.getBundles("ORIGINAL")) {
                for (Bitstream bs : b.getBitstreams()) {
                    if (!formats.contains(bs.getFormat().getMIMEType())) {
                        formats.add(bs.getFormat().getMIMEType());
                    }
                }
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
        return formats;
    }

    public XOAI(Context context, boolean optimize, boolean clean, boolean verbose) {
        this.context = context;
        this.optimize = optimize;
        this.clean = clean;
        this.verbose = verbose;
    }

    public XOAI(Context ctx, boolean hasOption) {
        context = ctx;
        verbose = hasOption;
    }

    private void println(String line) {
        System.out.println(line);
    }

    public int index(String idxType) throws DSpaceSolrIndexerException {
        int result = 0;
        try {

            if (clean) {
                clearIndex(idxType);
                System.out.println("Using full import.");
                result = this.indexAll(idxType);
            } else {
                SolrQuery solrParams = new SolrQuery("*:*")
                        .addField("item.lastmodified")
                        .addSortField("item.lastmodified", ORDER.desc).setRows(1);

                SolrDocumentList results = DSpaceSolrSearch.query(solrServerResolver.getServer(), solrParams);
                if (results.getNumFound() == 0) {
                    System.out.println("There are no indexed documents, using full import.");
                    result = this.indexAll(idxType);
                } else {
                    result = this.index(idxType, (Date) results.get(0).getFieldValue("item.lastmodified"));
                }
            }
            solrServerResolver.getServer().commit();


            if (optimize) {
                println("Optimizing Index");
                solrServerResolver.getServer().optimize();
                println("Index optimized");
            }

            return result;
        } catch (DSpaceSolrException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (SolrServerException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    /***
     * index all data whose modification date is greater than given start date.
     * 
     * @param idxType The index type (item, rp, ...)
     * @param start The latest date
     * @return The number of indexed fields.
     * @throws DSpaceSolrIndexerException
     */
    private int index(String idxType, Date start) throws DSpaceSolrIndexerException {
        System.out
                .println("Incremental import. Searching for documents modified after: "
                        + start.toString());

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        
        String discoverQuery = "lastModified:{%s TO *}";
        discoverQuery = String.format(discoverQuery, df.format(start));
        switch (idxType) {
        case ITEMTYPE_DEFAULT:
        case "rp":
        case "project":
        case "ou":
        case "other":
        case "all":
            break;
        default:
            throw new DSpaceSolrIndexerException("The partial index is not supported for type " + idxType);
        }
    	discoverQuery += " AND " + buildQuery(idxType);

        try {
            return indexWithQuery(discoverQuery);
        } catch (Exception ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    /***
     * index all data
     * 
     * @param idxType The index type (item, rp, ...)
     * @return The number of indexed fields
     * @throws DSpaceSolrIndexerException
     */
    private int indexAll(String idxType) throws DSpaceSolrIndexerException {
        System.out.println("Full import");
        try {
        	String discoverQuery = buildQuery(idxType);
            
            return indexWithQuery(discoverQuery);
        } catch (Exception ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }
    
    /***
     * Build the query used to retrieve the data of give type idxType.
     * 
     * The query can be overridden using:
     * 	oai.discover.query.item
     * 	oai.discover.query.crisrp
     * 	oai.discover.query.crisproject
     * 	oai.discover.query.crisou
     * 	oai.discover.query.cris<OtherObject>
     * 
     * @param idxType The index type
     * @return The number of indexed fields.
     * @throws DSpaceSolrIndexerException
     */
    private String buildQuery(String idxType) throws DSpaceSolrIndexerException {
    	String discoverQuery = "";
    	
    	switch (idxType) {
        case ITEMTYPE_DEFAULT:
        	discoverQuery = ConfigurationManager.getProperty("oai", "oai.discover.query.item");
        	if (discoverQuery == null || discoverQuery.trim().length() <= 0) {
        		discoverQuery = "discoverable:true AND search.resourcetype:2";
        	}
	        break;
        case "rp":
        	discoverQuery = ConfigurationManager.getProperty("oai", "oai.discover.query.crisrp");
        	if (discoverQuery == null || discoverQuery.trim().length() <= 0) {
        		discoverQuery = "discoverable:true AND search.resourcetype:" + CrisConstants.RP_TYPE_ID;
        	}
	        break;
        case "project":
        	discoverQuery = ConfigurationManager.getProperty("oai", "oai.discover.query.crisproject");
        	if (discoverQuery == null || discoverQuery.trim().length() <= 0) {
        		discoverQuery = "discoverable:true AND search.resourcetype:" + CrisConstants.PROJECT_TYPE_ID;
        	}
	        break;
        case "ou":
        	discoverQuery = ConfigurationManager.getProperty("oai", "oai.discover.query.crisou");
        	if (discoverQuery == null || discoverQuery.trim().length() <= 0) {
        		discoverQuery = "discoverable:true AND search.resourcetype:" + CrisConstants.OU_TYPE_ID;
        	}
	        break;
        case "other":
        	discoverQuery = ConfigurationManager.getProperty("oai", "oai.discover.query.crisother");
        	if (discoverQuery == null || discoverQuery.trim().length() <= 0) {
        		discoverQuery = "discoverable:true AND search.resourcetype:{" + CrisConstants.CRIS_DYNAMIC_TYPE_ID_START + " TO *}";
        	}
	        break;
        case "all":
        	discoverQuery = ConfigurationManager.getProperty("oai", "oai.discover.query.all");
        	if (discoverQuery == null || discoverQuery.trim().length() <= 0)
        		discoverQuery = "discoverable:true";
        	break;
    	default:
    		throw new DSpaceSolrIndexerException("Index is not supported for type " + idxType);
    	}
    	return discoverQuery;
    }
    
    /***
     * Paged solr query used to retrieve data to index.
     * The page size can be modified using:
     * 	oai.discover.pagesize
     * 
     * @param solrQuery The query
     * @return The number of indexed data.
     * @throws DSpaceSolrIndexerException
     */
    private int indexWithQuery(String solrQuery) throws DSpaceSolrIndexerException {
    	String discoverPageSize = "";
		try {
			int pageSize = -1;
			int total = 0;
			int read = -1;
	    	
	    	discoverPageSize = ConfigurationManager.getProperty("oai", "oai.discover.pagesize");
	    	if (discoverPageSize == null || discoverPageSize.trim().length() <= 0)
	    		pageSize = 100;
	    	else {
	    		pageSize = Integer.parseInt(discoverPageSize);
	    	}
	    	
	    	DiscoverResult results = null;
	    	int page = 0;
	    	int offset;
	    	
	    	do {
	    		offset = page > 0 ? page * pageSize : 0;
			
	    		DiscoverQuery query = new DiscoverQuery();
	    		query.setQuery(solrQuery);
	    		query.setMaxResults(pageSize);
	    		query.setStart(offset);
	    		query.addSearchField("item.cerifentitytype");
			 	results = SearchUtils.getSearchService().search(context, query, true);
			 	read = 0;
			 	if (!results.getDspaceObjects().isEmpty())
			 		read = indexResults(results, total);
			 	page++;
			 	total += read;
	    	}
	    	while (read == pageSize);
	    	
	    	System.out.println("Total: " + total + " items");
			return total;
		} catch (SearchServiceException e) {
			String message = "Error while processing solr query results: " + e.getMessage();
			log.error(message, e);
			throw new DSpaceSolrIndexerException(message, e);
		} catch (DSpaceSolrIndexerException e) {
			String message = "Error while processing solr query results: \" + e.getMessage()";
			log.error(message, e);
			throw new DSpaceSolrIndexerException(message, e);
		} catch (NumberFormatException e) {
			String message = "Error in option oai.discover.pagesize: \" + discoverPageSize + \". \" + e.getMessage()";
			log.error(message, e);
			throw new DSpaceSolrIndexerException(message, e);
		}
    }

    /***
     * Read one page of data.
     * 
     * @param result The paged data
     * @param subtotal The number of data processed so far.
     * @return The number of indexed data.
     * @throws DSpaceSolrIndexerException
     */
    @SuppressWarnings("rawtypes")
	private int indexResults(DiscoverResult result, int subtotal)
            throws DSpaceSolrIndexerException {
        try {
            int i = 0;
            SolrServer server = solrServerResolver.getServer();
            for (DSpaceObject o : result.getDspaceObjects()) {
                try {
                	SolrInputDocument solrDoc = null;
                	boolean doublingSolrDocument = false;
                	if (o instanceof Item) {
                	    Item item = (Item)o;
                	    String type = (String)item.getExtraInfo().get("item.cerifentitytype");
                	    solrDoc = this.indexResults(item, false);
                	    if(StringUtils.isNotBlank(type)) {
                	        doublingSolrDocument = true;
                	    }
                	}
                	else if (o instanceof ACrisObject) {
                		solrDoc = this.indexResults((ACrisObject)o);
                	}
                	server.add(solrDoc);
                	
                	if(doublingSolrDocument) {
                	    Item item = (Item)o;
                	    solrDoc = this.indexResults(item, true);
                	    server.add(solrDoc);
                	}
                    context.clearCache();
                } catch (SQLException ex) {
                    log.error(ex.getMessage(), ex);
                } catch (MetadataBindException e) {
                    log.error(e.getMessage(), e);
                } catch (ParseException e) {
                    log.error(e.getMessage(), e);
                } catch (XMLStreamException e) {
                    log.error(e.getMessage(), e);
                } catch (WritingXmlException e) {
                    log.error(e.getMessage(), e);
                }
                i++;
                if ((i+subtotal) % 100 == 0) System.out.println((i+subtotal) + " items imported so far...");
            }
            System.out.println("Partial Total: " + (i+subtotal) + " items");
            server.commit();
            return i;
        } catch (SolrServerException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    /***
     * Index one item
     * 
     * @param item The item
     * @return The sorl document
     * @throws SQLException
     * @throws MetadataBindException
     * @throws ParseException
     * @throws XMLStreamException
     * @throws WritingXmlException
     */
    private SolrInputDocument indexResults(Item item, boolean specialIdentifier) throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("item.id", item.getID());
        boolean pub = this.isPublic(item);
        doc.addField("item.public", pub);
        String handle = item.getHandle();
        if (verbose) {
            println("Prepare handle " + handle);
        }
        
        String type = (String)item.getExtraInfo().get("item.cerifentitytype");
        if(StringUtils.isNotBlank(type) && specialIdentifier) {
            doc.addField("item.identifier", type +"/"+ handle);
            doc.addField("item.type", ITEMTYPE_SPECIAL);
        }
        else {
            doc.addField("item.identifier", handle);
            doc.addField("item.type", ITEMTYPE_DEFAULT);
        }
        
        doc.addField("item.handle", handle);
        doc.addField("item.lastmodified", item.getLastModified());
        if (item.getSubmitter() != null) {
            doc.addField("item.submitter", item.getSubmitter().getEmail());
        }
        doc.addField("item.deleted", item.isWithdrawn() ? "true" : "false");
        for (Collection col : item.getCollections())
            doc.addField("item.collections",
                    "col_" + col.getHandle().replace("/", "_"));
        for (Community com : collectionsService.flatParentCommunities(item))
            doc.addField("item.communities",
                    "com_" + com.getHandle().replace("/", "_"));

        Metadatum[] allData = item.getMetadata(Item.ANY, Item.ANY, Item.ANY,
                Item.ANY);
        for (Metadatum dc : allData) {
            String key = "metadata." + Utils.standardize(dc.schema, dc.element, dc.qualifier, ".");
            
            String val =StringUtils.equals(dc.value, MetadataValue.PARENT_PLACEHOLDER_VALUE)? "N/D":dc.value;  

            doc.addField(key, val);
            if (dc.authority != null) {
                doc.addField(key + ".authority", dc.authority);
                doc.addField(key + ".confidence", dc.confidence + "");
            }
        }

        for (String f : getFileFormats(item)) {
            doc.addField("metadata.dc.format.mimetype", f);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutputContext xmlContext = XmlOutputContext.emptyContext(out, Second);
        Metadata metadata = null;
        if(StringUtils.isNotBlank(type) && specialIdentifier) {
            metadata = retrieveMetadata(context, item, true);
        }
        else {
            metadata = retrieveMetadata(context, item, false);
        }

        //Do any additional content on "item.compile" field, depends on the plugins
        List<XOAIItemCompilePlugin> xOAIItemCompilePlugins = new DSpace().getServiceManager().getServicesByType(XOAIItemCompilePlugin.class);
        for (XOAIItemCompilePlugin xOAIItemCompilePlugin : xOAIItemCompilePlugins)
        {
            metadata = xOAIItemCompilePlugin.additionalMetadata(context, metadata, item);
        }

        metadata.write(xmlContext);
        xmlContext.getWriter().flush();
        xmlContext.getWriter().close();
        doc.addField("item.compile", out.toString());

        if (verbose) {
            println(String.format("Item %d with handle %s indexed",
                    item.getID(), handle));
        }


        return doc;
    }
    
    /***
     * Index one cris item
     * 
     * @param item The cris item
     * @return The sorl document
     * @throws SQLException
     * @throws MetadataBindException
     * @throws ParseException
     * @throws XMLStreamException
     * @throws WritingXmlException
     */
    @SuppressWarnings("rawtypes")
    private SolrInputDocument indexResults(ACrisObject item) throws SQLException, MetadataBindException, ParseException, XMLStreamException, WritingXmlException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("item.id", item.getID());
        boolean pub = item.getStatus();
        doc.addField("item.public", pub);
        String handle = item.getHandle();
        if (verbose) {
            println("Prepare handle " + handle);
        }
        
        String type = ConfigurationManager.getProperty("oai", "identifier.cerifentitytype." + item.getPublicPath());
        if(StringUtils.isNotBlank(type)) {
            doc.addField("item.identifier", type + "/" + handle);    
        }
        else {
            doc.addField("item.identifier", handle);
        }
        
        doc.addField("item.handle", handle);
        doc.addField("item.type", item.getPublicPath());
        doc.addField("item.lastmodified", item.getLastModified());
        doc.addField("item.deleted", "false");

		MetadatumAuthorityDecorator[] allDataAuthDec = UtilsCrisMetadata.getAllMetadata(item, true, true, "oai");
        if (allDataAuthDec != null)
        {
            for (MetadatumAuthorityDecorator dcAuthDec : allDataAuthDec)
            {
            	Metadatum dc = dcAuthDec.getMetadatum();
                String key = "metadata." + Utils.standardize(dc.schema, dc.element, dc.qualifier, ".");

                String val = StringUtils.equals(dc.value,
                        MetadataValue.PARENT_PLACEHOLDER_VALUE) ? "N/D"
                                : dc.value;

                doc.addField(key, val);
                if (dc.authority != null)
                {
                    doc.addField(key + ".authority", dc.authority);
                    doc.addField(key + ".confidence", dc.confidence + "");
                }
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutputContext xmlContext = XmlOutputContext.emptyContext(out, Second);
        retrieveMetadata(context, item).write(xmlContext);
        xmlContext.getWriter().flush();
        xmlContext.getWriter().close();
        doc.addField("item.compile", out.toString());

        if (verbose) {
            println(String.format("Cris Item %s with handle %s indexed (type: %s)",
                    item.getCrisID(), handle, item.getTypeText()));
        }

        return doc;
    }


    private boolean isPublic(Item item) {
        boolean pub = false;
        try {
            //Check if READ access allowed on this Item
            pub = AuthorizeManager.authorizeActionBoolean(context, item, Constants.READ);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
        }
        return pub;
    }


    private static boolean getKnownExplanation(Throwable t) {
        if (t instanceof ConnectException) {
            System.err.println("Solr server ("
                    + ConfigurationManager.getProperty("oai", "solr.url")
                    + ") is down, turn it on.");
            return true;
        }

        return false;
    }

    private static boolean searchForReason(Throwable t) {
        if (getKnownExplanation(t))
            return true;
        if (t.getCause() != null)
            return searchForReason(t.getCause());
        return false;
    }

    private void clearIndex(String idxType) throws DSpaceSolrIndexerException {
        try {
            System.out.println("Clearing index");
            
            String eraseQuery = "";
            switch (idxType) {
            case ITEMTYPE_DEFAULT:
            	eraseQuery = ConfigurationManager.getProperty("oai", "oai.erase.query.item");
            	if (eraseQuery == null || eraseQuery.trim().length() <= 0) {
            		eraseQuery = "item.type:item";
            	}
    	        break;
            case "rp":
            	eraseQuery = ConfigurationManager.getProperty("oai", "oai.erase.query.crisrp");
            	if (eraseQuery == null || eraseQuery.trim().length() <= 0) {
            		eraseQuery = "item.type:rp";
            	}
    	        break;
            case "project":
            	eraseQuery = ConfigurationManager.getProperty("oai", "oai.erase.query.crisproject");
            	if (eraseQuery == null || eraseQuery.trim().length() <= 0) {
            		eraseQuery = "item.type:project";
            	}
    	        break;
            case "ou":
            	eraseQuery = ConfigurationManager.getProperty("oai", "oai.erase.query.crisou");
            	if (eraseQuery == null || eraseQuery.trim().length() <= 0) {
            		eraseQuery = "item.type:ou";
            	}
    	        break;
            case "other":
            	eraseQuery = ConfigurationManager.getProperty("oai", "oai.erase.query.crisother");
            	if (eraseQuery == null || eraseQuery.trim().length() <= 0) {
            		eraseQuery = "NOT (item.type:item OR item.type:rp OR item.type:project OR item.type:ou)";
            	}
    	        break;
            case "all":
            	eraseQuery = ConfigurationManager.getProperty("oai", "oai.erase.query.all");
            	if (eraseQuery == null || eraseQuery.trim().length() <= 0) {
            		eraseQuery = "*:*";
            	}
            	break;
            default:
            	throw new DSpaceSolrIndexerException("Index is not supported for type " + idxType);
            }
            solrServerResolver.getServer().deleteByQuery(eraseQuery);
            solrServerResolver.getServer().commit();
            System.out.println("Index cleared");
        } catch (SolrServerException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
        }
    }

    private static final String COMMAND_IMPORT = "import";

    public static void main(String[] argv) throws IOException, ConfigurationException {

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(new Class[]{
                BasicConfiguration.class
        });
        Context ctx = null;

        try {
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            options.addOption("c", "clear", false, "Clear index before indexing");
            options.addOption("o", "optimize", false,
                    "Optimize index at the end");
            options.addOption("v", "verbose", false, "Verbose output");
            options.addOption("h", "help", false, "Shows some help");
            options.addOption("n", "number", true, "FOR DEVELOPMENT MUST DELETE");
            options.addOption("t", "type", true, "Type of index (item, rp, project, ou, other, all). The default is 'all'.");
            CommandLine line = parser.parse(options, argv);

            String[] validSolrCommands = {COMMAND_IMPORT};

            boolean run = false;
            if (line.getArgs().length > 0) {
                    if (Arrays.asList(validSolrCommands).contains(line.getArgs()[0])) {
                        run = true;
                    }
            }

            if (!line.hasOption('h') && run) {
                System.out.println("OAI 2.0 manager action started");
                long start = System.currentTimeMillis();

                String command = line.getArgs()[0];

                if (COMMAND_IMPORT.equals(command)) {
                    ctx = new Context();
                    XOAI indexer = new XOAI(ctx,
                            line.hasOption('o'),
                            line.hasOption('c'),
                            line.hasOption('v'));

                    applicationContext.getAutowireCapableBeanFactory().autowireBean(indexer);
                    
                    String idxType = line.getOptionValue("t");
                    if (idxType == null || idxType.trim().length() <= 0)
                    	idxType = "all";
                    indexer.index(idxType);
                }
                System.out.println("OAI 2.0 manager action ended. It took "
                        + ((System.currentTimeMillis() - start) / 1000)
                        + " seconds.");
            } else {
                usage();
            }
        } catch (Throwable ex) {
            if (!searchForReason(ex)) {
                ex.printStackTrace();
            }
            log.error(ex.getMessage(), ex);
        }
        finally
        {
            // Abort our context, if still open
            if(ctx!=null && ctx.isValid())
                ctx.abort();
        }
    }

    private static void usage()
    {
        System.out.println("OAI Manager Script");
        System.out.println("Syntax: oai <action> [parameters]");
        System.out.println("> Possible actions:");
        System.out.println("     " + COMMAND_IMPORT
                + " - To import DSpace items and/or DSpace-CRIS entities into OAI index and cache system");
        System.out.println("> Parameters:");
        System.out.println("     -o Optimize index after indexing ("
                + COMMAND_IMPORT + " only)");
        System.out.println("     -c Clear index (" + COMMAND_IMPORT + " only)");
        System.out.println("     -v Verbose output");
        System.out.println("     -h Shows this text");
    }
}
