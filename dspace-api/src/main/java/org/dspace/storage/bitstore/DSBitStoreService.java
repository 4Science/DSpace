/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import org.apache.log4j.Logger;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.TableRow;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Native DSpace (or "Directory Scatter" if you prefer) asset store.
 * Implements a directory 'scatter' algorithm to avoid OS limits on
 * files per directory.
 * 
 * @author Peter Breton, Robert Tansley, Richard Rodgers, Peter Dietz
 */

public class DSBitStoreService extends ABitStoreService
{
    /** log4j log */
    private static Logger log = Logger.getLogger(DSBitStoreService.class);

    // Checksum algorithm
    private static final String CSA = "MD5";

    public DSBitStoreService()
    {
    }

    /**
     * Initialize the asset store
     *
     */
    public void init()
    {
        // the config string contains just the asset store directory path
        //set baseDir?
    }

    /**
     * Return an identifier unique to this asset store instance
     * 
     * @return a unique ID
     */
    public String generateId()
    {
        return Utils.generateKey();
    }

    /**
     * Retrieve the bits for the asset with ID. If the asset does not
     * exist, returns null.
     * 
     * @param bitstream row
     *            The ID of the asset to retrieve
     * @exception java.io.IOException
     *                If a problem occurs while retrieving the bits
     *
     * @return The stream of bits, or null
     */
    public InputStream get(TableRow bitstream) throws IOException
    {
        try {
            return new FileInputStream(getFile(bitstream));
        } catch (Exception e)
        {
            log.error("get(" + bitstream.getStringColumn("internal_id") + ")", e);
            throw new IOException(e);
        }
    }

    /**
     * Store a stream of bits.
     *
     * <p>
     * If this method returns successfully, the bits have been stored.
     * If an exception is thrown, the bits have not been stored.
     * </p>
     *
     * @param in
     *            The stream of bits to store
     * @exception java.io.IOException
     *             If a problem occurs while storing the bits
     *
     * @return Map containing technical metadata (size, checksum, etc)
     */
    public void put(TableRow bitstream, InputStream in) throws IOException
    {
        try {
            File file = getFile(bitstream);

            // Make the parent dirs if necessary
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            //Create the corresponding file and open it
            file.createNewFile();

            FileOutputStream fos = new FileOutputStream(file);

            // Read through a digest input stream that will work out the MD5
            DigestInputStream dis = null;

            try {
                dis = new DigestInputStream(in, MessageDigest.getInstance(CSA));
            }
            // Should never happen
            catch (NoSuchAlgorithmException nsae) {
                log.warn("Caught NoSuchAlgorithmException", nsae);
            }

            Utils.bufferedCopy(dis, fos);
            fos.close();
            in.close();

            bitstream.setColumn("size_bytes", file.length());

            if (dis != null)
            {
                bitstream.setColumn("checksum", Utils.toHex(dis.getMessageDigest()
                        .digest()));
                bitstream.setColumn("checksum_algorithm", CSA);
            }
        } catch (Exception e) {
            log.error("put(" + bitstream.getStringColumn("internal_id") + ", inputstream)", e);
            throw new IOException(e);
        }
    }

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param bitstream
     *            The bitstream row to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     *
     * @exception java.io.IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     */
    public Map about(TableRow bitstream, Map attrs) throws IOException
    {
        try {
            // potentially expensive, since it may calculate the checksum
            File file = getFile(bitstream);
            if (file != null && file.exists()) {
                if (attrs.containsKey("size_bytes")) {
                    attrs.put("size_bytes", file.length());
                }
                if (attrs.containsKey("checksum")) {
                    // generate checksum by reading the bytes
                    DigestInputStream dis = null;
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        dis = new DigestInputStream(fis, MessageDigest.getInstance(CSA));
                    } catch (NoSuchAlgorithmException e) {
                        log.warn("Caught NoSuchAlgorithmException", e);
                        throw new IOException("Invalid checksum algorithm");
                    }
                    final int BUFFER_SIZE = 1024 * 4;
                    final byte[] buffer = new byte[BUFFER_SIZE];
                    while (true) {
                        final int count = dis.read(buffer, 0, BUFFER_SIZE);
                        if (count == -1) {
                            break;
                        }
                    }
                    attrs.put("checksum", Utils.toHex(dis.getMessageDigest().digest()));
                    attrs.put("checksum_algorithm", CSA);
                    dis.close();
                }
                if (attrs.containsKey("modified")) {
                    attrs.put("modified", String.valueOf(file.lastModified()));
                }
                return attrs;
            } else if (!file.exists()) {
                log.error("File: " + file.getAbsolutePath() + " to be registered not found");
            }
            return null;
        } catch (Exception e) {
            log.error("about(" + bitstream.getStringColumn("internal_id") + ")", e);
            throw new IOException(e);
        }
    }

    /**
     * Remove an asset from the asset store. An irreversible operation.
     *
     * @param bitstream
     *            The bitstream row to delete
     * @exception java.io.IOException
     *             If a problem occurs while removing the asset
     */
    public void remove(TableRow bitstream) throws IOException
    {
        try {
            File file = getFile(bitstream);
            if (file != null) {
                if (file.delete()) {
                    deleteParents(file);
                }
            } else {
                log.warn("Attempt to remove non-existent asset. ID: " + bitstream.getStringColumn("internal_id"));
            }
        } catch (Exception e) {
            log.error("remove(" + bitstream.getStringColumn("internal_id") + ")", e);
            throw new IOException(e);
        }
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Delete empty parent directories.
     * 
     * @param file
     *            The file with parent directories to delete
     */
    private synchronized static void deleteParents(File file)
    {
        if (file == null)
        {
            return;
        }

        File tmp = file;

        for (int i = 0; i < directoryLevels; i++)
        {
            File directory = tmp.getParentFile();
            File[] files = directory.listFiles();

            // Only delete empty directories
            if (files.length != 0)
            {
                break;
            }

            directory.delete();
            tmp = directory;
        }
    }

    /**
     * Return the file corresponding to a bitstream. It's safe to pass in
     * <code>null</code>.
     *
     * @param bitstream
     *            the database table row for the bitstream. Can be
     *            <code>null</code>
     *
     * @return The corresponding file in the file system, or <code>null</code>
     *
     * @exception IOException
     *                If a problem occurs while determining the file
     */
    protected File getFile(TableRow bitstream) throws IOException
    {
        // Check that bitstream is not null
        if (bitstream == null)
        {
            return null;
        }

        // turn the internal_id into a file path relative to the assetstore directory
        String sInternalId = bitstream.getStringColumn("internal_id");

        StringBuilder bufFilename = new StringBuilder();
        bufFilename.append(getBaseDir().getCanonicalFile());
        bufFilename.append(File.separator);
        bufFilename.append(getRelativePath(sInternalId));
        if (log.isDebugEnabled()) {
            log.debug("Local filename for " + sInternalId + " is "
                    + bufFilename.toString());
        }
        return new File(bufFilename.toString());
    }

    @Override
    public String path(TableRow bitstream) throws IOException {
        return getFile(bitstream).getAbsolutePath();
    }

    @Override
    public String virtualPath(TableRow bitstream) throws IOException {
        return path(bitstream);
    }
}
