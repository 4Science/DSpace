/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security.service;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Service to check metadata security.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface MetadataSecurityService {

    /**
     * Returns all the metadata values of the given item filtered by permission
     * evaluations.
     *
     * @param  context the DSpace Context
     * @param  item    the item
     * @return         the metadata values
     */
    List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item);

    /**
     * Returns all the metadata values of the given item related to the given
     * metadataField filtered by permission evaluations.
     *
     * @param  context       the DSpace Context
     * @param  item          the item
     * @param  metadataField the metadata field
     * @return               the metadata values
     */
    List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item, String metadataField);

    /**
     * Returns all the metadata values of the given item filtered by permission
     * evaluations. If the provided preventBoxSecurityCheck parameter is true then
     * the security checks using boxes are not performed,
     *
     * @param  context                 the DSpace Context
     * @param  item                    the item
     * @param  preventBoxSecurityCheck true if the box security check must be
     *                                 skipped, false otherwise
     * @return                         the metadata values
     */
    List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item,
        boolean preventBoxSecurityCheck);

    /**
     * Returns all the metadata values of the given item filtered by permission
     * evaluations. If the provided preventBoxSecurityCheck parameter is true then
     * the security checks using boxes are not performed,
     * and filtered also by the current locale language of the given context.
     *
     * @param  context                 the DSpace Context
     * @param  item                    the item
     * @param  preventBoxSecurityCheck true if the box security check must be
     *                                 skipped, false otherwise
     * @return                         the metadata values
     */
    List<MetadataValue> getPermissionAndLangFilteredMetadataFields(Context context, Item item,
                                                            boolean preventBoxSecurityCheck);

    /**
     * Returns all the metadata values of the given item related to the given
     * metadataField filtered by permission evaluations.
     *
     * @param  context                 the DSpace Context
     * @param  item                    the item
     * @param  metadataField           the metadata field
     * @param  preventBoxSecurityCheck true if the box security check must be
     *                                 skipped, false otherwise
     * @return                         the metadata values
     */
    List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item, String metadataField,
        boolean preventBoxSecurityCheck);

    /**
     * Check if the given metadata field is visible for the given item.
     *
     * @param  context                 the DSpace Context
     * @param  item                    the item
     * @param  metadataField           the metadata field
     * @return                         true if the metadata field is visible, false
     *                                 otherwise
     */
    boolean checkMetadataFieldVisibility(Context context, Item item, MetadataField metadataField);
}
