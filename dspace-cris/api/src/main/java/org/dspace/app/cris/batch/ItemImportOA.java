/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.AdditionalMetadataUpdateProcessPlugin;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.util.ItemUtils;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.hibernate.Session;

/**
 * Import from the imp_* table in the DSpace data model. Because the script
 * manage the history we suggest to run this script form {@link ItemImportMainOA} 
 * Available operation: -a to build item -r to update
 * (List of metadata to remove first and after do an update [by default all metadata are delete,
 * specifying only the dc.title it will obtain an append on the other metadata]; 
 * use this option many times on the single metadata e.g. -m dc.title -m dc.contributor.*) 
 * -d to remove the item
 * 
 * Status changes: -p to send in workspace -w to send in workspace step one -y to send
 * in workspace step two -x to send in workspace step three -z to send inarchive
 * 
 * 
 * Call the script with the option -h to discover more setting.
 * 
 * <em>For massive import see {@link ItemImportMainOA}</em>
 */
public class ItemImportOA
{

    private DSpace dspace = new DSpace();

    /** logger */
    private static Logger log = Logger.getLogger(ItemImportOA.class);

    private boolean goToWFStepOne = false;

    private boolean goToWFStepTwo = false;

    private boolean goToWFStepThree = false;

    private boolean goToPublishing = false;

    private boolean goToWithdrawn = false;

    private boolean workspace = false;

    private EPerson myEPerson = null;

    private EPerson batchJob = null;

    private String[] metadataClean = null;

    private String sourceRef = null;
    
    public static void main(String[] argv)
    {
        Context context = null;

        try
        {
            context = new Context();
            impRecord(context, argv);
            context.complete();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

    }

    public static UUID impRecord(Context context, String[] argv) throws Exception
    {
        // instantiate loader
        ItemImportOA myLoader = new ItemImportOA();

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("a", "add", false, "add items to DSpace");
        options.addOption("b", "bitstream", false, "clear old bitstream");
        options.addOption("c", "collection", true,
                "destination collection(s) Handle or database ID");
        options.addOption("d", "delete", false, "delete items");
        options.addOption("e", "eperson", true, "id eperson doing importing");
        options.addOption("E", "batch_user", true, "user batch job");
        options.addOption("g", "withdrawn", false,
                "set item in withdrawn state");
        options.addOption("h", "help", false, "help");
        options.addOption("i", "record", true, "record ID");
        options.addOption("I", "importID", true, "import ID");        
        options.addOption("k", "handle", true, "handle of item");
        options.addOption("m", "metadata", true,
                "List of metadata to remove first and after do an update [by default all metadata are delete, specifying only the dc.title it will obtain an append on the other metadata]; use this option many times on the single metadata e.g. -m dc.title -m dc.contributor.*");
        options.addOption("o", "item", true, "item ID");
        options.addOption("p", "workspace", false,
                "send submission back to workspace");
        options.addOption("r", "replace", false, "update items");
        options.addOption("w", "workflow1", false,
                "send submission through collection's workflow step one");
        options.addOption("x", "workflow3", false,
                "send submission through collection's workflow step three");
        options.addOption("y", "workflow2", false,
                "send submission through collection's workflow step two");
        options.addOption("z", "published", false,
                "send submission through item's deposit");
        options.addOption("R", "sourceref", true,
                "name of the source");

        CommandLine line = parser.parse(options, argv);

        String command = null; // add replace remove, etc
        UUID epersonID = null; // db ID
        String[] collections = null; // db ID or handles
        String handle = null;
        String imp_record_id = null;
        UUID item_id = null;
        int imp_id = 0;

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemImport\n", options);
            System.out.println(
                    "adding items: ItemImport -a -e eperson -c collection");
            System.out.println(
                    "replacing items: ItemImport -r -e eperson -c collection");
            System.out.println("deleting items: ItemImport -d -e eperson");
            System.out.println(
                    "If multiple collections are specified, the first collection will be the one that owns the item.");

            System.exit(0);
        }

        if (line.hasOption('a'))
        {
            command = "add";
        }

        if (line.hasOption('r'))
        {
            command = "replace";
        }

        if (line.hasOption('d'))
        {
            command = "delete";
        }

        if (line.hasOption('w'))
        {
            myLoader.setGoToWFStepOne(true);
        }

        if (line.hasOption('y'))
        {
            myLoader.setGoToWFStepTwo(true);
        }

        if (line.hasOption('x'))
        {
            myLoader.setGoToWFStepThree(true);
        }

        if (line.hasOption('g'))
        {
            myLoader.setGoToWithdrawn(true);
        }

        if (line.hasOption('z'))
        {
            myLoader.setGoToPublishing(true);
        }

        if (line.hasOption('p'))
        {
            myLoader.setWorkspace(true);
        }

        if (line.hasOption('e')) // eperson
        {
            epersonID = UUID.fromString(line.getOptionValue('e').trim());
        }

        if (line.hasOption('c')) // collections
        {
            collections = line.getOptionValues('c');
        }

        if (line.hasOption('i')) // record ID
        {
            imp_record_id = line.getOptionValue('i').trim();
        }
        if (line.hasOption('o')) // item ID (replace or delete)
        {
            item_id = UUID.fromString(line.getOptionValue('o').trim());
        }
        if (line.hasOption('I')) // item ID (replace or delete)
        {
            imp_id = Integer.parseInt(line.getOptionValue('I').trim());
        }
        if (line.hasOption('E'))
        {
            String batchjob = line.getOptionValue('E').trim();
            EPerson tempBatchJob = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, batchjob);
            myLoader.setBatchJob(tempBatchJob);
            if (tempBatchJob == null)
            {
                throw new RuntimeException("User batch job not found");
            }
        }

        if (line.hasOption('m'))
        {
            myLoader.metadataClean = line.getOptionValues('m');
        }
        if (line.hasOption('k'))
        {
            handle = line.getOptionValue('k').trim();
        }
        boolean clearOldBitstream = false;
        if (line.hasOption('b'))
        {
            clearOldBitstream = true;
        }

        if (line.hasOption('R'))
        {
            myLoader.setSourceRef(line.getOptionValue('R'));
        }
        
        // now validate
        // must have a command set
        EPerson currUser = myLoader.batchJob;
        EPerson tempMyEPerson = null;

        if (command == null)
        {
            System.out.println(
                    "Error - must run with either add, replace, or remove (run with -h flag for details)");
            System.exit(1);
        }
        else if (command.equals("add") || command.equals("replace"))
        {
            if (epersonID == null)
            {
                System.out.println(
                        "Error - an eperson to do the importing must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            tempMyEPerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, epersonID);
            if (command.equals("add"))
            {
                currUser = tempMyEPerson;
            }
            if (collections == null)
            {
                System.out.println(
                        "Error - at least one destination collection must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
        }
        else if (command.equals("delete"))
        {
            tempMyEPerson = myLoader.batchJob;
        }

        myLoader.setMyEPerson(currUser);

        if (tempMyEPerson == null)
        {
            System.out.println("Error, eperson cannot be found: " + epersonID);
            throw new RuntimeException(
                    "Error, eperson cannot be found: " + epersonID);
        }

        context.setCurrentUser(tempMyEPerson);

        // find collections
        Collection[] mycollections = null;

        // don't need to validate collections set if command is "delete"
        if (!command.equals("delete"))
        {
            System.out.println("Destination collections:");

            mycollections = new Collection[collections.length];

            // validate each collection arg to see if it's a real collection
            for (int i = 0; i < collections.length; i++)
            {
                // is the ID a handle?
                if (collections[i].indexOf('/') != -1)
                {
                    // string has a / so it must be a handle - try and resolve
                    // it
                    mycollections[i] = (Collection) HandleServiceFactory.getInstance().getHandleService()
                            .resolveToObject(context, collections[i]);

                    // resolved, now make sure it's a collection
                    if ((mycollections[i] == null) || (mycollections[i]
                            .getType() != Constants.COLLECTION))
                    {
                        mycollections[i] = null;
                    }
                }
                // not a handle, try and treat it as an integer collection
                // database ID
                else if (collections[i] != null)
                {
                    mycollections[i] = ContentServiceFactory.getInstance().getCollectionService().find(context,
                            UUID.fromString(collections[i].trim()));
                }

                // was the collection valid?
                if (mycollections[i] == null)
                {
                    throw new IllegalArgumentException("Cannot resolve "
                            + collections[i] + " to collection");
                }

                // print progress info
                String owningPrefix = "";

                if (i == 0)
                {
                    owningPrefix = "Owning ";
                }

                System.out.println(owningPrefix + " Collection: "
                        + mycollections[i].getMetadata("name"));
            }
        } // end of validating collections

        try
        {
            context.turnOffAuthorisationSystem();
            if (command.equals("add"))
            {
                item_id = myLoader.addItem(context, mycollections, imp_id,
                        handle, clearOldBitstream);

                if(StringUtils.isNotBlank(myLoader.getSourceRef())) {
                    getHibernateSession(context).createSQLQuery(
                        "INSERT INTO imp_record_to_item " + "VALUES ( ?, ?, ?)").setParameter(0,
   		        imp_record_id).setParameter(1,myLoader.getSourceRef() ).setParameter(2, item_id).executeUpdate();	
                }
                else {
                    getHibernateSession(context).createSQLQuery(
			    "INSERT INTO imp_record_to_item " + "VALUES ( ?, null,?)").setParameter(0,
                            imp_record_id).setParameter(1, item_id).executeUpdate();                	
                }
            }
            else if (command.equals("replace"))
            {
                myLoader.replaceItems(context, mycollections, imp_record_id,
                        item_id, imp_id, clearOldBitstream);
            }
            else if (command.equals("delete")
                    || command.equals("deleteintegra"))
            {
                Item item = ContentServiceFactory.getInstance().getItemService().find(context, item_id);
                if (item != null)
                {
                    ItemUtils.removeOrWithdrawn(context, item);
                }
                if (command.equals("delete")
                        && (item == null || !item.isWithdrawn()))
                {
                	getHibernateSession(context).createSQLQuery(
                            "DELETE FROM imp_record_to_item "
                                    + "WHERE imp_record_id = ? AND imp_item_id = ?").setParameter(0,
                            imp_record_id).setParameter(1, item_id).executeUpdate();
                }
            }

            getHibernateSession(context).createSQLQuery(
                    "UPDATE imp_record " + "SET last_modified = LOCALTIMESTAMP"
                            + " WHERE imp_id = ?").setParameter(0,
	    	     imp_id).executeUpdate();
            context.restoreAuthSystemState();
            return item_id;
        }
        catch (RuntimeException e)
        {
            log.warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void replaceItems(Context c, Collection[] mycollections,
            String imp_record_id, UUID item_id, int imp_id,
            boolean clearOldBitstream) throws Exception
    {

        Item oldItem = ContentServiceFactory.getInstance().getItemService().find(c, item_id);

        // check item
        if (oldItem == null)
        {
            throw new RuntimeException("No item found with id: " + item_id);
        }

        processItemUpdate(c, imp_id, clearOldBitstream, oldItem);
    }

    private void processItemUpdate(Context c, int imp_id,
            boolean clearOldBitstream, Item item) throws SQLException,
                    AuthorizeException, TransformerException, IOException, WorkflowException
    {

        UUID item_id = item.getID();
        if (metadataClean != null && metadataClean.length > 0)
        {
            for (String mc : metadataClean)
            {
                StringTokenizer dcf = new StringTokenizer(mc.trim(), ".");

                String[] tokens = { "", "", "" };
                int i = 0;
                while (dcf.hasMoreTokens())
                {
                    tokens[i] = dcf.nextToken().trim();
                    i++;
                }
                String schema = tokens[0];
                String element = tokens[1];
                String qualifier = tokens[2];

                if ("*".equals(qualifier))
                {
                    item.getItemService().clearMetadata(c, item, schema, element, Item.ANY, Item.ANY);
                }
                else if ("".equals(qualifier))
                {
                	item.getItemService().clearMetadata(c, item, schema, element, null, Item.ANY);
                }
                else
                {
                	item.getItemService().clearMetadata(c, item, schema, element, qualifier, Item.ANY);
                }
            }
        }
        else
        {
        	item.getItemService().clearMetadata(c, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        }

        // now fill out dublin core for item
        loadDublinCore(c, item, imp_id);
        // and the bitstreams
        processImportBitstream(c, item, imp_id, clearOldBitstream);
        
        List<AdditionalMetadataUpdateProcessPlugin> additionalMetadataUpdateProcessPlugins = (List<AdditionalMetadataUpdateProcessPlugin>) dspace
                .getServiceManager().getServicesByType(AdditionalMetadataUpdateProcessPlugin.class);
        for(AdditionalMetadataUpdateProcessPlugin additionalMetadataUpdateProcessPlugin : additionalMetadataUpdateProcessPlugins) {
            additionalMetadataUpdateProcessPlugin.process(c, item, getSourceRef());
        }
        
        item.getItemService().update(c, item);

        if (goToWithdrawn)
        {
            if (item.isArchived())
            {
                ItemUtils.removeOrWithdrawn(c, item);
            }
            else
            {
                throw new RuntimeException("Item corresponding imp_id=" + imp_id
                        + " is not in archive");
            }
        }
        else
        {
            if (goToPublishing)
            {
                if (item.isWithdrawn())
                {
                    item.getItemService().reinstate(c, item);
                }
            }
            // check if item is in workspace status
            WorkspaceItem trWsi = ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(c, item);
            
            BasicWorkflowService WorkflowManager = (BasicWorkflowService)WorkflowServiceFactory.getInstance().getWorkflowService();
            BasicWorkflowItemService basicWorkflowItemService = (BasicWorkflowItemService)WorkflowServiceFactory.getInstance().getWorkflowItemService();
            if (trWsi != null)
            {

                if (goToWFStepOne || goToWFStepTwo || goToWFStepThree
                        || goToPublishing)
                {
                	BasicWorkflowItem wfi = WorkflowManager.startWithoutNotify(c,
                            trWsi);
                    if ((wfi != null
                            && wfi.getState() == BasicWorkflowService.WFSTATE_STEP1POOL)
                            && (goToWFStepTwo || goToWFStepThree
                                    || goToPublishing))
                    {

                        WorkflowManager.claim(c, wfi, batchJob);
                        WorkflowManager.advance(c, wfi, batchJob);
                    }
                    if ((wfi != null
                            && wfi.getState() == BasicWorkflowService.WFSTATE_STEP2POOL)
                            && (goToWFStepThree || goToPublishing))
                    {
                        WorkflowManager.claim(c, wfi, batchJob);
                        WorkflowManager.advance(c, wfi, batchJob);
                    }
                    if ((wfi != null
                            && wfi.getState() == BasicWorkflowService.WFSTATE_STEP3POOL)
                            && goToPublishing)
                    {
                        WorkflowManager.claim(c, wfi, batchJob);
                        WorkflowManager.advance(c, wfi, batchJob);
                    }
                }
            }
            else if (workspace || goToWFStepOne || goToWFStepTwo
                    || goToWFStepThree || goToPublishing)
            {

                // check if item is in workflow status
                BasicWorkflowItem wfi = basicWorkflowItemService.findByItem(c, item);                
                if (wfi != null)
                {
                    if (workspace)
                    {
                        WorkflowManager.abort(c, wfi, batchJob);
                    }
                    else
                    {
                        int state = wfi.getState();
                        if (state == BasicWorkflowService.WFSTATE_STEP1POOL
                                && !goToWFStepOne)
                        {
                            WorkflowManager.claim(c, wfi, batchJob);
                            WorkflowManager.advance(c, wfi, batchJob);
                            if ((wfi != null
                                    && wfi.getState() == BasicWorkflowService.WFSTATE_STEP2POOL)
                                    && (goToWFStepThree || goToPublishing))
                            {
                                WorkflowManager.claim(c, wfi, batchJob);
                                WorkflowManager.advance(c, wfi, batchJob);
                            }
                            if ((wfi != null
                                    && wfi.getState() == BasicWorkflowService.WFSTATE_STEP3POOL)
                                    && (goToPublishing))
                            {
                                WorkflowManager.claim(c, wfi, batchJob);
                                WorkflowManager.advance(c, wfi, batchJob);
                            }
                        }
                        else if (state == BasicWorkflowService.WFSTATE_STEP1)
                        {
                            if (goToWFStepOne)
                            {
                                WorkflowManager.unclaim(c, wfi, wfi.getOwner());
                            }
                            else
                            {
                                WorkflowManager.advance(c, wfi, batchJob);
                                if ((wfi != null
                                        && wfi.getState() == BasicWorkflowService.WFSTATE_STEP2POOL)
                                        && (goToWFStepThree || goToPublishing))
                                {
                                    WorkflowManager.claim(c, wfi, batchJob);
                                    WorkflowManager.advance(c, wfi, batchJob);
                                }
                                if ((wfi != null
                                        && wfi.getState() == BasicWorkflowService.WFSTATE_STEP3POOL)
                                        && (goToPublishing))
                                {
                                    WorkflowManager.claim(c, wfi, batchJob);
                                    WorkflowManager.advance(c, wfi, batchJob);
                                }
                            }
                        }
                        else if (state == BasicWorkflowService.WFSTATE_STEP2POOL
                                && !goToWFStepTwo)
                        {
                            WorkflowManager.claim(c, wfi, batchJob);
                            WorkflowManager.advance(c, wfi, batchJob);

                            if ((wfi != null
                                    && wfi.getState() == BasicWorkflowService.WFSTATE_STEP3POOL)
                                    && (goToPublishing))
                            {
                                WorkflowManager.claim(c, wfi, batchJob);
                                WorkflowManager.advance(c, wfi, batchJob);
                            }
                            // anomaly control
                            if (goToWFStepOne)
                            {
                                throw new RuntimeException("Error: Item "
                                        + item_id + " in status "
                                        + BasicWorkflowService.WFSTATE_STEP2POOL
                                        + " no turn back in status "
                                        + BasicWorkflowService.WFSTATE_STEP1);
                            }
                        }
                        else if (state == BasicWorkflowService.WFSTATE_STEP2)
                        {
                            if (goToWFStepTwo)
                            {
                                WorkflowManager.unclaim(c, wfi, wfi.getOwner());
                            }
                            else
                            {
                                WorkflowManager.advance(c, wfi, batchJob);
                                if ((wfi != null
                                        && wfi.getState() == BasicWorkflowService.WFSTATE_STEP3POOL)
                                        && (goToPublishing))
                                {
                                    WorkflowManager.claim(c, wfi, batchJob);
                                    WorkflowManager.advance(c, wfi, batchJob);
                                }
                                // anomaly control
                                if (goToWFStepOne)
                                {
                                    throw new RuntimeException("Error: Item "
                                            + item_id + " in status "
                                            + BasicWorkflowService.WFSTATE_STEP2POOL
                                            + " no turn back in status "
                                            + BasicWorkflowService.WFSTATE_STEP1);
                                }
                            }
                        }
                        else if (state == BasicWorkflowService.WFSTATE_STEP3POOL
                                && !goToWFStepThree)
                        {
                            WorkflowManager.claim(c, wfi, batchJob);
                            WorkflowManager.advance(c, wfi, batchJob);

                            // anomaly control
                            if (goToWFStepOne || goToWFStepTwo)
                            {
                                throw new RuntimeException("Error: Item "
                                        + item_id + " in status "
                                        + BasicWorkflowService.WFSTATE_STEP3POOL
                                        + " no turn back in status "
                                        + (goToWFStepOne
                                                ? BasicWorkflowService.WFSTATE_STEP1
                                                : BasicWorkflowService.WFSTATE_STEP2));
                            }
                        }
                        else if (state == BasicWorkflowService.WFSTATE_STEP3)
                        {
                            if (goToWFStepThree)
                            {
                                WorkflowManager.unclaim(c, wfi, wfi.getOwner());
                            }
                            else
                            {
                                WorkflowManager.advance(c, wfi, batchJob);
                                // anomaly control
                                if (goToWFStepOne || goToWFStepTwo)
                                {
                                    throw new RuntimeException("Error: Item "
                                            + item_id + " in status "
                                            + BasicWorkflowService.WFSTATE_STEP3POOL
                                            + " no turn back in status "
                                            + (goToWFStepOne
                                                    ? BasicWorkflowService.WFSTATE_STEP1
                                                    : BasicWorkflowService.WFSTATE_STEP2));
                                }
                            }
                        }
                    }
                }
                else
                {
                    // then item is in publish state
                    if (!goToPublishing)
                    {
                        throw new RuntimeException(
                                "Error: Item " + item_id + " in status "
                                        + BasicWorkflowService.WFSTATE_ARCHIVE
                                        + " no turn back.");
                    }
                    else
                    {
                        item.getItemService().update(c, item);
                    }
                }
            }

            // UPdate visibility
        }
    }

    /**
     * item? try and add it to the archive c mycollection path itemname handle -
     * non-null means we have a pre-defined handle already mapOut - mapfile
     * we're writing
     */
    private UUID addItem(Context c, Collection[] mycollections, int imp_id,
            String handle, boolean clearOldBitstream) throws Exception
    {

    	BasicWorkflowService WorkflowManager = (BasicWorkflowService)WorkflowServiceFactory.getInstance().getWorkflowService();
    	
        if (goToWithdrawn)
        {
            throw new RuntimeException("Item corresponding imp_id=" + imp_id
                    + " is not in archive");
        }

        // create workspace item
        Item myitem = null;
        WorkspaceItem wi = null;
        c.setCurrentUser(myEPerson);

        wi = ContentServiceFactory.getInstance().getWorkspaceItemService().create(c, mycollections[0], false);
        myitem = wi.getItem();

        if (StringUtils.isNotEmpty(handle))
        {
            // se ti arriva allora chiami il service che ti registra l'handle
        	IdentifierService identifierService  = IdentifierServiceFactory.getInstance().getIdentifierService();
            identifierService.register(c, myitem, handle);
        }

        // now fill out dublin core for item
        loadDublinCore(c, myitem, imp_id);
        // and the bitstreams
        processImportBitstream(c, myitem, imp_id, clearOldBitstream);
        
        List<AdditionalMetadataUpdateProcessPlugin> additionalMetadataUpdateProcessPlugins = (List<AdditionalMetadataUpdateProcessPlugin>) dspace
                .getServiceManager().getServicesByType(AdditionalMetadataUpdateProcessPlugin.class);
        for(AdditionalMetadataUpdateProcessPlugin additionalMetadataUpdateProcessPlugin : additionalMetadataUpdateProcessPlugins) {
            additionalMetadataUpdateProcessPlugin.process(c, myitem, getSourceRef());
        }
        
        wi.setMultipleFiles(true);
        wi.setMultipleTitles(true);
        wi.setPublishedBefore(true);
        wi.setStageReached(1);
        
        ContentServiceFactory.getInstance().getWorkspaceItemService().update(c, wi);
        

        if (goToWFStepOne || goToWFStepTwo || goToWFStepThree)
        {
            BasicWorkflowItem wfi = WorkflowManager.startWithoutNotify(c, wi);

            int status = wfi.getState();

            if (status == WorkflowManager.WFSTATE_STEP1POOL
                    && (goToWFStepTwo || goToWFStepThree))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
                status = wfi.getState();
            }

            if (status == WorkflowManager.WFSTATE_STEP2POOL && goToWFStepThree)
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }
        }
        else if (goToPublishing)
        {
        	BasicWorkflowItem wfi = WorkflowManager.startWithoutNotify(c, wi);

            if ((wfi != null
                    && wfi.getState() == WorkflowManager.WFSTATE_STEP1POOL))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }
            if ((wfi != null
                    && wfi.getState() == WorkflowManager.WFSTATE_STEP2POOL))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }
            if ((wfi != null
                    && wfi.getState() == WorkflowManager.WFSTATE_STEP3POOL))
            {
                WorkflowManager.claim(c, wfi, batchJob);
                WorkflowManager.advance(c, wfi, batchJob);
            }

            // Non necessaria perche la registrazione viene effettuata al
            // termine del wkf
            // only process handle file if not using workflow system// only
            // process handle file if not using workflow system
            // InstallItem.installItem(c, wi, null);
            // InstallItem.installItem(c, wfi, handle, false);
        }

        // now add to multiple collections if requested
        if (mycollections.length > 1)
        {
            for (int i = 1; i < mycollections.length; i++)
            {
                mycollections[i].getCollectionService().addItem(c, mycollections[i], myitem);
            }
        }

        return myitem.getID();
    }

    private void loadDublinCore(Context c, Item myitem, int imp_id)
            throws SQLException, AuthorizeException, TransformerException
    {

        String myQuery = "SELECT imp_value, imp_element, imp_qualifier, imp_authority, imp_confidence, TEXT_LANG, IMP_SHARE  FROM imp_metadatavalue WHERE imp_id = ? ORDER BY imp_metadatavalue_id, imp_element, imp_qualifier, metadata_order";

        List<Object[]> retTRI = getHibernateSession(c).createSQLQuery(myQuery).setParameter(0, imp_id).list();

        // Add each one as a new format to the registry
        for(Object[] row_data : retTRI)
        {
            addDCValue(c, myitem, "dc", row_data);
        }
    }

    /**
     * Get the information from the TableRow for the specific metadata.
     * The authority column can use the special value [GUESS], case insensitive, 
     * to rely on the getBestMatch to guess a potential authority
     * 
     * @param c
     * @param i
     * @param schema
     * @param n
     * @throws TransformerException
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void addDCValue(Context c, Item i, String schema, Object[] n)
            throws TransformerException, SQLException, AuthorizeException
    {
        String value = (String)n[0];//.getStringColumn("imp_value");
        // compensate for empty value getting read as "null", which won't
        // display
        if (value == null)
            value = "";

        String element = (String)n[1];//.getStringColumn("imp_element");
        String qualifier = (String)n[2];//.getStringColumn("imp_qualifier");
        String authority = (String)n[3];//.getStringColumn("imp_authority");
        int confidence = (Integer)n[4];//.getIntColumn("imp_confidence");
        String language = "";

        language = (String)n[5];//.getStringColumn("TEXT_LANG");

        System.out.println("\tSchema: " + schema + " Element: " + element
                + " Qualifier: " + qualifier + " Value: " + value);

        if (qualifier == null || qualifier.equals("none")
                || "".equals(qualifier))
        {
            qualifier = null;
        }

        // if language isn't set, use the system's default value
        if (language == null || language.equals(""))
        {
            language = ConfigurationManager.getProperty("default.language");
        }

        // a goofy default, but there it is
        if (language == null)
        {
            language = "en";
        }

        // let's check that the actual metadata field exists.
        MetadataSchema foundSchema = ContentServiceFactory.getInstance().getMetadataSchemaService().find(c, schema);

        if (foundSchema == null)
        {
            System.out.println("ERROR: schema '" + schema
                    + "' was not found in the registry.");
            return;
        }

        
        MetadataField foundField = ContentServiceFactory.getInstance().getMetadataFieldService().findByElement(c, foundSchema,
                element, qualifier);

        if (foundField == null)
        {
            System.out.println(
                    "ERROR: Metadata field: '" + schema + "." + element + "."
                            + qualifier + "' was not found in the registry.");
            return;
        }

        boolean bShare = ConfigurationManager
                .getBooleanProperty("sharepriority." + schema + "." + element
                        + "." + qualifier + ".share");
        int share = -1;
        if (bShare)
        {
            share = (Integer)n[6];//.getIntColumn("IMP_SHARE");
        }

        if (authority != null && authority.equalsIgnoreCase("[GUESS]"))
        {
            // remove placeholder
            authority = null;
            
            if (bShare)
            {
                //TODO not yet implemented
                // i.addMetadata(schema, element, qualifier, language, value,
                // authority, confidence, share, -1);
            }
            else
            {
                i.getItemService().addMetadata(c, i, schema, element, qualifier, language, value);
            }
        }
        else
        {
            if (bShare)
            {
                //TODO not yet implemented
                // i.addMetadata(schema, element, qualifier, language, value,
                // authority, confidence, share, -1);
            }
            else
            {
            	i.getItemService().addMetadata(c, i, schema, element, qualifier, language, value,
                        authority, confidence);
            }
        }
    }

    /**
     * Import a bitstream to relative item.
     * 
     * @param c
     * @param i
     * @param imp_id
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     */
    private void processImportBitstream(Context c, Item i, int imp_id,
            boolean clearOldBitstream)
                    throws SQLException, IOException, AuthorizeException
    {

        if (clearOldBitstream)
        {
            List<Bundle> bnds = i.getBundles();
            if (bnds != null)
            {
                for (Bundle bundle : bnds)
                {
                    List<Bitstream> bts = bundle.getBitstreams();
                    if (bts != null)
                    {
                        for (Bitstream b : bts)
                        {
                            bundle.getBundleService().removeBitstream(c, bundle, b);
                            bundle.getBundleService().update(c, bundle);
                        }
                    }
                    i.getItemService().removeBundle(c, i, bundle);
                }
            }
        }
        // retrieve the attached
        String sql_bs = "SELECT imp_bitstream_id, filepath, bundle, description, primary_bitstream, assetstore, name, embargo_policy, embargo_start_date, md5value, imp_blob FROM imp_bitstream WHERE imp_id = ? order by bitstream_order asc";

        List<Object[]> rows_bs_all = getHibernateSession(c).createSQLQuery(sql_bs).setParameter(0,imp_id).list();

        for (Object[] imp_bitstream : rows_bs_all)
        {

            String filepath = (String)imp_bitstream[1];//.getStringColumn("filepath");
            String bundleName = (String)imp_bitstream[2];//.getStringColumn("bundle");
            String description = (String)imp_bitstream[3];//.getStringColumn("description");
            Boolean primary_bitstream = (Boolean)imp_bitstream[4];//.getBooleanColumn("primary_bitstream");

            int assetstore = (Integer)imp_bitstream[5];//.getIntColumn("assetstore");
            System.out.println("\tProcessing contents file: " + filepath);

            String name_file = (String)imp_bitstream[6];//.getStringColumn("name");

            String start_date = "";

            // 0: all
            // 1: embargo
            // 2: only an authorized group
            // 3: not visible
            int embargo_policy = (Integer)imp_bitstream[7];//.getIntColumn("embargo_policy");
            String embargo_start_date = (String)imp_bitstream[8];//.getStringColumn("embargo_start_date");
            Group embargoGroup = null;
            if (embargo_policy != -1)
            {
                if (embargo_policy == 3)
                {
                    start_date = null;
                    embargoGroup = EPersonServiceFactory.getInstance().getGroupService().findByName(c, Group.ADMIN);
                }
                else if (embargo_policy == 2)
                {
                    embargoGroup = EPersonServiceFactory.getInstance().getGroupService().findByName(c, Group.EMBARGO);
                    if (embargo_start_date != null)
                    {
                        start_date = embargo_start_date;
                    }
                    else
                    {
                        start_date = null;
                    }
                }
                else if (embargo_policy == 1 && embargo_start_date != null)
                {
                    start_date = embargo_start_date;
                }
                else if (embargo_policy == 0)
                {
                    start_date = null;
                }
            }

            String valueMD5 = (String)imp_bitstream[9];//.getStringColumn("md5value");
            byte[] content = imp_bitstream[10]!= null ? imp_bitstream[10].toString().getBytes() : null;//.getBinaryData("imp_blob");
            Bitstream bs = processBitstreamEntry(c, i, filepath, bundleName,
                    description, primary_bitstream, name_file,
                    assetstore, embargoGroup, start_date, content,
                    valueMD5);
            // HACK: replace the bytea with a register like operation
            if (content != null)
            {
            	String sql = "UPDATE imp_bitstream SET imp_blob = null, assetstore = ?, filepath = ? WHERE imp_bitstream_id = ?";
                
                String assetstorePath;
                if (bs.getStoreNumber() == 0)
                {
                    assetstorePath = ConfigurationManager
                            .getProperty("assetstore.dir") + File.separatorChar;
                }
                else
                {
                    assetstorePath = ConfigurationManager.getProperty(
                            "assetstore.dir." + bs.getStoreNumber())
                            + File.separatorChar;
                }
                int length = assetstorePath.length();
                
                getHibernateSession(c).createSQLQuery(sql).setParameter(0, bs.getStoreNumber()).setParameter(1, bs.getSource().substring(length)).setParameter(2, imp_bitstream[0]).executeUpdate();
            }

        }
    }

    /**
     * Process bitstream
     * 
     * @param c
     * @param i
     * @param bitstreamPath
     * @param bundleName
     * @param description
     * @param license
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    private Bitstream processBitstreamEntry(Context c, Item i,
            String bitstreamPath, String bundleName,
            String description, Boolean primaryBitstream, String name_file,
            int alreadyInAssetstoreNr, Group embargoGroup, String start_date,
            byte[] content, String valueMD5)
                    throws SQLException, IOException, AuthorizeException
    {
        String fullpath = null;

        if (alreadyInAssetstoreNr == -1)
        {
            fullpath = bitstreamPath;
        }
        else
        {
            fullpath = ConfigurationManager
                    .getProperty("assetstore.dir." + alreadyInAssetstoreNr)
                    + File.separatorChar + bitstreamPath;
        }

        Bitstream bs = null;
        String newBundleName = bundleName;

        if (bundleName == null || bundleName.length() == 0)
        {
            // is it license.txt?
            if (bitstreamPath.endsWith("license.txt"))
            {
                newBundleName = "LICENSE";
            }
            else
            {
                // call it ORIGINAL
                newBundleName = "ORIGINAL";
            }
        }

        // find the bundle
        List<Bundle> bundles = i.getItemService().getBundles(i, newBundleName);
        Bundle targetBundle = null;

        if (bundles.size() < 1)
        {
            // not found, create a new one
            targetBundle = ContentServiceFactory.getInstance().getBundleService().create(c, i, newBundleName);
        }
        else
        {
            // put bitstreams into first bundle
            targetBundle = bundles.get(0);
        }

        // get an input stream
        if (alreadyInAssetstoreNr == -1)
        {
            InputStream bis;
            if (content != null)
            {
                bis = new ByteArrayInputStream(content);
            }
            else
            {
                bis = new BufferedInputStream(new FileInputStream(fullpath));
            }

            // now add the bitstream
            bs = ContentServiceFactory.getInstance().getBitstreamService().create(c, targetBundle, bis);
        }
        else
        {
            bs = ContentServiceFactory.getInstance().getBitstreamService().register(c, targetBundle, alreadyInAssetstoreNr,
                    bitstreamPath);
            if (valueMD5 != null)
            {
                bs.setMD5Value(c, valueMD5);
            }
        }

        bs.setDescription(c, description);
        if (primaryBitstream)
        {
            targetBundle.setPrimaryBitstreamID(bs);
        }
        if (name_file != null)
            bs.setName(c, name_file);
        else
            bs.setName(c, new File(fullpath).getName());

        // Identify the format
        // FIXME - guessing format guesses license.txt incorrectly as a text
        // file format!
        BitstreamFormat bf = ContentServiceFactory.getInstance().getBitstreamFormatService().guessFormat(c, bs);
        bs.setFormat(c, bf);
        if (bitstreamPath != null)
        {
            bs.setSource(c, bitstreamPath);
        }
        else
        {
            bs.setSource(c, StorageServiceFactory.getInstance().getBitstreamStorageService().absolutePath(c, bs));
        }

        if (embargoGroup == null) {
            embargoGroup = EPersonServiceFactory.getInstance().getGroupService().findByName(c, Group.ANONYMOUS);
        }
        Date embargoDate = null;
        if (StringUtils.isNotBlank(start_date)) {
            String[] split_date = start_date.split("/");
            int embargo_year = Integer.parseInt(split_date[2]);
            int embargo_month = Integer.parseInt(split_date[1]);
            int embargo_day = Integer.parseInt(split_date[0]);
            if (embargo_year > 0 && embargo_month > 0 && embargo_day > 0) {
                Calendar cal = Calendar.getInstance();
                embargo_month--;
                cal.set(embargo_year, embargo_month, embargo_day, 0, 0, 0);
                embargoDate = cal.getTime();
            }
        }
        AuthorizeServiceFactory.getInstance().getAuthorizeService().removeAllPoliciesByDSOAndType(c, bs,
                ResourcePolicy.TYPE_CUSTOM);
        AuthorizeServiceFactory.getInstance().getAuthorizeService().removeAllPoliciesByDSOAndType(c, bs,
                ResourcePolicy.TYPE_INHERITED);
        ResourcePolicy rp = AuthorizeServiceFactory.getInstance().getResourcePolicyService().create(c);
        rp.setdSpaceObject(bs);
        rp.setAction(Constants.READ);
        rp.setRpType(ResourcePolicy.TYPE_CUSTOM);
        rp.setGroup(embargoGroup);
        rp.setStartDate(embargoDate);
        AuthorizeServiceFactory.getInstance().getResourcePolicyService().update(c, rp);        
        ContentServiceFactory.getInstance().getBitstreamService().update(c, bs);
        return bs;
    }

    public void setGoToWFStepOne(boolean goToWFStepOne)
    {
        this.goToWFStepOne = goToWFStepOne;
    }

    public void setGoToWFStepTwo(boolean goToWFStepTwo)
    {
        this.goToWFStepTwo = goToWFStepTwo;
    }

    public void setGoToWFStepThree(boolean goToWFStepThree)
    {
        this.goToWFStepThree = goToWFStepThree;
    }

    public void setGoToPublishing(boolean goToPublishing)
    {
        this.goToPublishing = goToPublishing;
    }

    public void setGoToWithdrawn(boolean goToWithdrawn)
    {
        this.goToWithdrawn = goToWithdrawn;
    }

    public void setWorkspace(boolean workspace)
    {
        this.workspace = workspace;
    }

    public void setMyEPerson(EPerson myEPerson)
    {
        this.myEPerson = myEPerson;
    }

    public void setBatchJob(EPerson batchJob)
    {
        this.batchJob = batchJob;
    }

    public String getSourceRef()
    {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef)
    {
        this.sourceRef = sourceRef.trim();
    }

    protected static Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
}
