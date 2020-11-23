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
import java.util.Map;

import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class BitstreamStorageManager
{
    /**
     * This prefix string marks registered bitstreams in internal_id
     */
    public static final String REGISTERED_FLAG = "-R";

    private static BitstreamStorageService bitstreamStorageService;

    public static int getIncoming() {
    	return getBitstreamStorageService().getIncoming();
    }

    public static Map<Integer, BitStoreService> getStores() {
    	return getBitstreamStorageService().getStores();
    }

    public static int store(Context context, InputStream is)
            throws SQLException, IOException {
    	return getBitstreamStorageService().store(context, is);
    }

	public static int register(Context context, int assetstore,
				String bitstreamPath, boolean computeMD5) throws SQLException, IOException {
		return getBitstreamStorageService().register(context, assetstore, bitstreamPath, computeMD5);
	}

	public static boolean isRegisteredBitstream(String internalId) {
		return getBitstreamStorageService().isRegisteredBitstream(internalId);
	}
	
    public static String absolutePath(Context context, int id)
            throws SQLException, IOException {
    	return getBitstreamStorageService().absolutePath(context, id);
    }

    public static String virtualPath(Context context, int id)
            throws SQLException, IOException {
        return getBitstreamStorageService().virtualPath(context, id);
    }

    public static String intermediatePath(Context context, int id)
            throws SQLException, IOException {
    	return getBitstreamStorageService().intermediatePath(context, id);
    }

    public static InputStream retrieve(Context context, int id)
            throws SQLException, IOException {
    	return getBitstreamStorageService().retrieve(context, id);
    }

    public static void delete(Context context, int id)
    		throws SQLException {
    	getBitstreamStorageService().delete(context, id);
    }

    public static void cleanup(boolean deleteDbRecords, boolean verbose)
    		throws SQLException, IOException {
        getBitstreamStorageService().cleanup(deleteDbRecords, verbose);
    }

    public static int clone(Context context, int id)
    		throws SQLException {
    	return getBitstreamStorageService().clone(context, id);
    }

    public static void printStores(Context context) {
    	getBitstreamStorageService().printStores(context);
    }

    public static void migrate(Context context, Integer assetstoreSource,
    		Integer assetstoreDestination, boolean deleteOld, Integer batchCommitSize)
    				throws IOException, SQLException {
    	getBitstreamStorageService().migrate(context, assetstoreSource, assetstoreDestination, deleteOld, batchCommitSize);
    }

    public static BitstreamStorageService getBitstreamStorageService() {
		if (bitstreamStorageService == null) {
			bitstreamStorageService = new DSpace().getServiceManager()
					.getServiceByName(BitstreamStorageService.class.getName(),
							BitstreamStorageService.class);
		}
		return bitstreamStorageService;
	}
}
