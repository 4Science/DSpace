/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.log4j.Logger;
import org.dspace.checker.BitstreamInfoDAO;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.springframework.beans.factory.InitializingBean;

public class BitstreamStorageServiceImpl implements BitstreamStorageService, InitializingBean
{
    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageServiceImpl.class);

    /** asset stores */
    private Map<Integer, BitStoreService> stores = new HashMap<Integer, BitStoreService>();

    /** The index of the asset store to use for new bitstreams */
    private int incoming;

    @Override
    public void afterPropertiesSet() throws Exception {
        for(Map.Entry<Integer, BitStoreService> storeEntry : stores.entrySet()) {
            storeEntry.getValue().init();
        }
    }

    public int getIncoming() {
        return incoming;
    }

    public void setIncoming(int incoming) {
        this.incoming = incoming;
    }

    public Map<Integer, BitStoreService> getStores() {
        return stores;
    }

    public void setStores(Map<Integer, BitStoreService> stores) {
        this.stores = stores;
    }

    /**
     * Store a stream of bits.
     * 
     * <p>
     * If this method returns successfully, the bits have been stored, and RDBMS
     * metadata entries are in place (the context still needs to be completed to
     * finalize the transaction).
     * </p>
     * 
     * <p>
     * If this method returns successfully and the context is aborted, then the
     * bits will be stored in the asset store and the RDBMS metadata entries
     * will exist, but with the deleted flag set.
     * </p>
     * 
     * If this method throws an exception, then any of the following may be
     * true:
     * 
     * <ul>
     * <li>Neither bits nor RDBMS metadata entries have been stored.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * but no bits.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * and some or all of the bits have also been stored.
     * </ul>
     * 
     * @param context
     *            The current context
     * @param is
     *            The stream of bits to store
     * @exception IOException
     *                If a problem occurs while storing the bits
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     * 
     * @return The ID of the stored bitstream
     */
    public int store(Context context, InputStream is)
            throws SQLException, IOException
    {
        // Create internal ID
        String id = Utils.generateKey();

        // Create a deleted bitstream row, using a separate DB connection
        TableRow bitstream;
        Context tempContext = null;

        try
        {
            tempContext = new Context();

            bitstream = DatabaseManager.row("Bitstream");
            bitstream.setColumn("deleted", true);
            bitstream.setColumn("internal_id", id);

            /*
             * Set the store number of the new bitstream If you want to use some
             * other method of working out where to put a new bitstream, here's
             * where it should go
             */
            bitstream.setColumn("store_number", incoming);

            DatabaseManager.insert(tempContext, bitstream);

            tempContext.complete();
        }
        catch (SQLException sqle)
        {
            if (tempContext != null)
            {
                tempContext.abort();
            }

            throw sqle;
        }

        // For efficiencies sake, PUT is responsible for setting bitstream size_bytes, checksum, and checksum_algorithm
        stores.get(incoming).put(bitstream, is);

        bitstream.setColumn("deleted", false);
        DatabaseManager.update(context, bitstream);

        int bitstreamId = bitstream.getIntColumn("bitstream_id");

        if (log.isDebugEnabled())
        {
            log.debug("Stored bitstream " + bitstreamId);
        }

        return bitstreamId;
    }

	/**
	 * Register a bitstream already in storage.
	 *
	 * @param context
	 *            The current context
	 * @param assetstore The assetstore number for the bitstream to be
	 * 			registered
	 * @param bitstreamPath The relative path of the bitstream to be registered.
	 * 		The path is relative to the path of ths assetstore.
	 * @return The ID of the registered bitstream
	 * @exception SQLException
	 *                If a problem occurs accessing the RDBMS
	 * @throws IOException
	 */
	public int register(Context context, int assetstore,
				String bitstreamPath, boolean computeMD5) throws SQLException, IOException {

		// mark this bitstream as a registered bitstream
		String sInternalId = BitstreamStorageManager.REGISTERED_FLAG + bitstreamPath;

		// Create a deleted bitstream row, using a separate DB connection
		TableRow bitstream;
		Context tempContext = null;

		try {
			tempContext = new Context();

			bitstream = DatabaseManager.row("Bitstream");
			bitstream.setColumn("deleted", true);
			bitstream.setColumn("internal_id", sInternalId);
			bitstream.setColumn("store_number", assetstore);
			DatabaseManager.insert(tempContext, bitstream);

			tempContext.complete();
		} catch (SQLException sqle) {
			if (tempContext != null) {
				tempContext.abort();
			}
			throw sqle;
		}

		Map wantedMetadata = new HashMap();
        wantedMetadata.put("size_bytes", null);
        if (computeMD5) {
            wantedMetadata.put("checksum", null);
            wantedMetadata.put("checksum_algorithm", null);
        }

        Map receivedMetadata = stores.get(assetstore).about(bitstream, wantedMetadata);
        if(MapUtils.isEmpty(receivedMetadata)) {
            String message = "Not able to register bitstream:" + bitstream.getStringColumn("internal_id") + " at path: " + bitstreamPath;
            log.error(message);
            throw new IOException(message);
        } else {
            if(receivedMetadata.containsKey("checksum_algorithm")) {
                bitstream.setColumn("checksum_algorithm", receivedMetadata.get("checksum_algorithm").toString());
            }

            if(receivedMetadata.containsKey("checksum")) {
                bitstream.setColumn("checksum", receivedMetadata.get("checksum").toString());
            }

            if(receivedMetadata.containsKey("size_bytes")) {
                bitstream.setColumn("size_bytes", Long.valueOf(receivedMetadata.get("size_bytes").toString()));
            }
        }

		bitstream.setColumn("deleted", false);
		DatabaseManager.update(context, bitstream);

		int bitstreamId = bitstream.getIntColumn("bitstream_id");
		if (log.isDebugEnabled()) 
		{
			log.debug("Registered bitstream " + bitstreamId + " at location "
					+ bitstreamPath);
		}
		return bitstreamId;
	}

	/**
	 * Does the internal_id column in the bitstream row indicate the bitstream
	 * is a registered file
	 *
	 * @param internalId the value of the internal_id column
	 * @return true if the bitstream is a registered file
	 */
	public boolean isRegisteredBitstream(String internalId) {
		return internalId.startsWith(BitstreamStorageManager.REGISTERED_FLAG);
	}
	
    public String absolutePath(Context context, int id)
            throws SQLException, IOException
    {
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        int storeNumber = bitstream.getIntColumn("store_number");
        return stores.get(storeNumber).path(bitstream);
    }

    public String virtualPath(Context context, int id)
            throws SQLException, IOException
    {
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        int storeNumber = bitstream.getIntColumn("store_number");
        return stores.get(storeNumber).virtualPath(bitstream);
    }

    public String intermediatePath(Context context, int id)
            throws SQLException, IOException
    {
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        int storeNumber = bitstream.getIntColumn("store_number");
        return stores.get(storeNumber).intermediatePath(bitstream.getStringColumn("internal_id"));
    }

    /**
     * Retrieve the bits for the bitstream with ID. If the bitstream does not
     * exist, or is marked deleted, returns null.
     * 
     * @param context
     *            The current context
     * @param id
     *            The ID of the bitstream to retrieve
     * @exception IOException
     *                If a problem occurs while retrieving the bits
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     * 
     * @return The stream of bits, or null
     */
    public InputStream retrieve(Context context, int id)
            throws SQLException, IOException
    {
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        int storeNumber = bitstream.getIntColumn("store_number");
        return stores.get(storeNumber).get(bitstream);
    }

    /**
     * <p>
     * Remove a bitstream from the asset store. This method does not delete any
     * bits, but simply marks the bitstreams as deleted (the context still needs
     * to be completed to finalize the transaction).
     * </p>
     * 
     * <p>
     * If the context is aborted, the bitstreams deletion status remains
     * unchanged.
     * </p>
     * 
     * @param context
     *            The current context
     * @param id
     *            The ID of the bitstream to delete
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     */
    public void delete(Context context, int id) throws SQLException
    {
        DatabaseManager.updateQuery(context,
                "update Bundle set primary_bitstream_id=null where primary_bitstream_id = ? ",
                id);

        DatabaseManager.updateQuery(context,
                        "update Bitstream set deleted = '1' where bitstream_id = ? ",
                        id);
    }

    /**
     * Clean up the bitstream storage area. This method deletes any bitstreams
     * which are more than 1 hour old and marked deleted. The deletions cannot
     * be undone.
     * 
     * @param deleteDbRecords if true deletes the database records otherwise it
     * 	           only deletes the files and directories in the assetstore  
     * @exception IOException
     *                If a problem occurs while cleaning up
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     */
    public void cleanup(boolean deleteDbRecords, boolean verbose) throws SQLException, IOException
    {
        Context context = null;
        BitstreamInfoDAO bitstreamInfoDAO = new BitstreamInfoDAO();
        int commitCounter = 0;

        try
        {
            context = new Context();

            String myQuery = "select * from Bitstream where deleted = '1'";

            List<TableRow> storage = DatabaseManager.queryTable(context, "Bitstream", myQuery)
                    .toList();

            for (Iterator<TableRow> iterator = storage.iterator(); iterator.hasNext();)
            {
                TableRow row = iterator.next();
                int bid = row.getIntColumn("bitstream_id");

                Map wantedMetadata = new HashMap();
                wantedMetadata.put("size_bytes", null);
                wantedMetadata.put("modified", null);
                Map receivedMetadata = stores.get(row.getIntColumn("store_number")).about(row, wantedMetadata);

                // Make sure entries which do not exist are removed
                if (MapUtils.isEmpty(receivedMetadata))
                {
                    log.debug("bitstore.about is empty, so file is not present");
                    if (deleteDbRecords)
                    {
                        log.debug("deleting record");
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                        }
                        bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                        }
                        DatabaseManager.delete(context, "Bitstream", bid);
                    }
                    continue;
                }

                // This is a small chance that this is a file which is
                // being stored -- get it next time.
                if (isRecent(Long.valueOf(receivedMetadata.get("modified").toString())))
                {
                	log.debug("file is recent");
                    continue;
                }

                if (deleteDbRecords)
                {
                    log.debug("deleting db record");
                    if (verbose)
                    {
                        System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                    }
                    bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);
                    if (verbose)
                    {
                        System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                    }
                    DatabaseManager.delete(context, "Bitstream", bid);
                }

				if (isRegisteredBitstream(row.getStringColumn("internal_id"))) {
				    continue;			// do not delete registered bitstreams
				}


                // Since versioning allows for multiple bitstreams, check if the internal identifier isn't used on another place
                TableRow duplicateBitRow = DatabaseManager.querySingleTable(context, "Bitstream", "SELECT * FROM Bitstream WHERE internal_id = ? AND bitstream_id <> ?", row.getStringColumn("internal_id"), bid);
                if(duplicateBitRow == null)
                {
                    stores.get(row.getIntColumn("store_number")).remove(row);

                    String message = ("Deleted bitstream " + bid + ", internalID "
                                + row.getStringColumn("internal_id"));
                    if (log.isDebugEnabled())
                    {
                        log.debug(message);
                    }
                    if (verbose)
                    {
                        System.out.println(message);
                    }
                }

                // Make sure to commit our outstanding work every 100
                // iterations. Otherwise you risk losing the entire transaction
                // if we hit an exception, which isn't useful at all for large
                // amounts of bitstreams.
                commitCounter++;
                if (commitCounter % 100 == 0)
                {
                	System.out.print("Committing changes to the database...");
                    context.commit();
                    System.out.println(" Done!");
                }
            }

            context.complete();
        }
        // Aborting will leave the DB objects around, even if the
        // bitstreams are deleted. This is OK; deleting them next
        // time around will be a no-op.
        catch (SQLException sqle)
        {
            if (verbose)
            {
                System.err.println("Error: " + sqle.getMessage());
            }
            context.abort();
            throw sqle;
        }
        catch (IOException ioe)
        {
            if (verbose)
            {
                System.err.println("Error: " + ioe.getMessage());
            }
            context.abort();
            throw ioe;
        }
    }

    /**
     *
     * @param context
     * @param id of the bitstream to clone.
     * @return id of the clone bitstream.
     * @throws SQLException
     */
    public int clone(Context context, int id) throws SQLException
    {
        TableRow row = DatabaseManager.find(context, "bitstream", id);
        row.setColumn("bitstream_id", -1);
        DatabaseManager.insert(context, row);
        return row.getIntColumn("bitstream_id");
    }


    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Return true if this file is too recent to be deleted, false otherwise.
     * 
     * @param file
     *            The file to check
     * @return True if this file is too recent to be deleted
     */
    private static boolean isRecent(Long lastmod)
    {
        long now = new java.util.Date().getTime();

        if (lastmod >= now)
        {
            return true;
        }

        // Less than one hour old
        return (now - lastmod) < (1 * 60 * 1000);
    }

    public void printStores(Context context) {
        try {
            for(Integer storeNumber : stores.keySet()) {
                String query = "select count(*) as mycount from Bitstream where store_number = ? ";
                TableRowIterator tri = DatabaseManager.query(context, query, storeNumber);
                long countBitstreams = 0;

                try {
                    TableRow r = tri.next();
                    countBitstreams = r.getLongColumn("mycount");
                }
                finally {
                    // close the TableRowIterator to free up resources
                    if (tri != null) {
                        tri.close();
                    }
                }

                System.out.println("store[" + storeNumber + "] == " + stores.get(storeNumber).getClass().getSimpleName() + ", which has " + countBitstreams + " bitstreams.");
            }
            System.out.println("Incoming assetstore is store[" + incoming + "]");
        } catch (SQLException e) {
            log.error(e);
        }
    }

    /**
     * Migrates all assets off of one assetstore to another
     * @param assetstoreSource
     * @param assetstoreDestination
     */
    public void migrate(Context context, Integer assetstoreSource, Integer assetstoreDestination, boolean deleteOld, Integer batchCommitSize) throws IOException, SQLException {
        //Find all the bitstreams on the old source, copy it to new destination, update store_number, save, remove old
        String myQuery = "select * from Bitstream where store_number = ?";
        Iterator<TableRow> allBitstreamsInSource = DatabaseManager.queryTable(context, "Bitstream", myQuery, assetstoreSource)
                .toList().iterator();

        Integer processedCounter = 0;
        while (allBitstreamsInSource.hasNext()) {
            TableRow bitstream = allBitstreamsInSource.next();
            log.info("Copying bitstream:" + bitstream.getIntColumn("bitstream_id")
                    + " from assetstore[" + assetstoreSource + "] to assetstore["
                    + assetstoreDestination + "]");

            InputStream inputStream = retrieve(context, bitstream.getIntColumn("bitstream_id"));
            stores.get(assetstoreDestination).put(bitstream, inputStream);
            bitstream.setColumn("store_number", assetstoreDestination);
            DatabaseManager.update(context, bitstream);

            if (deleteOld) {
                log.info("Removing bitstream:" + bitstream.getIntColumn("bitstream_id") + " from assetstore[" + assetstoreSource + "]");
                stores.get(assetstoreSource).remove(bitstream);
            }

            processedCounter++;

            //modulo
            if ((processedCounter % batchCommitSize) == 0) {
                log.info("Migration Commit Checkpoint: " + processedCounter);
                context.commit();
            }
        }

        log.info("Assetstore Migration from assetstore[" + assetstoreSource + "] to assetstore[" + assetstoreDestination + "] completed. " + processedCounter + " objects were transferred.");
    }
}
