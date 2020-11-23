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

/**
 * <P>
 * Stores, retrieves and deletes bitstreams.
 * </P>
 * 
 * <P>
 * Presently, asset stores are specified in <code>dspace.cfg</code>. Since
 * Java does not offer a way of detecting free disk space, the asset store to
 * use for new bitstreams is also specified in a configuration property. The
 * drawbacks to this are that the administrators are responsible for monitoring
 * available space in the asset stores, and DSpace (Tomcat) has to be restarted
 * when the asset store for new ('incoming') bitstreams is changed.
 * </P>
 * 
 * <P>
 * Mods by David Little, UCSD Libraries 12/21/04 to allow the registration of
 * files (bitstreams) into DSpace.
 * </P>
 * 
 * <p>Cleanup integration with checker package by Nate Sarr 2006-01. N.B. The 
 * dependency on the checker package isn't ideal - a Listener pattern would be 
 * better but was considered overkill for the purposes of integrating the checker.
 * It would be worth re-considering a Listener pattern if another package needs to 
 * be notified of BitstreamStorageManager actions.</p> 
 *
 * @author Peter Breton, Robert Tansley, David Little, Nathan Sarr
 * @version $Revision$
 */
public interface BitstreamStorageService
{
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
    public int store(Context context, InputStream is) throws SQLException, IOException;

    /**
     * Register a bitstream already in storage.
     *
     * @param context
     *            The current context
     * @param assetstore The assetstore number for the bitstream to be
     *             registered
     * @param bitstreamPath The relative path of the bitstream to be registered.
     *         The path is relative to the path of ths assetstore.
     * @return The ID of the registered bitstream
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     * @throws IOException
     */
    public int register(Context context, int assetstore,
                String bitstreamPath, boolean computeMD5) throws SQLException, IOException;

    /**
     * Does the internal_id column in the bitstream row indicate the bitstream
     * is a registered file
     *
     * @param internalId the value of the internal_id column
     * @return true if the bitstream is a registered file
     */
    public boolean isRegisteredBitstream(String internalId);

    public String absolutePath(Context context, int id)
            throws SQLException, IOException;

    public String virtualPath(Context context, int id)
            throws SQLException, IOException;

    public String intermediatePath(Context context, int id)
            throws SQLException, IOException;

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
            throws SQLException, IOException;

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
    public void delete(Context context, int id) throws SQLException;

    /**
     * Clean up the bitstream storage area. This method deletes any bitstreams
     * which are more than 1 hour old and marked deleted. The deletions cannot
     * be undone.
     * 
     * @param deleteDbRecords if true deletes the database records otherwise it
     *                only deletes the files and directories in the assetstore  
     * @exception IOException
     *                If a problem occurs while cleaning up
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     */
    public void cleanup(boolean deleteDbRecords, boolean verbose) throws SQLException, IOException;

    /**
     *
     * @param context
     * @param id of the bitstream to clone.
     * @return id of the clone bitstream.
     * @throws SQLException
     */
    public int clone(Context context, int id) throws SQLException;

    public void printStores(Context context);

    /**
     * Migrates all assets off of one assetstore to another
     * @param assetstoreSource
     * @param assetstoreDestination
     */
    public void migrate(Context context, Integer assetstoreSource, Integer assetstoreDestination,
            boolean deleteOld, Integer batchCommitSize) throws IOException, SQLException;

    public int getIncoming();

    public Map<Integer, BitStoreService> getStores();
}
