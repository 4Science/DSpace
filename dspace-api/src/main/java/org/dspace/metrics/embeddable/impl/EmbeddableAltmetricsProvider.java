/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.impl;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

public class EmbeddableAltmetricsProvider extends AbstractEmbeddableMetricProvider {

    protected String doiField;

    protected String doiDataAttr;

    protected String pmidField;

    protected String pmidDataAttr;

    protected String badgeType;

    protected String popover;

    private String details;

    private Boolean noScore;

    private Boolean hideNoMentions;

    private String linkTarget;

    protected String listBadgeType;

    protected String listPopOver;

    @Override
    public String innerHtml(Context context, Item item) {
        String doiAttr = this.calculateAttribute(item, doiField, doiDataAttr);
        String pmidAtt = this.calculateAttribute(item, pmidField, pmidDataAttr);

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("popover", this.popover);
        jsonObject.addProperty("badgeType", this.badgeType);
        jsonObject.addProperty("doiAttr", doiAttr);
        jsonObject.addProperty("pmidAttr", pmidAtt);

        jsonObject.addProperty("list-popover", this.listPopOver);
        jsonObject.addProperty("list-badgeType", this.listBadgeType);
        jsonObject.addProperty("list-doiAttr", doiAttr);
        jsonObject.addProperty("list-pmidAttr", pmidAtt);

        return jsonObject.toString();
    }

    protected String calculateAttribute(Item item, String field, String attr) {
        if (field != null && attr != null) {
            List<MetadataValue> values = this.getItemService().getMetadataByMetadataString(item, field);
            if (!values.isEmpty()) {
                return Optional.ofNullable(values.get(0))
                        .map(MetadataValue::getValue).orElse("");
            }
        }
        return "";
    }

    @Override
    public String getMetricType() {
        return "altmetric";
    }

    public void setBadgeType(String badgeType) {
        this.badgeType = badgeType;
    }

    public void setPopover(String popover) {
        this.popover = popover;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setNoScore(Boolean noScore) {
        this.noScore = noScore;
    }

    public void setHideNoMentions(Boolean hideNoMentions) {
        this.hideNoMentions = hideNoMentions;
    }

    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }

    public void setDoiField(String doiField) {
        this.doiField = doiField;
    }

    public void setDoiDataAttr(String doiDataAttr) {
        this.doiDataAttr = doiDataAttr;
    }

    public void setPmidField(String pmidField) {
        this.pmidField = pmidField;
    }

    public void setPmidDataAttr(String pmidDataAttr) {
        this.pmidDataAttr = pmidDataAttr;
    }

    public void setListBadgeType(String listBadgeType) {
        this.listBadgeType = listBadgeType;
    }

    public void setListPopOver(String listPopOver) {
        this.listPopOver = listPopOver;
    }
}
