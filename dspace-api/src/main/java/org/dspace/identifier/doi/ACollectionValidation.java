/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.sql.SQLException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItem;

/**
 * This implementation provide the possibility prevent/permit DOI registration
 * based on Item Owning Collection and other criteria, such as check on metadata
 * or if item has files.
 * 
 * checkFile property contains boolean value; if true checks if item has files. 
 * checkMetadata check if the metadata listed have value to permits registration (true, t, on, yes or y); otherwise false. 
 * 
 * @author Riccardo Fazio (riccardo.fazio at 4science.it)
 *
 */
public abstract class ACollectionValidation
        implements IdentifierRegisterValidation
{

    private Logger log = Logger.getLogger(ACollectionValidation.class);

    private String checkFile;

    private String checkMetadata;
    
    public abstract boolean canRegister(Context context, DSpaceObject dso);

    /**
     * Check if the DOI minting is allowed for the Item 
     * 
     * @param item
     * 
     * @return true if DOI is permits on Item
     */
    protected boolean isToRegister(Item item)
    {
        boolean register = true;
        if (BooleanUtils.toBoolean(getCheckFile()))
        {
            try
            {
                register = item.hasUploadedFiles();
            }
            catch (SQLException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        if (StringUtils.isNotBlank(getCheckMetadata()))
        {
            register = register
                    && checkMetadataValue(item);
        }

        return register;
    }

    /**
     * Check if the metadata value is allowed to minting DOI
     * 
     * @param item
     * 
     * @return
     */
    protected boolean checkMetadataValue(Item item)
    {
        return BooleanUtils.toBoolean(item.getMetadata(getCheckMetadata()));
    }

    public String getCollectionHandle(Context context, Item item)
    {
        String collHandle = null;
        try
        {
            collHandle = item.getParentObject().getHandle();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return collHandle;
    }

    public String getCheckFile()
    {
        return checkFile;
    }

    public void setCheckFile(String checkFile)
    {
        this.checkFile = checkFile;
    }

    public String getCheckMetadata()
    {
        return checkMetadata;
    }

    public void setCheckMetadata(String checkMetadata)
    {
        this.checkMetadata = checkMetadata;
    }
}