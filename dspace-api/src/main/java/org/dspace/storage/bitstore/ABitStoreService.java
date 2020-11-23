/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.File;

/**
 * A low-level asset store abstract class
 * 
 * @author Richard Rodgers, Peter Dietz, Francesco Pio Scognamiglio
 */
public abstract class ABitStoreService implements BitStoreService
{
    // These settings control the way an identifier is hashed into
    // directory and file names
    //
    // With digitsPerLevel 2 and directoryLevels 3, an identifier
    // like 12345678901234567890 turns into the relative name
    // /12/34/56/12345678901234567890.
    //
    // You should not change these settings if you have data in the
    // asset store, as the BitstreamStorageManager will be unable
    // to find your existing data.
    protected static final int digitsPerLevel = 2;
    protected static final int directoryLevels = 3;

    /** the asset directory */
    private File baseDir;

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Return the relative path formed by the intermediate path and the bitstream internal id.
     *
     * @param sInternalId
     *            The internal_id
     * @return The relative path based on the intermediate path and the id without leading or trailing separators
     */
    protected String getRelativePath(String sInternalId) {
        // there are 2 cases:
        // -conventional bitstream, conventional storage
        // -registered bitstream, conventional storage
        // conventional bitstream - dspace ingested, dspace random name/path
        // registered bitstream - registered to dspace, any name/path
        String sIntermediatePath = null;
        if (BitstreamStorageManager.isRegisteredBitstream(sInternalId)) {
            sInternalId = sInternalId.substring(BitstreamStorageManager.REGISTERED_FLAG.length());
            sIntermediatePath = "";
        } else {
            // Sanity Check: If the internal ID contains a
            // pathname separator, it's probably an attempt to
            // make a path traversal attack, so ignore the path
            // prefix.  The internal-ID is supposed to be just a
            // filename, so this will not affect normal operation.
            if (sInternalId.contains(File.separator)) {
                sInternalId = sInternalId.substring(sInternalId.lastIndexOf(File.separator) + 1);
            }

            sIntermediatePath = intermediatePath(sInternalId);
        }

        return sIntermediatePath + sInternalId;
    }

    @Override
    public String intermediatePath(String internalId) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < directoryLevels; i++) {
            int digits = i * digitsPerLevel;
            if (i > 0) {
                buf.append(File.separator);
            }
            buf.append(internalId.substring(digits, digits
                    + digitsPerLevel));
        }
        buf.append(File.separator);
        return buf.toString();
    }
}
