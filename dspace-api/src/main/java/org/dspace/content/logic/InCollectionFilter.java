/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A condition that accepts a list of collection handles and returns true
 * if the item belongs to any of them.
 *
 * @author Kim Shepherd
 * @author Giuseppe Digilio
 */
public class InCollectionFilter implements Filter {

    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected CollectionService collectionService;
    @Autowired(required = true)
    protected HandleService handleService;

    private String name;
    private Map<String, Object> parameters = new HashMap<>();
    private static Logger log = LogManager.getLogger(InCollectionFilter.class);

    /**
     * Get parameters set by spring configuration in item-filters.xml
     * These could be any kind of map that the extending condition class needs for evaluation
     * @return map of parameters
     * @throws LogicalStatementException
     */
    public Map<String, Object> getParameters() throws LogicalStatementException {
        return this.parameters;
    }

    /**
     * Set parameters - used by Spring when creating beans from item-filters.xml
     * These could be any kind of map that the extending condition class needs for evaluation
     * @param parameters
     * @throws LogicalStatementException
     */
    @Autowired(required = true)
    public void setParameters(Map<String, Object> parameters) throws LogicalStatementException {
        this.parameters = parameters;
    }

    /**
     * Return true if item is in one of the specified collections
     * Return false if not
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    @Override
    public Boolean getResult(Context context, Item item) throws LogicalStatementException {

        List<String> collectionHandles = (List<String>)getParameters().get("collections");
        List<Collection> itemCollections = item.getCollections();
        for (Collection collection : itemCollections) {
            if (collectionHandles.contains(collection.getHandle())) {
                log.debug("item " + item.getHandle() + " is in collection "
                    + collection.getHandle() + ", returning true");
                return true;
            }
        }

        // Look for the parent object of the item. This is important as the item.getOwningCollection method
        // may return null, even though the item itself does have a parent object, at the point of archival
        try {
            DSpaceObject parent = itemService.getParentObject(context, item);
            if (parent != null) {
                log.debug("Got parent DSO for item: " + parent.getID().toString());
                log.debug("Parent DSO handle: " + parent.getHandle());
                if (collectionHandles.contains(parent.getHandle())) {
                    log.debug("item " + item.getHandle() + " is in collection "
                        + parent.getHandle() + ", returning true");
                    return true;
                }
            } else {
                log.debug("Parent DSO is null...");
            }
        } catch (SQLException e) {
            log.error("Error obtaining parent DSO", e);
            throw new LogicalStatementException(e);
        }

        // If we reach this statement, the item did not appear in any of the collections from the parameters
        log.debug("item " + item.getHandle() + " not found in the passed collection handle list");

        return false;
    }

    @Override
    public void setBeanName(String name) {
        log.debug("Initialize bean " + name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
