/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.util.List;

import org.dspace.content.logic.DefaultFilter;
import org.dspace.content.logic.Filter;
import org.dspace.content.security.AccessItemMode;
import org.dspace.content.security.CrisSecurity;

/**
 * This Class representing a modality of edit an item and adding some association to the item
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class AssociateItemMode implements AccessItemMode {

    public static final String NONE = "none";
    /**
     * Configuration name
     */
    private String name;

    /**
     * Discovery name
     */
    private String discovery;

    /**
     * item Type Source
     */
    private String itemTypeSource;

    /**
     * item Type Target
     */
    private String itemTypeTarget;

    /**
     * Item type Metadatafield
     */

    private String metadatafield;

    /**
     * Contains the condition for the item to be matched to fulfill to edit the source item
     */
    private DefaultFilter conditionSource = null;

    /**
     * Contains the condition for the item to be matched to fulfill to edit the target item
     */
    private DefaultFilter conditionTarget = null;

    /**
     * disable authorization check for editing source item
     */
    private boolean disableAuthSource = false;

    /**
     * Label used in UI for i18n
     */
    private String label;
    /**
     * Defines the users enabled to use this edit configuration
     */
    private List<CrisSecurity> securities;

    /**
     * Contains the list of groups metadata for CUSTOM security or groups name/uuid
     * for GROUP security
     */
    private List<String> groups;
    /**
     * Contains the list of users metadata for CUSTOM security
     */
    private List<String> users;
    /**
     * Contains the list of items metadata for CUSTOM security
     */
    private List<String> items;

    public AssociateItemMode() {}

    @Override
    public List<CrisSecurity> getSecurities() {
        return securities;
    }

    public void setSecurity(CrisSecurity security) {
        this.securities = List.of(security);
    }

    public void setSecurities(List<CrisSecurity> securities) {
        this.securities = securities;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public List<String> getGroupMetadataFields() {
        return groups;
    }
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
    public List<String> getUserMetadataFields() {
        return users;
    }
    public void setUsers(List<String> users) {
        this.users = users;
    }
    public List<String> getItemMetadataFields() {
        return items;
    }
    public void setItems(List<String> items) {
        this.items = items;
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    public Filter getAdditionalFilter() {
        return null;
    }

    @Override
    public String toString() {
        return "AssociateItemMode [name=" + name + ", label=" + label + ", security=" + securities + ", discovery="
            + discovery + "]";
    }

    public boolean isdisableAuthSource() {
        return disableAuthSource;
    }

    public DefaultFilter getConditionTarget() {
        return conditionTarget;
    }

    public void setConditionTarget(DefaultFilter conditionTarget) {
        this.conditionTarget = conditionTarget;
    }

    public DefaultFilter getConditionSource() {
        return conditionSource;
    }

    public void setConditionSource(DefaultFilter conditionSource) {
        this.conditionSource = conditionSource;
    }

    public String getItemTypeTarget() {
        return itemTypeTarget;
    }

    public void setItemTypeTarget(String itemTypeTarget) {
        this.itemTypeTarget = itemTypeTarget;
    }

    public String getItemTypeSource() {
        return itemTypeSource;
    }

    public void setItemTypeSource(String itemTypeSource) {
        this.itemTypeSource = itemTypeSource;
    }

    public void setDiscovery(String discovery) {
        this.discovery = discovery;
    }

    public String getDiscovery() {
        return discovery;
    }

    public String getMetadatafield() {
        return metadatafield;
    }

    public void setMetadatafield(String metadatafield) {
        this.metadatafield = metadatafield;
    }

    public void setDisableAuthSource(boolean disableAuthSource) {
        this.disableAuthSource = disableAuthSource;
    }
}
